Feature: Task Detail

  Tapping a task on the list navigates to a separate detail/edit screen that shows
  the task's current fields (title, notes, priority) for viewing and editing.
  Saving persists the edits (see Edit Task) and returns to the list; leaving the
  screen closes it. The open task and its fields are exposed as observable UI state
  so the detail screen renders from the presenter, not from the row.

# Task Detail 1
Scenario Outline: Task Detail 1
  Given a fresh task board
  When I add a task titled <title> with notes <notes> and priority <priority>
  And I open the task titled <title>
  Then the detail shows title <title>, notes <notes>, and priority <priority>
  Examples:
    | title        | notes             | priority |
    | Book dentist | Annual checkup    | Low      |
    | Renew cert   | Expires next week | High     |

# Task Detail 2
Scenario Outline: Task Detail 2
  Given the board has a task titled <title>
  When I open the task titled <title>
  Then a task detail is open
  Examples:
    | title                     |
    | Renew domain registration |

# Task Detail 3
Scenario Outline: Task Detail 3
  Given the board has a task titled <title>
  When I open the task titled <title>
  And I change the title to <new_title>, the notes to <new_notes>, and the priority to <new_priority>
  And I save the task
  Then no task detail is open
  Examples:
    | title        | new_title     | new_notes | new_priority |
    | Book dentist | Book eye exam | Yearly    | High         |

# Task Detail 4
Scenario: Task Detail 4
  Given a fresh task board
  Then no task detail is open
