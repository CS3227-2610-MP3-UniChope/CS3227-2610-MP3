# Tutor AI Security Verification Increment

Status: In progress; extends the existing P6 security boundary without enabling a
real provider or changing product behavior.
Date: 23 October 2026

## Goal and threat boundaries

Verify that untrusted Tutor notes and model output cannot gain application
authority, disclose secrets, or cause an unreviewed write. The Tutor draft task
uses no tools, database access, retrieval, or actions. Its prompt receives only a
fixed instruction and the Tutor's submitted rough notes. There is no external
document or retrieved-context path in this increment; therefore indirect retrieval
injection has no input channel here. A future addition of retrieval, files, or tools
must add a threat model and tests before implementation.

Attack surfaces in scope are the rough-note request, authenticated HTTP/session and
CSRF boundary, shared admission/provider gateway, untrusted provider output, and the
UI actions that copy or save a draft. Provider output is data only. Text claiming
that a draft is approved, saved, or authorized cannot call endpoints or alter roles.
Saving remains a separate Tutor-authenticated, owner-checked, versioned request.

## Required controls and evidence

- Validate size and shape before admission; unauthorized, foreign, incomplete, or
  invalid requests make zero gateway calls.
- Keep actor, task, provider key, and output budget in server-owned metadata. Never
  accept key, role, actor, or model selection from the browser.
- Treat rough notes as untrusted input; send no hidden records or credentials and
  expose no tools or mutation capabilities to the model.
- Revalidate the captured session after generation and discard output if the Tutor
  account or session is no longer valid.
- Parse and bound model output; reject malformed and markup-like content without
  returning raw provider data.
- Do not persist generated output automatically. Require explicit user review,
  copy, and a separate version-checked save. Model text cannot grant permission.
- Keep raw notes, prompts, responses, credentials, and cookies out of routine logs.
  Tutor draft components currently add no content logging.

The matrix maps these claims to the existing tests and adds a focused approval-text
case. Deterministic fakes and static inspection establish application boundaries;
they do not prove that a live model resists injection or that valid-looking output is
truthful. Independent review and human acceptance remain separate.
