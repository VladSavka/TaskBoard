Feature: Filter Tasks

Background:
  Given a fresh task board
  And the board has a task titled "Renew domain registration"
  And the board has a task titled "Reply to design feedback"
  And the board has a task titled "Book dentist"
  And the board finishes loading

# Filter Tasks 1
Scenario Outline: Filter Tasks 1
  When I search tasks for <query>
  Then the list shows <visible_count> tasks
  Examples:
    | query  | visible_count |
    | design | 1             |
    | RENEW  | 1             |
    | e      | 3             |
    |        | 3             |

# Filter Tasks 2
Scenario Outline: Filter Tasks 2
  When I search tasks for <query>
  Then the list shows a task titled <shown_title>
  And the list hides a task titled <hidden_title>
  Examples:
    | query  | shown_title               | hidden_title              |
    | design | Reply to design feedback  | Renew domain registration |
    | renew  | Renew domain registration | Book dentist              |
