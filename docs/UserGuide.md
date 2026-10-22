# User guide

This guide covers signing in, Student account registration and the Admin workspace.
Open UniChope using the website address provided by your team.

## Sign in and sign out

1. Enter your account email and password on the **Sign in** page.
2. Select **Sign in**. Your account role determines which workspace opens.
3. Select **Sign out** in the page header when you have finished.

If you received an account from an administrator, use the initial password they
provided. Passwords are case-sensitive. If sign-in fails, check your details and
try again. After too many failed attempts, wait for the time shown before retrying.

A session expires after 30 minutes without activity. If the application asks you
to sign in again, do so before continuing. An inactive account cannot sign in;
contact your administrator if you need access restored.

Password reset and password change are not available in this version.

## Create a Student account

1. On the sign-in page, select **Create a Student account**.
2. Enter your name and email address.
3. Enter a password of 8–128 characters and enter it again in **Confirm password**.
   Spaces count as part of the password, and both entries must match exactly.
4. Select **Create account**.
5. After successful registration, sign in with your new email and password.

Registration creates a Student account. Tutor accounts are created by an Admin.
If an account cannot be created with your email, try signing in or use another
email address. If the outcome could not be confirmed, try signing in before
submitting the registration again.

## Find your way around the Admin workspace

After signing in as an Admin, use the navigation to open:

| Page | Use |
| --- | --- |
| **Overview** | View booking totals, percentages and breakdowns. |
| **Users** | Create Student/Tutor accounts and manage their access. |
| **Modules** | Create, edit and deactivate teaching modules. |
| **Tutor assignments** | Assign tutors to modules or remove assignments. |
| **Booking activity** | View and filter consultation booking records. |
| **Activity report assistant** | |

Select **Refresh** to reload the records on a page. On smaller screens, scroll
horizontally through the navigation to reach the other pages. Use the theme button
in the header to switch between light and dark appearance.

## Manage users

### Create an account

1. Open **Users**.
2. Under **Create account**, choose **Student** or **Tutor**.
3. Enter the person's full name and email address.
4. Enter an initial password of 8–128 characters.
5. Select **Create account** and wait for **Change saved**.
6. Share the account email and initial password securely with the account owner.

The password field clears after submission. Existing passwords are not displayed.
You cannot create Admin accounts or change account roles from this page.

### Find an account

Under **Accounts**, use **Filter role** and **Filter account status** to narrow the
list. Each row shows the person's name, email, role and current access status.

### Deactivate or reactivate an account

1. Find the Student or Tutor in **Accounts**.
2. Select **Deactivate** or **Reactivate**.
3. Read the confirmation message. Select **Go back** to leave the account unchanged,
   or **Confirm change** to proceed.
4. Wait for confirmation and check the updated status.

Deactivation prevents access and retains the account's history. Reactivation
restores access; previously revoked sessions require a new sign-in. Admin accounts
are marked **Protected account** and cannot be changed here.

A Student cannot be deactivated while they have an active booking. A Tutor cannot
be deactivated while affected active bookings or future available slots remain.
Resolve those commitments through the relevant Student or Tutor workflow, then
refresh the account list before trying again.

## Manage modules

### Create a module

1. Open **Modules**.
2. Under **Create module**, enter a module code and module name.
3. Use 1–20 letters, digits or hyphens for the code, and up to 200 characters for
   the name. Module codes must be unique.
4. Select **Create module** and wait for confirmation.

### Edit a module

1. Select **Edit** beside the module.
2. Change its code or name in the form.
3. Select **Save module**, or **Cancel editing** to discard the form changes.

Editing keeps the module's existing bookings and history associated with it.

### Deactivate a module

1. Select **Deactivate** beside an active module.
2. Read the message and select **Confirm change**, or **Go back** to cancel.
3. Check that the module is marked **Inactive**.

Active bookings and future available slots can prevent deactivation. Resolve those
commitments before trying again. Deactivation retains history; module deletion and
reactivation are not offered in this interface.

## Manage tutor assignments

### Assign a tutor to a module

1. Open **Tutor assignments**.
2. Under **Assign a tutor**, choose an active Tutor and an active module.
3. Select **Assign tutor** and wait for confirmation.
4. Check the pair under **Current assignments**.

Only active Tutors and active modules appear as choices. Assigning a pair that
already exists does not create a duplicate.

### Remove an assignment

1. Find the pair under **Current assignments**.
2. Select **Remove**.
3. Read the message and select **Confirm change**, or **Go back** to cancel.

Removal is blocked while that Tutor/module pair has active bookings or future
available slots. Other assignments are unaffected, and booking history is retained.

## View booking activity

1. Open **Booking activity**.
2. Narrow the records using **Module**, **Tutor**, **Status** or
   **Consultation date (SGT)**. Filters apply together.
3. Select **Reset filters** to show all records again, or **Refresh** to reload.

The table shows the module, Student, Tutor, consultation time and booking status.
Dates and times use Singapore time (SGT). The date filter refers to the consultation's
start date. Statuses are **ACTIVE**, **COMPLETED** and **CANCELLED**.

Bookings are read-only on this page. Admins do not cancel or complete bookings here,
and private consultation notes are not displayed.

## Read the overview statistics

Open **Overview** to see:

- Total booking records.
- Completed and cancelled booking counts.
- Completion and cancellation percentages.
- Booking counts by module and Tutor.

Statistics cover all booking history, including inactive accounts/modules and
cancelled bookings. A cancelled booking followed by a new booking counts as two
records. Both percentages use all booking records as their denominator. For example,
two completed records out of four total records gives a 50% completion rate.
When there are no bookings, counts and percentages are zero.

Check the snapshot capture time to see when the figures were read. Select
**Refresh** for a new snapshot.

## Activity report assistant

## If something goes wrong

| Message or situation | What to do |
| --- | --- |
| Session expired or sign-in requested | Sign in again before continuing. |
| Account inactive | Contact your administrator about restoring access. |
| Access denied | Check that you are using the correct account and workspace. |
| Details already exist | Check the account email or module code; refresh the list before creating again. |
| Change blocked by bookings or slots | Resolve the relevant active bookings or future available slots, then refresh and retry. |
| Security token expired | Submit again as prompted; the application obtains a fresh token. |
| Request limit reached | Wait before trying again. |
| Service unavailable | Try **Refresh** or try again later. |
| Outcome could not be confirmed | Refresh and inspect the records before resubmitting. The first change may already have completed. |

Wait for each submitted change to finish. Leaving the page does not guarantee that
an operation already sent has been cancelled.
