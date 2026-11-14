# Personal Productivity & Task Manager

## Product Requirements, UX Blueprint and Development Roadmap

**Project type:** Personal native Android application\
**Primary development environment:** Google AI Studio -- Android Build
mode\
**Initial platform:** Android\
**Architecture:** Offline-first, local database, optional cloud sync\
**Primary goal:** Make it effortless to capture, organize, schedule,
complete and review personal work.

------------------------------------------------------------------------

## 1. Product vision

Build a polished personal productivity command centre---not merely a
basic to-do list. The app should help one person manage tasks, projects,
routines, deadlines, reminders and focus sessions from a single,
intuitive mobile workspace.

The experience should be:

-   Fast to open and easy to understand.
-   Optimized for one-handed mobile use.
-   Flexible without overwhelming the user.
-   Reliable when offline.
-   Consistent across light and dark themes.
-   Smooth during navigation, scrolling, gestures and drag-and-drop.
-   Safe against accidental deletion and data loss.
-   Useful without AI, with AI available as an optional enhancement.

### Product principles

1.  **Capture quickly:** A task should be addable in seconds.
2.  **Make the next action obvious:** Keep the Today screen focused on
    what matters now.
3.  **Organize flexibly:** Let users create categories and arrange work
    their own way.
4.  **Remind reliably:** Treat reminders as a core engineering
    subsystem.
5.  **Keep data local-first:** Core functionality must not depend on
    internet access.
6.  **Avoid productivity guilt:** Streaks and scores should be optional,
    not coercive.
7.  **Make every control real:** No decorative buttons or fake
    functionality.

------------------------------------------------------------------------

## 2. Target user and use cases

The initial app is for an individual who wants to organize multiple
areas of life, such as:

-   College, assignments, exams and practical work.
-   Personal responsibilities and errands.
-   Software projects and research.
-   Learning plans and courses.
-   Fitness, routines and habits.
-   Content creation and creative work.
-   Payments and other time-sensitive responsibilities.

Typical user journeys:

-   Quickly capture an idea in Inbox and organize it later.
-   Add an assignment to a College category with a deadline and
    reminders.
-   Drag tasks into a preferred order.
-   Review today's priorities on the dashboard.
-   Move an unfinished task to another date.
-   Run a focus session against a task.
-   Review completed work at the end of the week.
-   Export a backup before changing devices.

------------------------------------------------------------------------

## 3. Technology recommendation

Use a native Android project rather than a web page wrapped in an app.

  Layer                    Recommended technology
  ------------------------ ----------------------------------------------------------
  Development              Google AI Studio -- Android Build mode
  Language                 Kotlin
  UI                       Jetpack Compose
  Design system            Material 3 with custom tokens
  Architecture             MVVM + Repository
  Async/state              Kotlin Coroutines, Flow and StateFlow
  Local database           Room / SQLite
  Preferences              DataStore
  Navigation               Navigation Compose
  Scheduled reminders      AlarmManager where appropriate
  Background maintenance   WorkManager
  Optional cloud           Firebase Authentication + Cloud Firestore
  Optional AI              Gemini through an appropriate secure integration
  Testing                  Unit tests, Compose UI tests and physical-device testing

### Architecture rules

-   Keep UI, state management, business rules and data access separate.
-   Composables should render state and emit user actions; they should
    not contain database logic.
-   ViewModels expose observable UI state.
-   Repositories are the single interface to data sources.
-   Use immutable UI state and unidirectional data flow.
-   Use dependency injection where practical.
-   Keep date/time calculations in testable domain logic.
-   Use stable IDs rather than list positions as entity identity.
-   Add database migrations from the beginning.
-   Do not store the entire database as one JSON or preferences value.

### Offline-first strategy

**Version 1:** Room is the source of truth. Tasks, categories, reminders
metadata, settings and history work offline.

**Version 2:** Add optional account and cloud synchronization.

**Version 3:** Add backup history, restore, conflict resolution and
multi-device support.

The app must remain usable if the network or cloud service is
unavailable.

------------------------------------------------------------------------

## 4. Information architecture and navigation

Use four primary bottom-navigation destinations:

1.  **Today** -- daily dashboard, priorities and focus.
2.  **Tasks** -- Inbox, categories, projects and all tasks.
3.  **Calendar** -- month, week and day planning.
4.  **Progress** -- statistics, history and trends.

Persistent or easily accessible controls:

-   Floating Add Task action.
-   Global search.
-   Notification/reminder centre.
-   Settings.
-   Focus Mode shortcut.

### Navigation principles

-   Keep primary navigation to four destinations.
-   Avoid deep navigation for common actions.
-   Use bottom sheets for quick creation and lightweight editing where
    appropriate.
-   Use full screens for complex task details and settings.
-   Preserve scroll position and filters when navigating back.
-   Support Android system back predictably.
-   Provide accessible alternatives to gestures.
-   Avoid duplicate routes to the same screen unless they improve
    discoverability.

### Screen inventory

-   Welcome / onboarding.
-   Today dashboard.
-   Inbox.
-   All tasks.
-   Category list.
-   Category detail.
-   Task creation / quick add.
-   Task detail / edit.
-   Calendar month view.
-   Calendar week view.
-   Calendar day agenda.
-   Focus timer.
-   Focus history.
-   Progress dashboard.
-   Task history.
-   Reminder centre.
-   Search results.
-   Settings.
-   Backup / restore.
-   Optional AI assistant.

------------------------------------------------------------------------

## 5. Dashboard: Today

The Today screen is the app's home and should answer: **What matters
today, and what should I do next?**

### Recommended layout

**Top bar** - Time-aware greeting. - Current date. - Search action. -
Reminder centre indicator. - Settings/profile action.

**Daily progress** - Circular or linear completion indicator. -
Completed versus total tasks. - Optional daily target. - Keep the visual
calm and avoid shaming language.

**Overview** - Overdue. - Due today. - Upcoming. - Completed today.

**My Day task list** - Tasks ordered by user preference or selected
sorting mode. - Priority indicator. - Category color/icon. - Due-time
label. - Completion control. - Optional estimated duration.

**Quick actions** - Add task. - Add category. - Start focus session. -
Open calendar.

### Dashboard behavior

-   Tap a task to open details.
-   Swipe to complete only if the action is discoverable and undo is
    available.
-   Swipe to snooze/reschedule only with clear affordances.
-   Long-press to reorder.
-   Tap category chip to open that category.
-   Provide an Undo snackbar after completion or other reversible
    changes.
-   Do not automatically hide overdue tasks.

### Empty states

Provide useful, friendly empty states: - No tasks today: offer quick
task creation. - All tasks complete: show a calm completion state. - No
categories: suggest creating one or using Inbox. - No upcoming work:
explain how to schedule a task.

------------------------------------------------------------------------

## 6. Task management

### Task fields

  Field                Description
  -------------------- -----------------------------------------------------
  ID                   Stable unique identifier
  Title                Required task name
  Description          Optional details
  Category ID          Optional category relationship; Inbox if unassigned
  Priority             Low, Medium, High, Urgent
  Status               To do, In progress, Completed, Cancelled
  Created at           Creation timestamp
  Updated at           Last update timestamp
  Start date/time      Optional planned start
  Due date/time        Optional deadline
  All-day flag         Whether the date has no specific time
  Estimated duration   Optional expected effort
  Actual duration      Derived from tracked sessions if enabled
  Sort order           User-defined position
  Notes                Optional notes
  Tags                 Many-to-many labels
  Recurrence ID        Optional recurrence rule relationship
  Completed at         Completion timestamp
  Archived at          Optional archive timestamp

### Task creation modes

**Quick Add** - One text field for title. - Optional category and date
shortcuts. - Save immediately. - More options available without forcing
a long form.

**Detailed Add** - Title and description. - Category. - Priority. -
Start and due date/time. - Estimated duration. - Reminder
configuration. - Recurrence. - Subtasks. - Tags. - Notes and links.

### Date shortcuts

-   Today.
-   Tomorrow.
-   This weekend.
-   Next week.
-   Choose date.
-   No date.

### Task actions

-   Complete / reopen.
-   Edit.
-   Duplicate.
-   Move category.
-   Change priority.
-   Reschedule.
-   Add subtask.
-   Add note.
-   Archive.
-   Delete with confirmation or undo.
-   View history.
-   Convert to recurring task where appropriate.

### Task sections and filters

-   Inbox.
-   Today.
-   Upcoming.
-   Overdue.
-   In progress.
-   Completed.
-   Archived.

Filters: - Category. - Priority. - Status. - Date range. - Tag. - Has
reminder. - Overdue. - Completed.

Sorting: - Manual order. - Due date. - Priority. - Created date. -
Recently updated. - Alphabetical.

A task without a deadline must remain a first-class task and must not be
forced into a calendar time slot.

------------------------------------------------------------------------

## 7. Categories and workspaces

Users can create as many categories as needed.

Example starter categories:

  Category   Example tasks
  ---------- --------------------------------
  College    Assignments, exams, practicals
  Personal   Errands and responsibilities
  Projects   Development and research
  Learning   Courses and study goals
  Fitness    Workouts and routines
  Finance    Bills and payments
  Content    Video and creative work

### Category properties

-   Stable ID.
-   Name.
-   Description.
-   Icon.
-   Color.
-   Sort order.
-   Created/updated timestamps.
-   Archived flag.
-   Optional target or goal.

### Category actions

-   Create, edit and archive.
-   Delete with a safe task-handling choice.
-   Reorder.
-   Pin favorites.
-   View task count and completion percentage.
-   Open dedicated workspace.
-   Customize view and default sorting.

### Category deletion behavior

Never silently delete associated tasks. Offer explicit choices: - Move
tasks to Inbox. - Move tasks to another category. - Delete tasks as
well, with clear confirmation.

### Category layouts

Each category can support:

1.  **List view:** Compact, efficient task list.
2.  **Board view:** To do, In progress and Completed columns.
3.  **Calendar view:** Tasks by date.

Persist each category's selected view if practical.

------------------------------------------------------------------------

## 8. Drag-and-drop and organization

This is a core interaction, not a decorative enhancement.

### Required interactions

-   Reorder categories.
-   Reorder tasks within a list.
-   Move tasks between categories.
-   Move tasks between board columns.
-   Reorder subtasks.
-   Drag calendar tasks to another date to reschedule.

### UX requirements

-   Long-press or a clear drag handle starts movement.
-   Provide haptic feedback where supported.
-   Show elevation or visual lift while dragging.
-   Highlight valid drop targets.
-   Auto-scroll near screen edges.
-   Animate the item into its new position.
-   Persist the new order.
-   Prevent accidental drags from ordinary taps and scrolling.
-   Handle empty drop targets.
-   Show a clear result when a move is invalid.

### Accessibility and alternatives

Dragging must never be the only way to organize items. Provide: - Move
to category menu. - Move up/down actions. - Change status menu. -
Accessible semantics and labels.

### Ordering data

Use a dedicated sort-order field or a robust ordering strategy. Do not
use the current array index as an entity identifier. Ensure reorder
operations remain stable after database reloads and concurrent edits.

------------------------------------------------------------------------

## 9. Reminders and notification engine

**This is a critical subsystem.** A reminder feature that silently fails
is worse than no reminder feature.

### Reminder options

-   At scheduled time.
-   5, 10, 15 or 30 minutes before.
-   1 hour before.
-   1 day before.
-   Custom date and time.
-   Multiple reminders per task.
-   No reminder.

Example: A task due Friday at 6 PM can have reminders Thursday at 8 PM,
Friday at 10 AM and Friday at 5 PM.

### Notification actions

-   Complete.
-   Snooze 5 minutes.
-   Snooze 15 minutes.
-   Open task.
-   Reschedule, if the interaction can be implemented reliably.

### Advanced reminder settings

-   Daily briefing at a chosen time.
-   Evening review.
-   Optional overdue reminders.
-   User-defined reminder frequency and daily limit.
-   Quiet hours.
-   Per-task reminder enable/disable.
-   Reminder centre for upcoming, dismissed and completed reminders.

### Android implementation considerations

-   Request notification permission on Android 13 and later.
-   Explain why permission is needed before requesting it.
-   Use AlarmManager for user-visible scheduled reminders when
    appropriate.
-   Use exact alarms only when genuinely required and when the
    applicable permission/special access is available.
-   Use WorkManager for deferrable background maintenance, not as a
    replacement for precise alarms.
-   Recreate eligible reminders after reboot.
-   Reschedule after task date/time or reminder edits.
-   Cancel obsolete alarms after completion, deletion or disabling
    reminders.
-   Handle app updates, device time changes and time-zone changes.
-   Avoid duplicate alarms and notifications.
-   Clearly communicate when OS restrictions or denied permissions
    prevent reliable delivery.

Do not promise exact delivery on every device. Android battery
management and OEM restrictions can affect timing.

### Reminder scheduling rules

-   Store reminder configuration in the database.
-   Give each scheduled reminder a stable identifier.
-   Make scheduling idempotent.
-   Reconcile scheduled alarms against database state when the app
    starts and after relevant system events.
-   Use a single source of truth for due dates and recurrence.
-   Never schedule reminders for completed, cancelled or deleted tasks.
-   Define behavior for tasks with no due date.
-   Handle past trigger times explicitly.

------------------------------------------------------------------------

## 10. Recurring tasks and routines

Support: - Daily. - Every N days. - Selected weekdays. - Weekly. - Every
N weeks. - Monthly by date. - Monthly by weekday occurrence where
feasible. - Yearly. - Custom end date. - End after N occurrences.

### Recurrence behavior

Decide and document whether completion creates the next occurrence
immediately or whether future instances are materialized by a scheduler.
Avoid generating unlimited future rows.

Recommended approach: - Store a recurrence rule separately from task
instances. - Generate only a bounded upcoming window or the next
instance. - Use stable links between series and instances. - Prevent
duplicate generation. - Preserve completion history. - Define what
happens when a user edits one occurrence versus the entire series. -
Define behavior when a scheduled day does not exist, such as the 31st in
a shorter month.

Routines and habits can be built on top of recurrence but should have
their own history and completion views if needed.

------------------------------------------------------------------------

## 11. Calendar and scheduling

Provide three views:

### Month

-   Monthly grid.
-   Task indicators.
-   Overdue highlighting.
-   Tap a day to inspect tasks.
-   Quick task creation.

### Week

-   Seven-day overview.
-   Scheduled tasks.
-   All-day tasks.
-   Category and priority filters.

### Day

-   Chronological agenda.
-   Timed tasks.
-   All-day and unscheduled tasks in separate sections.
-   Quick rescheduling.

### Calendar interactions

-   Tap date to view tasks.
-   Drag task to another date to reschedule.
-   Filter by category and priority.
-   Show recurring tasks.
-   Create task for selected date.
-   Clearly distinguish due date from planned start time.

Do not infer that a task has a specific time if only a date was
supplied.

------------------------------------------------------------------------

## 12. Focus Mode and time tracking

### Timer presets

  Preset                   Work          Break
  -------------- -------------- --------------
  Pomodoro               25 min          5 min
  Deep work              50 min         10 min
  Long session           90 min         15 min
  Custom           User-defined   User-defined

### Features

-   Start focus session for a task.
-   Pause, resume and stop.
-   Short and long breaks.
-   Custom duration.
-   Session history.
-   Daily and weekly focus totals.
-   Optional end notification.
-   Record actual time against a task.

### Timer reliability

Do not rely on a continuously running foreground countdown. Store start
time, target end time, pause state and accumulated paused duration.
Derive remaining time from timestamps so the timer remains correct after
app minimization, process recreation or device sleep.

Define behavior for: - App process killed. - Device reboot. - User
changes device clock. - User starts a second session. - Session ends
while app is closed.

------------------------------------------------------------------------

## 13. Productivity insights

### Daily statistics

-   Total tasks.
-   Completed tasks.
-   Pending tasks.
-   Overdue tasks.
-   Completion percentage.
-   Focus duration.

### Weekly and monthly statistics

-   Tasks completed per day.
-   Category-wise completion.
-   Focus hours.
-   Completion trends.
-   Most active days.
-   Average completion time where data supports it.

### Productivity calendar

A heatmap can show activity over time. Make it clear what counts as
activity and allow users to disable or ignore streak-style indicators.

### Analytics principles

-   Do not fabricate precision from incomplete data.
-   Distinguish completed tasks from cancelled or skipped tasks.
-   Define how recurring instances are counted.
-   Avoid double-counting.
-   Make date ranges and time zones explicit.
-   Keep analytics useful and non-judgmental.

------------------------------------------------------------------------

## 14. Search, tags, notes and attachments

### Universal search

Search across: - Task titles. - Descriptions. - Notes. - Categories. -
Tags. - Completed and archived tasks, when selected.

Include: - Recent searches. - Filters. - Clear no-result state. - Fast
local search.

### Tags

-   Create and rename tags.
-   Optional color.
-   Assign multiple tags to tasks.
-   Filter by one or more tags.

### Notes and attachments

Potential support: - Plain-text notes. - URLs and reference links. -
Images and documents. - Optional file attachments.

Use Android's appropriate file picker and content URI permissions. Do
not assume a selected file path remains permanently accessible without
handling permissions and lifecycle correctly.

------------------------------------------------------------------------

## 15. AI-assisted features

AI is an optional enhancement. Basic task management must never require
a Gemini call.

### AI Quick Add

Example input:

> Complete my Java assignment tomorrow at 5 PM, high priority.

AI can propose: - Task title. - Date and time. - Priority. - Category
suggestion. - Estimated duration, only if requested or clearly marked as
a suggestion.

Always show a confirmation/edit screen before saving AI-interpreted task
data.

### AI task breakdown

User enters a large task. AI suggests: - Smaller subtasks. - A possible
order. - Optional estimated effort. - Potential milestones.

All suggestions must be editable and explicitly approved.

### Smart Daily Planner

Suggest an order for tasks based on: - Deadlines. - Priority. -
Estimated duration. - User-selected available hours. - Existing
scheduled commitments, if connected.

Never silently move or reschedule tasks. The user reviews and approves
the proposed plan.

### AI integration safeguards

-   Keep API keys out of source code and public builds.
-   Use a secure supported integration or backend for sensitive
    credentials.
-   Do not send all personal task data by default.
-   Let the user choose what context to share.
-   Handle network failures and rate limits.
-   Provide non-AI fallback behavior.
-   Validate structured model output before saving.
-   Never execute AI-generated actions without user confirmation.

------------------------------------------------------------------------

## 16. Backup, restore and cloud synchronization

### Local backup

Export: - Categories. - Tasks. - Subtasks. - Tags and relationships. -
Recurrence rules. - Reminder settings. - Preferences. - Relevant history
and focus sessions.

Use a versioned export format with schema version and export timestamp.

### Restore

-   Validate the file before modifying data.
-   Show a summary of what will be imported.
-   Detect duplicate IDs.
-   Define merge versus replace behavior.
-   Keep a recoverable backup before destructive replacement.
-   Handle older export versions.
-   Report partial failures clearly.

### Optional cloud sync

Later, integrate: - Firebase Authentication. - Firestore. - Secure
account handling. - Per-user data isolation. - Offline writes and sync
status. - Conflict resolution. - Account deletion and data export.

Cloud synchronization must not overwrite newer local work silently.
Define conflict handling and test simultaneous edits.

------------------------------------------------------------------------

## 17. Visual design system

### Design direction

Aim for a premium, minimal, calm and modern productivity interface.

### Themes

**Light mode** - Warm neutral or clean light background. - Clear text
hierarchy. - Restrained accent colors. - Subtle card surfaces.

**Dark mode** - Deep charcoal surfaces. - High-contrast readable text. -
Carefully restrained accent colors. - Avoid pure-black backgrounds
everywhere unless intentional.

### Design tokens

  Element         Recommendation
  --------------- -------------------------------------
  Design system   Material 3 customized
  Spacing         Consistent 8 dp grid
  Card corners    Approximately 14--20 dp
  Touch targets   Comfortable Android accessible size
  Animation       Subtle, typically 180--250 ms
  Themes          Light, dark, system
  Typography      Clear sans-serif hierarchy
  Icons           Consistent icon family
  Layout          Adapt to phone and tablet widths

### Motion and micro-interactions

-   Checkbox completion animation.
-   Smooth drag movement.
-   Subtle bottom-sheet transitions.
-   Progress indicator animation.
-   Haptic feedback on meaningful actions.
-   Animated navigation transitions.
-   Swipe feedback.
-   Skeleton or progress loading states where needed.

Do not animate every component. Maintain smooth scrolling and
responsiveness on modest devices.

### Accessibility

-   Sufficient color contrast.
-   Screen-reader labels.
-   Accessible control sizes.
-   Scalable text.
-   Support system font scaling.
-   Do not communicate priority by color alone.
-   Provide non-gesture alternatives.
-   Test with TalkBack.

------------------------------------------------------------------------

## 18. Onboarding

Keep onboarding short.

1.  Welcome screen.
2.  Choose light, dark or system theme.
3.  Create first category or use suggested categories.
4.  Configure reminder preferences.
5.  Explain notification permission when reminders are enabled.
6.  Open Today dashboard.

Do not force account creation. Users should be able to start
immediately.

------------------------------------------------------------------------

## 19. Data model proposal

Recommended Room entities:

  -----------------------------------------------------------------------
  Entity                              Important fields
  ----------------------------------- -----------------------------------
  Category                            id, name, description, icon, color,
                                      sortOrder, archivedAt

  Task                                id, title, description, categoryId,
                                      priority, status, startAt, dueAt,
                                      sortOrder, createdAt, updatedAt,
                                      completedAt

  Subtask                             id, taskId, title, completed,
                                      sortOrder

  Reminder                            id, taskId, triggerAt, enabled,
                                      notificationId, type

  RecurrenceRule                      id, taskId/seriesId, frequency,
                                      interval, selectedDays, endAt

  Tag                                 id, name, color

  TaskTag                             taskId, tagId

  FocusSession                        id, taskId, startedAt, endedAt,
                                      duration, status

  TaskHistory                         id, taskId, action, timestamp,
                                      metadata

  AppSettings                         theme, defaultView, quietHours,
                                      preferences
  -----------------------------------------------------------------------

### Data integrity requirements

-   Use stable primary keys.
-   Define foreign-key behavior intentionally.
-   Index common filters such as status, category and due date.
-   Use transactions for multi-step operations.
-   Use migrations for schema changes.
-   Keep timestamps consistent and store enough information to interpret
    local date/time correctly.
-   Define deletion and archival behavior.
-   Prevent orphaned subtasks, reminders and tag relationships.
-   Avoid storing derived values when they can be safely calculated.

------------------------------------------------------------------------

## 20. Development roadmap

Build in controlled phases. Do not ask the coding agent to generate
every feature in one enormous request.

### Phase 1 --- Foundation and visual prototype

-   Create native Android project.
-   Establish package and folder structure.
-   Build design tokens and reusable components.
-   Implement bottom navigation.
-   Create Today dashboard.
-   Create initial Tasks, Calendar and Progress screens.
-   Add onboarding and settings shell.
-   Verify compilation and navigation.

**Exit criteria:** Polished, navigable prototype with consistent visual
language.

### Phase 2 --- Database and core task management

-   Add Room entities and DAOs.
-   Add repositories and ViewModels.
-   Implement category CRUD.
-   Implement task CRUD.
-   Implement subtasks and task details.
-   Implement Inbox and task sections.
-   Add search, filtering and sorting.
-   Test persistence after process death and restart.

**Exit criteria:** Fully functional offline task and category manager.

### Phase 3 --- Organization and drag-and-drop

-   Implement category reordering.
-   Implement task reordering.
-   Implement cross-category movement.
-   Implement board columns.
-   Persist ordering.
-   Add accessible move alternatives.
-   Test rapid movement and empty drop targets.

**Exit criteria:** Organization works reliably and remains correct after
app restart.

### Phase 4 --- Reminder engine

-   Implement notification channels.
-   Request and explain notification permission.
-   Implement reminder persistence.
-   Schedule and cancel alarms.
-   Add notification actions.
-   Implement snooze.
-   Implement quiet hours.
-   Handle reboot and time changes.
-   Add reminder reconciliation.

**Exit criteria:** Reminder lifecycle passes physical-device testing.

### Phase 5 --- Recurrence and calendar

-   Implement recurrence rules.
-   Prevent duplicate instances.
-   Add month, week and day views.
-   Add date-based rescheduling.
-   Recalculate reminders after changes.
-   Test month-end and leap-year behavior.

**Exit criteria:** Calendar and recurring tasks remain consistent with
task data.

### Phase 6 --- Focus and insights

-   Implement timestamp-based focus timer.
-   Add session history.
-   Add daily and weekly summaries.
-   Add category analytics.
-   Add productivity calendar.

**Exit criteria:** Statistics are accurate and timer survives app
lifecycle events.

### Phase 7 --- Backup and intelligence

-   Implement versioned export/import.
-   Add restore validation.
-   Add optional Firebase sync.
-   Add AI Quick Add.
-   Add AI task breakdown.
-   Add smart planning suggestions.

**Exit criteria:** Backup recovery is safe; AI suggestions are validated
and user-approved.

### Phase 8 --- Quality and release

-   Test on physical Android devices.
-   Fix accessibility issues.
-   Optimize scrolling and database queries.
-   Check battery behavior.
-   Test notification permissions and OEM restrictions.
-   Add crash reporting if appropriate.
-   Prepare app icon, screenshots and release build.
-   Verify privacy and data handling.
-   Prepare Play Store listing if publishing.

**Exit criteria:** Stable release candidate with no known critical
data-loss or reminder bugs.

------------------------------------------------------------------------

## 21. Testing strategy and acceptance criteria

### Core functional tests

  -----------------------------------------------------------------------
  Area                    Scenario                Expected result
  ----------------------- ----------------------- -----------------------
  Task                    Create, edit and delete Database updates
                                                  correctly

  Persistence             Force-close and reopen  Data remains saved

  Category                Delete category with    No unintended data loss
                          tasks                   

  Drag-and-drop           Move task between       New category and order
                          categories              persist

  Sorting                 Reorder categories      Correct order after
                                                  restart

  Reminder                Edit reminder           Old alarm cancelled;
                                                  new alarm scheduled

  Reminder                Delete task             Associated alarms
                                                  cancelled

  Reboot                  Restart device          Eligible reminders are
                                                  recreated

  Permission              Deny notifications      App remains usable and
                                                  explains limitation

  Recurrence              Complete weekly task    Next occurrence
                                                  generated correctly

  Calendar                Move task to another    Database and reminder
                          date                    schedule update

  Time zone               Change device time zone Future reminders
                                                  recalculated

  Duplicate protection    Reopen app repeatedly   No duplicate recurring
                                                  tasks or alarms

  Backup                  Export and import       Data restored correctly

  Search                  Search completed tasks  Correct results
                                                  returned

  Performance             Load thousands of tasks Lists remain responsive

  Accessibility           Avoid drag gestures     Alternative actions
                                                  work
  -----------------------------------------------------------------------

### Edge cases

Test at least:

1.  Monthly recurrence on the 29th, 30th and 31st.
2.  Leap-year dates.
3.  Tasks without deadlines.
4.  Tasks due at midnight.
5.  Multiple reminders at the same time.
6.  Task completed immediately before notification delivery.
7.  Due date changed after an alarm was scheduled.
8.  Notifications disabled after reminders were created.
9.  Backup with duplicate IDs.
10. Backup from an older schema version.
11. Focus session interrupted by process death.
12. Device reboot during a focus session.
13. Category deletion while a task editor is open.
14. Rapid repeated completion taps.
15. Drag-and-drop into an empty category or column.
16. Device time changed manually.
17. Daylight-saving transitions in applicable time zones.
18. Very long task titles and descriptions.
19. Large numbers of subtasks.
20. App upgrade with an existing database.

### Quality gates

Do not move to the next major phase until: - The app compiles. -
Existing core workflows still work. - New features have meaningful
tests. - No major data integrity issue remains. - Navigation and back
behavior are correct. - Loading, empty and error states are present. -
The feature works without internet where expected.

------------------------------------------------------------------------

## 22. Security, privacy and reliability

-   Keep personal task data local by default.
-   Request only necessary Android permissions.
-   Explain why notifications are needed.
-   Do not log sensitive task descriptions.
-   Keep API keys and credentials out of client source code.
-   Use secure storage for sensitive tokens.
-   Validate imported files.
-   Avoid unsafe deserialization.
-   Provide a clear data export and deletion path.
-   Use account-level access controls if cloud sync is added.
-   Ensure one user's cloud data cannot be accessed by another user.
-   Make destructive actions reversible where practical.
-   Test database migration and backup restoration.

------------------------------------------------------------------------

## 23. Performance requirements

-   Use lazy lists for large task collections.
-   Avoid unnecessary recomposition.
-   Use stable keys in Compose lists.
-   Keep database queries off the main thread.
-   Index commonly filtered columns.
-   Avoid loading all task history into the dashboard.
-   Paginate or incrementally load large histories.
-   Keep drag-and-drop responsive.
-   Avoid expensive calculations during every frame.
-   Profile on a mid-range Android device, not only an emulator.
-   Ensure animations do not block input.

------------------------------------------------------------------------

## 24. Google AI Studio build workflow

Use Google AI Studio Android Build mode and work incrementally.

### Initial build prompt

``` text
Build a premium native Android personal productivity and task
management application using Kotlin and Jetpack Compose.

Use a customized Material 3 design system, MVVM architecture,
Repository pattern, Kotlin Coroutines and Flow, and Room for
local persistent storage.

Create a polished Today dashboard and four primary navigation
destinations: Today, Tasks, Calendar and Progress.

Create reusable UI components, a consistent light/dark theme,
proper navigation, accessible controls and responsive layouts.

The first implementation must include:
- Project structure and architecture.
- Design system.
- Bottom navigation.
- Today dashboard.
- Tasks and category screens.
- Task creation and detail screens.
- Room database foundation.
- Working task and category CRUD.
- Persistence after app restart.

All essential functionality must work offline.

Do not put all application logic in Composable functions.
Do not use hardcoded data as a substitute for database operations.
Do not create controls that do nothing.

Do not attempt to implement every future feature in this first
phase. Establish a stable, maintainable foundation first.

Run the project, resolve compilation errors and verify the
basic workflows before finishing.
```

### Follow-up build prompts

Run these individually after reviewing each implementation:

1.  Implement complete Room-backed task and category management.
2.  Implement drag-and-drop, manual ordering and category movement.
3.  Implement notification scheduling, notification actions and snooze.
4.  Implement recurring tasks and recurrence edge cases.
5.  Implement calendar views and drag-to-reschedule.
6.  Implement Focus Mode and Pomodoro sessions.
7.  Implement productivity statistics and charts.
8.  Implement versioned backup and restore.
9.  Implement optional Firebase synchronization.
10. Implement Gemini AI Quick Add and task breakdown.
11. Conduct comprehensive testing, accessibility and performance
    improvements.

### Instructions for every follow-up

Before changing code, ask the agent to: - Inspect the existing project
and architecture. - Identify files and modules that need changes. -
Preserve working functionality. - Avoid duplicating existing components
or logic. - Implement the feature in small steps. - Add or update
tests. - Run the app and resolve errors. - Summarize changed files and
known limitations.

Never accept a claim that something is implemented without testing the
actual behavior.

------------------------------------------------------------------------

## 25. Final recommended release scope

### Version 1 --- Essential personal productivity app

Must include: - Today dashboard. - Tasks and Inbox. - Unlimited
categories. - Task details and subtasks. - Priority and due dates. -
Drag-and-drop ordering. - List and board views. - Local database. -
Reliable scheduled reminders. - Snooze and notification actions. -
Search and filters. - Light and dark themes. - Backup and restore.

### Version 1.5 --- Planning and productivity

Add: - Calendar month/week/day views. - Recurring tasks. - Habits and
routines. - Focus Mode. - Productivity statistics. - Task history. -
More advanced sorting and filters.

### Version 2 --- Intelligent personal assistant

Add: - Gemini AI Quick Add. - AI task breakdown. - Smart daily
planning. - Optional Firebase account. - Cloud synchronization. -
Advanced analytics. - Home-screen widgets.

------------------------------------------------------------------------

## 26. Definition of success

The project is successful when a user can:

1.  Open the app and understand what needs attention today.
2.  Add a task in seconds.
3.  Organize it into a custom category.
4.  Rearrange tasks naturally with touch gestures.
5.  Set one or more reminders.
6.  Receive useful notifications while the app is closed, subject to
    Android system restrictions.
7.  Complete or snooze a task directly from a notification.
8.  Plan work in a calendar.
9.  Track focus sessions and completed work.
10. Close and reopen the app without losing data.
11. Back up and restore the complete workspace.
12. Use all core features without an internet connection.

**Final product philosophy:** Build a fast, beautiful, dependable
personal productivity system first. Add intelligence only where it
genuinely saves the user time. The best task manager is one that makes
organizing and completing work feel easier---not one that makes managing
the app itself another task.
