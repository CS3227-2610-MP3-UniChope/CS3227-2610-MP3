# Trusted synthetic harness check; no network, subprocess or filesystem writes.
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'app'))
from calc import add
actual = add(2, 3)
if actual != 5:
    print(f'FAIL SYN-01: expected 5, observed {actual}')
    raise SystemExit(1)
print('PASS SYN-01: add(2, 3) == 5')
