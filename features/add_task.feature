Feature: Add Task

Background:
  Given a fresh task board with no tasks

# Add Task 1
Scenario Outline: Add Task 1
  When I add a task titled <title> with notes <notes> and priority <priority>
  Then the saved task count is <saved_count>
  Examples:
    | title                     | notes                 | priority | saved_count |
    | Renew domain registration | Expires end of month  | High     | 1           |
    | Book dentist              |                       | Low      | 1           |
    |                           | notes without a title | Medium   | 0           |

# Add Task 2
Scenario Outline: Add Task 2
  When I add a task titled <title> with notes <notes> and priority <priority>
  Then the board contains a task titled <title> with notes <notes>, priority <priority>, and completion false
  Examples:
    | title                     | notes                | priority |
    | Renew domain registration | Expires end of month | High     |
    | Reply to design feedback  |                      | Medium   |
