(ns aps-project.handlers
  "Project step handlers: connect Gherkin step text to Task Board behavior.

  Handlers use regex patterns that capture placeholder names, then read the
  matching example values by name. Given/When steps accumulate a list of harness
  commands in the world; Then steps append a query command, run the whole
  sequence through the headless `bin/product-harness` in one JVM invocation, and
  assert on its single line of output.

  The harness protocol: each command is one process argument whose fields are
  tab-separated (titles/notes may contain spaces but never tabs)."
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))

(defn- example-str
  "Fetch a required example value by placeholder name, failing when missing."
  [example name]
  (let [raw (get example name)]
    (when (nil? raw)
      (throw (ex-info (str "missing example value: " name) {:name name})))
    raw))

(defn- harness-script
  "Locate bin/product-harness by walking up from the working directory."
  []
  (let [candidates (mapcat (fn [dir]
                             [(fs/path dir "bin" "product-harness")
                              (fs/path dir "acceptance" "bin" "product-harness")])
                           (take 5 (iterate fs/parent (fs/absolutize "."))))]
    (or (some #(when (fs/exists? %) (str %)) candidates)
        (throw (ex-info "product-harness not found from working directory" {})))))

(defn- harness
  "Run the headless product harness with args, returning its trimmed stdout."
  [& args]
  (let [{:keys [exit out err]}
        (apply p/shell {:out :string :err :string :continue true} (harness-script) args)]
    (when-not (zero? exit)
      (throw (ex-info (str "harness " (str/join " " args) " failed: " err) {})))
    (str/trim out)))

(defn- assert=
  [expected actual what]
  (when (not= expected actual)
    (throw (ex-info (str what ": expected " (pr-str expected) " but was " (pr-str actual)) {})))
  true)

(defn- command
  "Build a single tab-separated harness command from its parts."
  [& parts]
  (str/join "\t" parts))

(defn- add-command
  "Append a harness command (built from parts) to the world's command list."
  [world & parts]
  (update world :commands (fnil conj []) (apply command parts)))

(defn- ask
  "Run the accumulated commands plus a final query command through the harness
  and return its output."
  [world & query-parts]
  (apply harness (conj (vec (:commands world)) (apply command query-parts))))

(def handlers
  [;; --- board setup (Given) -------------------------------------------------
   {:pattern #"^a fresh task board(?: with no tasks)?$"
    :fn (fn [world _ _] (assoc world :commands (or (:commands world) [])))}

   {:pattern #"^the task source has <([A-Za-z0-9_]+)> tasks$"
    :fn (fn [world example [count-name]]
          (add-command world "count" (example-str example count-name)))}

   {:pattern #"^the task source has a task titled <([A-Za-z0-9_]+)> with priority <([A-Za-z0-9_]+)> and completion <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name priority-name done-name]]
          (add-command world "seed"
                       (example-str example title-name) ""
                       (example-str example priority-name)
                       (example-str example done-name)))}

   {:pattern #"^the board has a task titled <([A-Za-z0-9_]+)> with completion <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name done-name]]
          (add-command world "seed"
                       (example-str example title-name) "" "Medium"
                       (example-str example done-name)))}

   {:pattern #"^the board has a task titled <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name]]
          (add-command world "seed" (example-str example title-name) "" "Medium" "false"))}

   {:pattern #"^a task source read that has not completed$"
    :fn (fn [world _ _] (add-command world "fault" "pending"))}

   {:pattern #"^loading tasks will fail$"
    :fn (fn [world _ _] (add-command world "fault" "fail"))}

   {:pattern #"^loading tasks will fail once and then return <([A-Za-z0-9_]+)> tasks$"
    :fn (fn [world example [count-name]]
          (-> world
              (add-command "fault" "fail_once")
              (add-command "count" (example-str example count-name))))}

   ;; --- actions (When) ------------------------------------------------------
   {:pattern #"^the board (?:finishes loading|is loading|launches)$"
    :fn (fn [world _ _] (add-command world "load"))}

   {:pattern #"^I retry loading$"
    :fn (fn [world _ _] (add-command world "retry"))}

   {:pattern #"^I add a task titled <([A-Za-z0-9_]+)> with notes <([A-Za-z0-9_]+)> and priority <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name notes-name priority-name]]
          (add-command world "add"
                       (example-str example title-name)
                       (example-str example notes-name)
                       (example-str example priority-name)))}

   {:pattern #"^I add a task titled <([A-Za-z0-9_]+)> with due date <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name due-name]]
          (add-command world "add_due"
                       (example-str example title-name)
                       (example-str example due-name)))}

   {:pattern #"^I set the due date to <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [due-name]]
          (add-command world "set_due" (example-str example due-name)))}

   {:pattern #"^I toggle completion of the task titled <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name]]
          (add-command world "toggle" (example-str example title-name)))}

   {:pattern #"^I delete the task titled <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name]]
          (add-command world "delete" (example-str example title-name)))}

   {:pattern #"^I undo the delete$"
    :fn (fn [world _ _] (add-command world "undo"))}

   {:pattern #"^I open the task titled <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name]]
          (add-command world "open" (example-str example title-name)))}

   {:pattern #"^I change the title to <([A-Za-z0-9_]+)>, the notes to <([A-Za-z0-9_]+)>, and the priority to <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name notes-name priority-name]]
          (add-command world "edit"
                       (example-str example title-name)
                       (example-str example notes-name)
                       (example-str example priority-name)))}

   {:pattern #"^I save the task$"
    :fn (fn [world _ _] (add-command world "save"))}

   ;; --- assertions (Then) ---------------------------------------------------
   {:pattern #"^the list state is (?:<([A-Za-z0-9_]+)>|([a-z]+))$"
    :fn (fn [world example [state-name literal]]
          (let [expected (if state-name (example-str example state-name) literal)]
            (assert= expected (ask world "state") "list state"))
          world)}

   {:pattern #"^a retry action is available$"
    :fn (fn [world _ _]
          (assert= "true" (ask world "retry_available") "retry available")
          world)}

   {:pattern #"^the list shows <([A-Za-z0-9_]+)> tasks$"
    :fn (fn [world example [count-name]]
          (assert= (example-str example count-name) (ask world "visible_count") "visible task count")
          world)}

   {:pattern #"^the list shows a task titled <([A-Za-z0-9_]+)> with priority <([A-Za-z0-9_]+)> and completion <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name priority-name done-name]]
          (assert= "true"
                   (ask world "shown"
                        (example-str example title-name)
                        (example-str example priority-name)
                        (example-str example done-name))
                   "task shown")
          world)}

   {:pattern #"^the saved task count is <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [count-name]]
          (assert= (example-str example count-name) (ask world "saved_count") "saved task count")
          world)}

   {:pattern #"^the task titled <([A-Za-z0-9_]+)> has completion <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name done-name]]
          (assert= (example-str example done-name)
                   (ask world "completion" (example-str example title-name))
                   "task completion")
          world)}

   {:pattern #"^the board contains a task titled <([A-Za-z0-9_]+)> with notes <([A-Za-z0-9_]+)>, priority <([A-Za-z0-9_]+)>, and completion false$"
    :fn (fn [world example [title-name notes-name priority-name]]
          (assert= "true"
                   (ask world "contains_full"
                        (example-str example title-name)
                        (example-str example notes-name)
                        (example-str example priority-name)
                        "false")
                   "board contains task")
          world)}

   {:pattern #"^the board contains a task titled <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name]]
          (assert= "true" (ask world "contains_title" (example-str example title-name))
                   "board contains task")
          world)}

   {:pattern #"^the board does not contain a task titled <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name]]
          (assert= "false" (ask world "contains_title" (example-str example title-name))
                   "board does not contain task")
          world)}

   ;; --- due dates -----------------------------------------------------------
   {:pattern #"^today is \"([^\"]+)\"$"
    :fn (fn [world _ [date]] (add-command world "today" date))}

   {:pattern #"^the board has a task titled <([A-Za-z0-9_]+)> due on <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name due-name]]
          (add-command world "seed_due"
                       (example-str example title-name)
                       (example-str example due-name)))}

   {:pattern #"^the board has a task titled <([A-Za-z0-9_]+)> with no due date$"
    :fn (fn [world example [title-name]]
          (add-command world "seed" (example-str example title-name) "" "Medium" "false"))}

   {:pattern #"^the task titled <([A-Za-z0-9_]+)> shows due label <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name label-name]]
          (assert= (example-str example label-name)
                   (ask world "due_label" (example-str example title-name))
                   "due label")
          world)}

   {:pattern #"^the task titled <([A-Za-z0-9_]+)> shows no due label$"
    :fn (fn [world example [title-name]]
          (assert= "" (ask world "due_label" (example-str example title-name)) "due label")
          world)}

   ;; --- filter / sort setup (literal-title seeds) ---------------------------
   {:pattern #"^the board has a task titled \"([^\"]+)\" with priority \"([^\"]+)\" and completion \"([^\"]+)\"$"
    :fn (fn [world _ [title priority done]]
          (add-command world "seed" title "" priority done))}

   {:pattern #"^the board has a task titled \"([^\"]+)\"$"
    :fn (fn [world _ [title]]
          (add-command world "seed" title "" "Medium" "false"))}

   ;; --- filter --------------------------------------------------------------
   {:pattern #"^I search tasks for <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [query-name]]
          (add-command world "search" (example-str example query-name)))}

   {:pattern #"^the list shows a task titled <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name]]
          (assert= "true" (ask world "shown_title" (example-str example title-name)) "task shown")
          world)}

   {:pattern #"^the list hides a task titled <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name]]
          (assert= "false" (ask world "shown_title" (example-str example title-name)) "task hidden")
          world)}

   ;; --- sort ----------------------------------------------------------------
   {:pattern #"^I sort tasks by <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [sort-name]]
          (add-command world "sort" (example-str example sort-name)))}

   {:pattern #"^the task at position <([A-Za-z0-9_]+)> is titled <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [position-name title-name]]
          (assert= (example-str example title-name)
                   (ask world "title_at" (example-str example position-name))
                   "task at position")
          world)}

   ;; --- preserve state ------------------------------------------------------
   {:pattern #"^the screen is recreated$"
    :fn (fn [world _ _] (add-command world "recreate"))}

   {:pattern #"^the active search query is <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [query-name]]
          (assert= (example-str example query-name) (ask world "active_query") "active search query")
          world)}

   {:pattern #"^the active sort is <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [sort-name]]
          (assert= (example-str example sort-name) (ask world "active_sort") "active sort")
          world)}

   ;; --- theme preference ----------------------------------------------------
   {:pattern #"^the system is in dark mode <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [flag-name]]
          (add-command world "theme_system_dark" (example-str example flag-name)))}

   {:pattern #"^I select the <([A-Za-z0-9_]+)> theme$"
    :fn (fn [world example [mode-name]]
          (add-command world "theme_select" (example-str example mode-name)))}

   {:pattern #"^dark mode is <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [flag-name]]
          (assert= (example-str example flag-name) (ask world "app_in_dark_mode") "app in dark mode")
          world)}

   {:pattern #"^the selected theme is ([A-Za-z]+)$"
    :fn (fn [world _ [mode]]
          (assert= mode (ask world "selected_theme") "selected theme")
          world)}

   {:pattern #"^the theme options are \"([^\"]+)\"$"
    :fn (fn [world _ [options]]
          (assert= options (ask world "theme_options") "theme options")
          world)}

   ;; --- add-task entry point ------------------------------------------------
   {:pattern #"^an add-task action is available$"
    :fn (fn [world _ _]
          (assert= "true" (ask world "add_action_available") "add-task action available")
          world)}

   {:pattern #"^an add-task action is not available$"
    :fn (fn [world _ _]
          (assert= "false" (ask world "add_action_available") "add-task action not available")
          world)}

   {:pattern #"^the empty message key is <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [key-name]]
          (assert= (example-str example key-name) (ask world "empty_message_key") "empty message key")
          world)}

   ;; --- mock network save outcomes ------------------------------------------
   {:pattern #"^the network will fail the next save$"
    :fn (fn [world _ _] (add-command world "net_fail_next_save"))}

   {:pattern #"^the network will succeed the next save$"
    :fn (fn [world _ _] (add-command world "net_succeed_next_save"))}

   {:pattern #"^the save failed$"
    :fn (fn [world _ _]
          (assert= "failed" (ask world "save_result") "save result")
          world)}

   {:pattern #"^the save succeeded$"
    :fn (fn [world _ _]
          (assert= "succeeded" (ask world "save_result") "save result")
          world)}

   ;; --- sample seed ---------------------------------------------------------
   {:pattern #"^the app's default seed$"
    :fn (fn [world _ _] (add-command world "seed_default"))}

   ;; --- task detail ---------------------------------------------------------
   {:pattern #"^the detail shows title <([A-Za-z0-9_]+)>, notes <([A-Za-z0-9_]+)>, and priority <([A-Za-z0-9_]+)>$"
    :fn (fn [world example [title-name notes-name priority-name]]
          (assert= "true"
                   (ask world "detail_shows"
                        (example-str example title-name)
                        (example-str example notes-name)
                        (example-str example priority-name))
                   "detail fields")
          world)}

   {:pattern #"^a task detail is open$"
    :fn (fn [world _ _]
          (assert= "true" (ask world "detail_open") "task detail open")
          world)}

   {:pattern #"^no task detail is open$"
    :fn (fn [world _ _]
          (assert= "false" (ask world "detail_open") "no task detail open")
          world)}])
