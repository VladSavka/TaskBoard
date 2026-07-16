(ns aps-project.runtime-spec
  (:require [speclj.core :refer :all]
            [aps-project.runtime :as rt]))

;; Generic test handlers exercising placeholder capture, world state,
;; assertions, and failure paths independent of any product vocabulary.
(def test-handlers
  [{:pattern #"^the number <([A-Za-z0-9_]+)>$"
    :fn (fn [world example captures]
          (let [name (first captures)
                raw (get example name)]
            (when (nil? raw)
              (throw (ex-info (str "missing example value: " name) {})))
            (update world :numbers (fnil conj []) (Long/parseLong raw))))}
   {:pattern #"^the sum is <([A-Za-z0-9_]+)>$"
    :fn (fn [world example captures]
          (let [expected (Long/parseLong (get example (first captures)))
                actual (reduce + 0 (:numbers world))]
            (when (not= expected actual)
              (throw (ex-info (str "expected sum " expected " but was " actual) {})))
            world))}])

(def sum-scenario
  {"name" "adds"
   "steps" [{"keyword" "Given" "text" "the number <a>" "parameters" ["a"]}
            {"keyword" "And" "text" "the number <b>" "parameters" ["b"]}
            {"keyword" "Then" "text" "the sum is <sum>" "parameters" ["sum"]}]
   "examples" [{"a" "2" "b" "3" "sum" "5"}
               {"a" "10" "b" "1" "sum" "11"}]})

(describe "execution expansion"
  (it "creates one execution per example row with a one-based name"
    (let [execs (rt/executions {"name" "F" "scenarios" [sum-scenario]})]
      (should= 2 (count execs))
      (should= "adds/example_1" (:name (first execs)))
      (should= "adds/example_2" (:name (second execs)))
      (should= {"a" "2" "b" "3" "sum" "5"} (:example (first execs)))))

  (it "runs a scenario without examples once with an empty example object"
    (let [scenario {"name" "noex"
                    "steps" [{"keyword" "Then" "text" "the number <a>"}]
                    "examples" []}
          execs (rt/executions {"name" "F" "scenarios" [scenario]})]
      (should= 1 (count execs))
      (should= "noex/example_1" (:name (first execs)))
      (should= {} (:example (first execs)))))

  (it "prepends background steps to every execution"
    (let [ir {"name" "F"
              "background" [{"keyword" "Given" "text" "the number <a>"}]
              "scenarios" [sum-scenario]}
          execs (rt/executions ir)]
      (should= "the number <a>" (get-in (first execs) [:steps 0 "text"]))
      (should= 4 (count (:steps (first execs)))))))

(describe "run-ir"
  (it "passes when every scenario execution satisfies its handlers"
    (let [result (rt/run-ir {"name" "F" "scenarios" [sum-scenario]} test-handlers)]
      (should= true (:ok result))
      (should= 2 (:passed result))
      (should= 0 (:failed result))))

  (it "fails an execution whose assertion does not hold"
    (let [bad (assoc-in sum-scenario ["examples" 0 "sum"] "999")
          result (rt/run-ir {"name" "F" "scenarios" [bad]} test-handlers)]
      (should= false (:ok result))
      (should= 1 (:failed result))))

  (it "fails when a step text matches no handler"
    (let [scenario {"name" "bad" "steps" [{"keyword" "Then" "text" "an unsupported step"}] "examples" []}
          result (rt/run-ir {"name" "F" "scenarios" [scenario]} test-handlers)]
      (should= false (:ok result))
      (should-contain "Unsupported step" (-> result :results first :message))))

  (it "fails when a required example value is missing"
    (let [scenario (assoc sum-scenario "examples" [{"a" "2" "sum" "5"}])
          result (rt/run-ir {"name" "F" "scenarios" [scenario]} test-handlers)]
      (should= false (:ok result))
      (should-contain "missing example value" (-> result :results first :message)))))

(run-specs)
