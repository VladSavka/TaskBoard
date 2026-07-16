Feature: Mock Network

  The app has no backend; its data source is a mock network layer. Reads carry an
  artificial delay (300-800 ms) and both loads and saves fail roughly 15% of the
  time, so the loading and error states are real. Latency and failure are driven
  by an injected delay provider and failure policy backed by a seedable RNG, so
  the behavior is deterministic under test; the layer is exposed via suspending
  functions. Read failures surface the error state with retry (see Task List
  States). This feature pins how save failures behave: the change is not persisted
  and the failure is surfaced as a one-shot error event.

Background:
  Given a fresh task board

# Mock Network 1
Scenario Outline: Mock Network 1
  Given the network will fail the next save
  When I add a task titled <title> with notes <notes> and priority <priority>
  Then the save failed
  And the saved task count is <saved_count>
  Examples:
    | title    | notes | priority | saved_count |
    | Book gym |       | Low      | 0           |

# Mock Network 2
Scenario Outline: Mock Network 2
  Given the network will succeed the next save
  When I add a task titled <title> with notes <notes> and priority <priority>
  Then the save succeeded
  And the saved task count is <saved_count>
  Examples:
    | title    | notes | priority | saved_count |
    | Book gym |       | Low      | 1           |
