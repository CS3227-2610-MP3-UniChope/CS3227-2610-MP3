#!/usr/bin/env python3
"""Hash an explicitly selected non-secret input scope, including untracked files."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess


def capture(root, selected, output):
    root = root.resolve()
    output = output.resolve()
    excluded = {'.git', '.gradle', 'build', 'dist', 'node_modules', '.aws',
                '__pycache__', 'coverage', '.codex'}
    files = set()
    for name in selected:
        source = root / name
        if source.is_symlink() or not source.resolve().is_relative_to(root):
            raise ValueError(f'Input outside root or symlink: {name}')
        if not source.exists():
            raise ValueError(f'Missing input: {name}')
        for path in source.rglob('*') if source.is_dir() else [source]:
            relative = path.relative_to(root)
            if path.is_symlink() or excluded.intersection(relative.parts):
                continue
            lower = path.name.lower()
            if (lower.startswith('.env') or lower.endswith(('.pem', '.key', '.p12'))
                    or any(word in lower for word in ('secret', 'credential', 'canary'))):
                continue
            if path.is_file() and path.resolve() != output:
                files.add(relative.as_posix())
    tracked = set(subprocess.check_output(
        ['git', '-C', str(root), 'ls-files', '-z']).decode().split('\0'))
    entries = []
    for name in sorted(files):
        path = root / name
        data = path.read_bytes()
        entries.append({'path': name, 'tracked': name in tracked, 'bytes': len(data),
                        'sha256': hashlib.sha256(data).hexdigest(),
                        'executable': bool(path.stat().st_mode & 0o111)})
    return {'format': 1, 'head': subprocess.check_output(
        ['git', '-C', str(root), 'rev-parse', 'HEAD']).decode().strip(),
        'scope': selected, 'files': entries,
        'limitations': 'Explicit scope only; filename exclusions are a convenience, not a secret scanner. '
                        'Inspect paths first. Hashes identify but cannot restore contents.'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('paths', nargs='+')
    args = parser.parse_args()
    manifest = capture(args.root, args.paths, args.output)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(manifest, indent=2) + '\n')
    print(f"Captured {len(manifest['files'])} files at HEAD {manifest['head']}")
