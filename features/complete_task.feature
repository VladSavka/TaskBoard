Feature: Complete Task

Background:
  Given a fresh task board

# Complete Task 1
Scenario Outline: Complete Task 1
  Given the board has a task titled <title> with completion <initial_done>
  When I toggle completion of the task titled <title>
  Then the task titled <title> has completion <final_done>
  Examples:
    | title                    | initial_done | final_done |
    | Book dentist             | true         | false      |
    | Reply to design feedback | false        | true       |
