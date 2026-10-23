# UniChope User Guide

UniChope is a web app for booking university consultations. This guide adds
the Tutor workflow to the deployed-user instructions and retains the existing
authentication and Admin-service information. Tutor dates and times shown in
the app use Singapore time (SGT).

> **Release setup:** Replace `[production URL]` and the bracketed account
> instructions with the team's deployed URL and secure account-delivery
> details before sharing this guide with peer testers. Do not put real
> passwords or provider keys in this public document.

## 1. Quick start

### 1.1 Open UniChope

Open **[production URL]** in a current web browser. The hosted app is ready
to use; you do not need to install Java or configure a database. Keep the
browser on the same site while using UniChope so your session is retained.

### 1.2 Sign in as a Tutor

Tutors cannot self-register. Ask an Admin for a Tutor account and initial
password. On your first sign-in with that password, UniChope requires you to
set and confirm a new password before opening your Tutor workspace. Use a
password between 8 and 128 characters. After it is changed, sign in again with
the new password. An Admin must assign you to an active module before you can
offer slots for it. The role belongs to your account, so you do not choose a
role at sign-in. Use **Sign out** when you finish, especially on a shared
computer. Existing Tutor accounts will also receive this one-time prompt when
the password-change update is deployed.

Sessions expire after 30 minutes of inactivity. Sign in again if UniChope
returns you to the sign-in page. An account reactivated by an Admin must sign
in again.

![UniChope sign-in page with email and password fields.](images/sign-in.png)

After authenticating with the initial password, complete the required
password-change screen before proceeding:

![Required first-login Tutor password-change screen.](images/tutor-first-login-password-change.png)

## 2. Tutor

### Workspace tour

| Tab | What you can do |
| --- | --- |
| Availability | Review upcoming slots, create slots for assigned modules, and cancel available slots |
| Bookings | Review consultations, filter the list, and complete a consultation |
| Slot history | Review past slots and open completed consultations |
| Consultation notes | Load and save private notes and request an AI note draft |

The screenshots below show the Tutor interface with synthetic sample data.

Times are shown in SGT. On **Availability**, use **Refresh slots** after
another user may have changed a slot. Follow the workspace message after each
action.

### First consultation walkthrough

1. An Admin assigns your Tutor account to an active module.
2. Sign in, open **Availability**, select the module, choose a future date and
   time, then select **Create slot**. The slot appears as `AVAILABLE`.
3. Once the slot is booked, it appears in **Bookings** as `ACTIVE` and the
   slot becomes `BOOKED`.
4. After the consultation, select the booking and choose **Mark complete**.
   The booking and slot become `COMPLETED` together.
5. Open **Consultation notes**, select the completed consultation, choose
   **Edit note** to write or revise a note, then select **Save note**. To use
   an AI draft, review it, choose **Use reviewed draft**, and save the note.

Choose a future date and time when creating a slot. The server checks for
conflicts when saving; if another slot overlaps yours, revise the time and
submit again.

### Availability

#### View upcoming availability

The **Availability** table shows your upcoming `AVAILABLE` and `BOOKED` slots.
Use the SGT date filter to see a particular day, select **All upcoming** to
clear the date filter, or select **Refresh slots** to reload the latest
information.

#### Create a slot

Choose an active assigned module, a future date, and a start and end time.
The end must be later than the start. UniChope rejects a slot that overlaps
one of your existing `AVAILABLE` or `BOOKED` slots. Adjacent slots are
allowed.

**Example:** Select `CS3227`, choose **30/10/2026**, enter `10:00` and
`10:30`, and select **Create slot**. The new row appears as `AVAILABLE`.

#### Cancel an available slot

Select an `AVAILABLE` slot, choose **Cancel slot**, and confirm. A
`BOOKED` slot cannot be cancelled by the Tutor; its Student must cancel the
booking first. Cancelled slots remain in history.

![Tutor availability page with slot creation controls, filters, and upcoming slots.](images/tutor-availability.png)

*Example screen. The displayed records are synthetic.*

### Bookings

The **Bookings** tab shows consultations for your slots, including the
Student, module, SGT date and time, and status. Filter by module, start date,
status, or student name, then select **Apply filters**. Empty results mean
there are no matching bookings; change or clear the filters to see other
records.

To complete a consultation, select its `ACTIVE` booking and choose **Mark
complete** after the consultation. Confirm the action. The booking and slot
become `COMPLETED` together. Completion can be recorded when a consultation
finishes early; only complete it after the consultation has actually ended.

![Tutor bookings page with filters and an active consultation.](images/tutor-bookings.png)

*Example screen. The displayed records are synthetic.*

### Slot history

#### Review slot history

Slot history shows `CANCELLED` or `COMPLETED` slots, one status at a time.
Choose a status and optional SGT date or student name, then select **Load
history**. Leave the date blank to see all matching history for the selected
status. The separate completed-consultation list used for notes is not changed
by the slot-history filter.

![Tutor slot history with a completed consultation and its Open note action.](images/tutor-slot-history.png)

*Example screen. The displayed records are synthetic.*

### Consultation notes

#### Load and save a note

Select a completed consultation from the list. Its saved note loads
automatically. Choose **Edit note** to enter or revise the note, then choose
**Save note**. The note belongs to that consultation and remains available
after signing out and back in. If another session has saved a newer version,
reload the saved note before saving to avoid overwriting that newer note.

#### Generate and review an AI note draft

Enter rough notes for the selected completed consultation and choose
**Prepare AI draft**, then select **Generate draft**. The assistant suggests a
structured draft with **Summary**,
**Topics discussed**, and **Follow-up** sections. Review the content and edit
it as needed. Choose **Use reviewed draft** to place the reviewed text in the
note editor, then choose **Save note** to save it.

For clearer drafts, write short, factual points about one consultation. Include
the topic, what was discussed, and any agreed follow-up. Separate confirmed
details from questions or plans, and leave out information that is not needed.
For example:

> Topic: Recursion base cases. Discussed tracing a recursive function with a
> small input. Follow-up: Student will attempt two practice questions before
> the next session.

AI text is only a suggestion, not a verified record. It may omit facts or add
unsupported details, so compare every statement with what happened and remove
anything you cannot verify. Generation alone does not save a note or change a
booking. Do not include unnecessary personal or sensitive information in rough
notes. If generation fails, times out, or returns no usable draft, your rough
notes remain available; write the note manually or try again later. A **FAKE
RESULT** badge means the app returned deterministic local test output, not a
SoC response.

![Tutor consultation notes with a saved note and an editable AI draft for review.](images/tutor-consultation-notes.png)

*Example screen. The displayed records are synthetic; the AI draft shown is a local test result.*

## Admin and authentication backend

This increment provides APIs, not a browser interface. A client must keep the session
cookie, obtain a CSRF token and include its returned header/token on every mutation.
Use HTTPS outside localhost. See src/backend/README.md for server/database setup.

1. GET /api/auth/csrf to obtain {headerName,token} and an anonymous cookie.
2. POST /api/auth/login with {email,password}, cookie and CSRF header.
3. Fetch a fresh CSRF token after login. GET /api/auth/me returns id,name,role
   and mustChangePassword.
4. If mustChangePassword is true, POST /api/auth/change-password with
   {password,confirmation} and the CSRF token. Both values must match and
   contain 8–128 characters. Success returns 204 and revokes the session, so
   the Tutor signs in again with the new password.
5. Otherwise, call only the APIs for the stored role. POST /api/auth/logout
   signs out; fetch another CSRF token before a new login. A session expires
   after 30 minutes idle.

Public POST /api/auth/register accepts name,email,password,confirmation and creates
a Student without signing in. Passwords are 8–128 Unicode code points;
confirmation must match. Password reset is not provided. Admin-created
Student/Tutor accounts use the initial password supplied securely by the Admin.
New Tutor accounts require a password change after the first sign-in, before
Tutor APIs are available. Student and Admin accounts are not subject to this
Tutor first-login requirement.

Admin users can list accounts, create Student/Tutor accounts and activate/deactivate
those accounts. Admin accounts cannot be changed through these APIs. Deactivation
is blocked by active bookings and, for Tutors, future available slots. Deactivation
revokes previous sessions; reactivation requires a new login for those sessions.

Admin modules can be created, edited and deactivated. Assignment endpoints assign
or unassign active Tutors/modules. Resolve active bookings and future available
slots before deactivation/unassignment; history is retained. Repeated no-op state
changes do not duplicate records. No history deletion or module reactivation is offered.

GET /api/admin/bookings accepts optional moduleId,tutorId,date,status. Date means
Singapore slot start date. GET /api/admin/statistics returns all-time booking counts
and completion/cancellation percentages; every booking record is in the denominator.
Empty data gives0percent. Lists use stable ordering and are unpaginated.

POST /api/admin/activity-report with {} returns the statistics snapshot and a draft
explanation requiring review. Reports have no side effects and cannot change records.
Empty data returns a no-data message without AI. Local fake mode is labelled fake
and does not call SoC. Real AI is unavailable until the shared gateway is integrated;
manual operations and statistics still work. Never treat an explanation as verified
fact or as permission to perform an operation.

Errors use {code,message,requestId}.401 requires login;403 may mean wrong role,
inactive account or invalid CSRF;409 indicates conflicting current state/duplicate
record;429 means wait for Retry-After;503 indicates temporary unavailability.
Refresh after an uncertain mutation result before resubmitting creation requests.
Exact endpoint/body shapes are in workflow/admin/TECHNICAL-SPEC.md and
workflow/auth/TECHNICAL-SPEC.md. No frontend or deployment is claimed by this guide.

## 4. Troubleshooting and limitations

### I cannot sign in

Check that you are using the account supplied for your role and the correct
production site. Ask an Admin to confirm that your account is active or
provide the initial password securely. There is no self-service password
reset.

### No slots appear

Ask an Admin to confirm your assignment to an active module. Check that you
selected the correct SGT date and created a future slot. Booked, cancelled,
and past slots are not available to book.

### A slot or Admin action is rejected

Read the workspace message. A slot may have been booked or withdrawn since
the list was refreshed. Active bookings or future available slots can block
account, module, or assignment changes. Refresh the relevant list before
trying again.

### Current limitations

- There is no self-service password reset, email verification, or NUS SSO.
- Consultations cannot be rescheduled as one action. The Student must cancel
  an eligible booking, then the Tutor can cancel the released slot and create
  a replacement.
- The module list is managed by Admins and is not synchronized with a live NUS
  course catalogue.
- AI output is a suggestion and requires human review. It cannot complete a
  consultation or save a note by itself.

## Frequently asked questions

### Account and sign-in

#### What timezone does UniChope use?

Dates and times throughout the app are shown in Singapore time (SGT).

#### Do I need an account, and how do I get one?

Students can register for a Student account from the sign-in page. Admins create
Student and Tutor accounts; ask your organization's UniChope administrator for
access if you are a Tutor or Admin. Your role is assigned to your account and
cannot be selected at sign-in.

#### Why was I signed out?

Sessions expire after 30 minutes of inactivity. Sign in again. If an Admin
deactivated and then reactivated your account, you must also sign in again before
using the app.

#### Can I reset a forgotten password myself?

No. UniChope has no self-service password reset. Contact an Admin through your
usual support channel.

### Student FAQs

#### How do I find and book a consultation?

Browse future available slots, filter by module or date, and select a slot to
book. A booking is confirmed only if the slot is still available and does not
overlap another active booking of yours. Refresh the list if a slot is no longer
available.

#### Can I cancel a booking?

You can cancel your own upcoming active booking before its start time. Cancelling
releases the slot for another Student. Past or already completed bookings cannot
be cancelled.

#### Can a Student see Tutor consultation notes?

No. Tutor consultation notes are private to the Tutor and are not shown to
Students.

#### Does the AI agenda answer my questions or book a consultation for me?

No. The agenda is a preparation suggestion based on your questions. Review it
yourself; AI does not book, cancel, or change consultations. If AI is unavailable,
you can still use the normal booking features.

### Tutor FAQs

#### Can I create my own Tutor account?

No. Ask an Admin for a Tutor account and an active module assignment.

#### What happens when I sign in with my initial password?

UniChope asks you to set and confirm a password between 8 and 128 characters.
The Tutor workspace stays unavailable until the change succeeds. Your current
session is then revoked; sign in again with the new password.

#### Why can’t I cancel a booked slot?

The Student must cancel the booking first. Once the slot is available again,
you can cancel it from **Availability**. See the [availability screen](images/tutor-availability.png).

#### What happens when I mark a booking complete?

After the consultation has ended, choose **Mark complete**. The booking and
its slot are marked `COMPLETED` together. See the [bookings screen](images/tutor-bookings.png).

#### Does generating an AI draft save my note?

No. Review the draft, choose **Use reviewed draft**, then choose **Save note**.
See the [consultation notes screen](images/tutor-consultation-notes.png).

#### Who can see my consultation notes?

Notes are private to you and belong to the completed consultation. Reload the
saved note before saving if another session may have changed it.

### Admin FAQs

#### What can an Admin manage?

Admins can manage Student and Tutor accounts, modules, and Tutor-module
assignments, and review booking activity and statistics. Admin actions may be
rejected when active bookings or future available slots would be affected.

#### Can an Admin read Tutor consultation notes?

No. Admin booking activity and statistics exclude private Tutor notes.

#### Does an AI activity report change bookings or accounts?

No. It returns a draft explanation of aggregate statistics for review. It cannot
change bookings, accounts, modules, or assignments. Real AI reports may be
unavailable while the shared service is being configured; ordinary Admin
management and statistics remain available.
