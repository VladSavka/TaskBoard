Feature: Sample Seed

  On launch the board is populated from a built-in sample seed of four tasks
  (the challenge's sample data) so the app opens with real content instead of an
  empty screen. The board begins this load automatically the moment it is created,
  with no separate trigger from the UI, so the screen never stays stuck on the
  loading indicator and the add-task control appears as soon as the load settles.
  The same seed constant feeds the app's data source and this spec. The seed
  reaches the data source through the app's dependency-injection graph, which must
  use Hilt (per the architecture article) rather than a hand-rolled graph; the
  acceptance harness constructs objects directly and depends on no DI framework.

Background:
  Given the app's default seed

# Sample Seed 1
Scenario Outline: Sample Seed 1
  When the board finishes loading
  Then the list shows <task_count> tasks
  Examples:
    | task_count |
    | 4          |

# Sample Seed 2
Scenario Outline: Sample Seed 2
  When the board finishes loading
  Then the list shows a task titled <title> with priority <priority> and completion <done>
  Examples:
    | title                                                                       | priority | done  |
    | Renew domain registration                                                   | High     | false |
    | Reply to design feedback                                                    | Medium   | false |
    | Book dentist                                                                | Low      | true  |
    | Migrate the analytics pipeline to the new warehouse and validate dashboards | Medium   | false |

# Sample Seed 3
Scenario Outline: Sample Seed 3
  When the board launches
  Then the list state is content
  And the list shows <task_count> tasks
  And an add-task action is available
  Examples:
    | task_count |
    | 4          |
