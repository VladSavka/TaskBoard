Feature: Add Task Entry Point

  The board offers an add-task affordance — a Material 3 floating action button —
  from every settled state (empty, content, and error), so a task can always be
  added, especially from the empty "no tasks yet" screen. The affordance is hidden
  only while the initial load is still in flight. All user-facing text is supplied
  by string resources (strings.xml), not hardcoded literals; the empty-state
  message is identified by a stable resource key so it can be asserted.

Background:
  Given a fresh task board

# Add Task Entry Point 1
Scenario Outline: Add Task Entry Point 1
  Given the task source has <task_count> tasks
  When the board finishes loading
  Then the list state is <state>
  And an add-task action is available
  Examples:
    | task_count | state   |
    | 0          | empty   |
    | 3          | content |

# Add Task Entry Point 2
Scenario: Add Task Entry Point 2
  Given loading tasks will fail
  When the board finishes loading
  Then the list state is error
  And an add-task action is available

# Add Task Entry Point 3
Scenario: Add Task Entry Point 3
  Given a task source read that has not completed
  When the board is loading
  Then the list state is loading
  And an add-task action is not available

# Add Task Entry Point 4
Scenario Outline: Add Task Entry Point 4
  Given the task source has <task_count> tasks
  When the board finishes loading
  Then the empty message key is <message_key>
  Examples:
    | task_count | message_key |
    | 0          | tasks_empty |
