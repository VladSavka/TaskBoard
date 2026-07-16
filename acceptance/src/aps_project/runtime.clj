(ns aps-project.runtime
  "Acceptance runtime: expands JSON IR into scenario executions and dispatches
  each step to a project step handler. See ../.aps/acceptance-generator.md for
  the runtime and step-handler contract."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]))

(defn load-ir
  "Read parser-produced JSON IR from a path into a string-keyed map."
  [path]
  (json/parse-string (slurp path) false))

(defn- background-steps [ir]
  (or (get ir "background") []))

(defn- scenario-executions [ir scenario]
  (let [bg (background-steps ir)
        steps (vec (concat bg (get scenario "steps")))
        examples (get scenario "examples")
        rows (if (seq examples) examples [{}])]
    (map-indexed
     (fn [idx example]
       {:scenario (get scenario "name")
        :name (str (get scenario "name") "/example_" (inc idx))
        :steps steps
        :example example})
     rows)))

(defn executions
  "Expand every scenario in the IR into scenario executions. Scenarios with
  examples yield one execution per row; scenarios without examples yield a
  single execution with an empty example object. Background steps are prepended
  to every execution."
  [ir]
  (vec (mapcat #(scenario-executions ir %) (get ir "scenarios"))))

(defn- match-handler
  "Return [handler captures] for the first handler whose pattern matches the
  step text, or nil when no handler matches."
  [handlers text]
  (some (fn [handler]
          (when-let [m (re-matches (:pattern handler) text)]
            [handler (if (sequential? m) (vec (rest m)) [])]))
        handlers))

(defn- run-step [handlers world step example]
  (let [text (get step "text")
        matched (match-handler handlers text)]
    (when-not matched
      (throw (ex-info (str "Unsupported step: " text) {:step text})))
    (let [[handler captures] matched]
      ((:fn handler) world example captures))))

(defn run-execution
  "Run one scenario execution against the handlers with a fresh world object.
  Returns {:name .. :ok bool :message str-or-nil}."
  [handlers execution]
  (try
    (reduce (fn [world step]
              (run-step handlers world step (:example execution)))
            {}
            (:steps execution))
    {:name (:name execution) :ok true :message nil}
    (catch Exception e
      {:name (:name execution) :ok false :message (.getMessage e)})))

(defn run-ir
  "Run every scenario execution represented by the IR (path or map). Returns an
  aggregate {:ok bool :total :passed :failed :results [..]}."
  [ir-or-path handlers]
  (let [ir (if (string? ir-or-path) (load-ir ir-or-path) ir-or-path)
        results (mapv #(run-execution handlers %) (executions ir))
        failed (count (remove :ok results))]
    {:ok (zero? failed)
     :total (count results)
     :passed (- (count results) failed)
     :failed failed
     :results results}))
