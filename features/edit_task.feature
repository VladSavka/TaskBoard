Feature: Edit Task

Background:
  Given a fresh task board

# Edit Task 1
Scenario Outline: Edit Task 1
  Given the board has a task titled <original_title>
  When I open the task titled <original_title>
  And I change the title to <new_title>, the notes to <new_notes>, and the priority to <new_priority>
  And I save the task
  Then the board contains a task titled <new_title> with notes <new_notes>, priority <new_priority>, and completion false
  And the board does not contain a task titled <original_title>
  Examples:
    | original_title            | new_title             | new_notes         | new_priority |
    | Renew domain registration | Renew SSL certificate | Expires next week | Low          |
    | Book dentist              | Book optometrist      | Annual checkup    | High         |
