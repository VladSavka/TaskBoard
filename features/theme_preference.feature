Feature: Theme Preference

  The theme is chosen from a single icon button in the app bar that opens a
  dialog listing the available modes. Selecting a mode updates the app's
  appearance immediately; System follows the device's dark-mode setting.

Background:
  Given a fresh task board

# Theme Preference 1
Scenario Outline: Theme Preference 1
  Given the system is in dark mode <system_dark>
  When I select the <mode> theme
  Then dark mode is <app_in_dark_mode>
  Examples:
    | mode   | system_dark | app_in_dark_mode |
    | Dark   | false       | true             |
    | Light  | true        | false            |
    | System | true        | true             |
    | System | false       | false            |

# Theme Preference 2
Scenario: Theme Preference 2
  Then the selected theme is System

# Theme Preference 3
Scenario: Theme Preference 3
  Then the theme options are "System, Light, Dark"
