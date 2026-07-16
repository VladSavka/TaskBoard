Feature: Delete Task

Background:
  Given a fresh task board

# Delete Task 1
Scenario Outline: Delete Task 1
  Given the board has a task titled <kept_title>
  And the board has a task titled <deleted_title>
  When I delete the task titled <deleted_title>
  Then the board does not contain a task titled <deleted_title>
  And the board contains a task titled <kept_title>
  Examples:
    | kept_title                | deleted_title             |
    | Reply to design feedback  | Renew domain registration |
    | Renew domain registration | Reply to design feedback  |

# Delete Task 2
Scenario Outline: Delete Task 2
  Given the board has a task titled <title>
  When I delete the task titled <title>
  And I undo the delete
  Then the board contains a task titled <title>
  Examples:
    | title                     |
    | Renew domain registration |
