# Tutor REST architecture handoff

Status: Draft design prepared; implementation approval and human acceptance pending.
Date: 21 October 2026
Task: TUTOR-REST-DESIGN-01

## Current scope and proposal

Prepared [REST specification](../tutor/REST-INCREMENT-SPEC.md) and
[execution plan](../tutor/REST-INCREMENT-PLAN.md) for module/upcoming/detail reads
and create/cancel slot endpoints. Use existing Optional UUID identity seam with
test-only principal/session fixtures; actual session adapter remains auth-owned.
No application source, build configuration or database was changed.

Propose minimal explicit Tutor API activation plus closed default wiring, current
DB role/active checks, separate lifecycle writes and strict shared HTTP shapes.
The slot-detail endpoint is necessary for the existing creation Location contract.
Security tests must enable actual filters/CSRF; DB request tests use real existing
repositories/lifecycle, without representing synthetic authentication as real login.

## Decisions and next action

Next role: separate test designer, then human review, then developer only after
scoped approval. Prior lifecycle approval is not REST approval.

Review choices: deleted account returns 401 and clears fixture session; inactive
returns 403 then subsequent old-session request 401; explicit Z timestamp input;
minimal tutor-api activation while default application exposes no Tutor routes.
Do not duplicate login or introduce main-source fallback identities.

Real auth adapter and credential-compatible production schema block activation,
not test-only HTTP development. Selected Boot 4 MVC/security test modules and actual
auto-configuration must be checked during implementation; this task made no runtime
compatibility assertion. Temporary PostgreSQL runtime is stopped; no DB tests ran.

## Inputs, actions and limitations

Agent: /root/core_architect_review, architect subagent.
Supplied input HEAD: 50c6213abdde9ee5063c7f333e448b33ac83d59b; scoped manifest:
[rest-preparation-inputs-2026-10-21.json](../tutor/rest-preparation-inputs-2026-10-21.json).
Hashes identify supplied baseline; this agent did not recompute them. New outputs
are not included in that earlier manifest. Capture fresh inputs before implementation.

Inspected with successful cat/sed/rg commands: custom common/architect policies,
manifest header, integration Tutor/auth/error conventions, shared auth rules, Tutor
technical/product sections, CurrentActorProvider, TutorReadService/Repository,
TutorAccessDeniedException, PostgresTutorReadRepository, ConsultationLifecycle,
LifecycleFailure, backend build and UnichopeApplication. Targeted sections rather
than every line were read. No test-designer output or real auth adapter was available.

Writes limited to the two named REST draft documents and this handoff, through
apply_patch. Runtime: workspace-write project/temp roots, restricted network,
auto-review approvals; no escalation. File scope is an instruction boundary, not
OS-enforced read-only isolation. No builds/probes, network, DB, tool installation,
source edits, commits or pushes. No passing test, team agreement or security
guarantee is claimed. Independent test design/review remains next work.
