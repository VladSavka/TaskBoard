Feature: Task Due Dates

Background:
  Given a fresh task board
  And today is "2026-07-15"

# Task Due Dates 1
Scenario Outline: Task Due Dates 1
  Given the board has a task titled <title> due on <due_date>
  When the board finishes loading
  Then the task titled <title> shows due label <label>
  Examples:
    | title                     | due_date   | label      |
    | Renew domain registration | 2026-07-15 | Today      |
    | Reply to design feedback  | 2026-07-16 | Tomorrow   |
    | Book dentist              | 2026-07-14 | Yesterday  |
    | File taxes                | 2026-07-18 | in 3 days  |
    | Archive logs              | 2026-07-10 | 5 days ago |

# Task Due Dates 2
Scenario Outline: Task Due Dates 2
  Given the board has a task titled <title> with no due date
  When the board finishes loading
  Then the task titled <title> shows no due label
  Examples:
    | title        |
    | Book dentist |

# Task Due Dates 3
Scenario Outline: Task Due Dates 3
  When I add a task titled <title> with due date <due_date>
  Then the task titled <title> shows due label <label>
  Examples:
    | title       | due_date   | label     |
    | File taxes  | 2026-07-18 | in 3 days |
    | Pay invoice | 2026-07-15 | Today     |

# Task Due Dates 4
Scenario Outline: Task Due Dates 4
  Given the board has a task titled <title>
  When I open the task titled <title>
  And I set the due date to <due_date>
  And I save the task
  Then the task titled <title> shows due label <label>
  Examples:
    | title        | due_date   | label     |
    | Book dentist | 2026-07-18 | in 3 days |
