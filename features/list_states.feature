Feature: Task List States

Background:
  Given a fresh task board

# Task List States 1
Scenario Outline: Task List States 1
  Given the task source has <task_count> tasks
  When the board finishes loading
  Then the list state is <state>
  Examples:
    | task_count | state   |
    | 0          | empty   |
    | 4          | content |

# Task List States 2
Scenario: Task List States 2
  Given a task source read that has not completed
  When the board is loading
  Then the list state is loading

# Task List States 3
Scenario: Task List States 3
  Given loading tasks will fail
  When the board finishes loading
  Then the list state is error
  And a retry action is available

# Task List States 4
Scenario Outline: Task List States 4
  Given loading tasks will fail once and then return <task_count> tasks
  When the board finishes loading
  And I retry loading
  Then the list state is content
  And the list shows <task_count> tasks
  Examples:
    | task_count |
    | 3          |

# Task List States 5
Scenario Outline: Task List States 5
  Given the task source has a task titled <title> with priority <priority> and completion <done>
  When the board finishes loading
  Then the list shows a task titled <title> with priority <priority> and completion <done>
  Examples:
    | title                                                                       | priority | done  |
    | Renew domain registration                                                   | High     | false |
    | Reply to design feedback                                                    | Medium   | false |
    | Book dentist                                                                | Low      | true  |
    | Migrate the analytics pipeline to the new warehouse and validate dashboards | Medium   | false |
