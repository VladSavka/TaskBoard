Feature: Sort Tasks

Background:
  Given a fresh task board
  And the board has a task titled "Book dentist" with priority "Low" and completion "true"
  And the board has a task titled "Renew domain registration" with priority "High" and completion "false"
  And the board has a task titled "Reply to design feedback" with priority "Medium" and completion "true"
  And the board finishes loading

# Sort Tasks 1
Scenario Outline: Sort Tasks 1
  When I sort tasks by <sort>
  Then the task at position <position> is titled <title>
  Examples:
    | sort       | position | title                     |
    | priority   | 1        | Renew domain registration |
    | priority   | 2        | Reply to design feedback  |
    | priority   | 3        | Book dentist              |
    | completion | 1        | Renew domain registration |
    | completion | 2        | Book dentist              |
    | completion | 3        | Reply to design feedback  |
