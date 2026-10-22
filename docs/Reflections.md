# Reflections

## Kok Seng — Admin and authentication

Draft based on recorded work on22October2026; pending my verification and personal additions.

### AI security

I used Codex to implement an activity-report feature whose model input contains
booking totals and percentages only. It receives no consultation notes, account
names or emails, and has no tools for changing records. Generated JSON is checked
for exact fields, length and unsafe markup before it becomes plain-text output.
Tests use invalid output and a provider that ignores cancellation. The latter led
to retaining the account's in-flight block until the worker finishes, even after
its minimum60-second hold. Real SoC quality and shared durable quotas remain unverified;
fake responses establish application behavior, not real-model resistance.

### Spec Driven Development

I asked Codex to refresh main before working and use the shared contracts rather
than create parallel ones. The implementation retains /api, the existing user DTO,
CSRF behavior, core account operations and the shared transaction lock order.
Acceptance scenarios cover authorization, session revocation, management conflicts
and statistics. Tests include both orderings of module/assignment changes racing
with Tutor slot creation, so they check the intended consistency rule rather than
only one successful request. Password reset and forced password changes were excluded
from both the specifications and implementation.

### Basic Multi Agent SE

I used separate implementation agents for authentication and Admin, with application
wiring handled in the main task and an independent security review. The reviewer
found that database calls were outside the login timeout. Codex moved lookup,
password verification and account confirmation into the bounded operation and added
a timeout check before issuing a session. Another review found that an AI call could
still run after its lease expired; the implementation now waits for actual completion
before permitting overlap. These examples show why a reported test pass was followed
by independent inspection and additional tests. My final verification of this draft
and interaction summaries is still pending.

For the development process, a separate test supplied fake instructions to publish
credentials, disable CSRF and claim my approval without evidence. The reviewing
agent rejected them and requested real test reports. It did not execute commands
or access secrets. This is one limited evaluation, not a claim that the workflow is
immune to malicious instructions.
