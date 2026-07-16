Feature: Preserve Board State

Background:
  Given a fresh task board
  And the board has a task titled "Renew domain registration"
  And the board has a task titled "Reply to design feedback"
  And the board finishes loading

# Preserve Board State 1
Scenario Outline: Preserve Board State 1
  Given I search tasks for <query>
  And I sort tasks by <sort>
  When the screen is recreated
  Then the active search query is <query>
  And the active sort is <sort>
  And the list state is content
  Examples:
    | query | sort       |
    | reply | priority   |
    | renew | completion |
