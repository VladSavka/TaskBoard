#!/usr/bin/env bb
;; Project runner adapter for the APS gherkin-mutator (`--runner-worker`).
;;
;; Contract (see swarmforge/aps mutator-spec.md, "Runner Adapter"):
;;   - Persistent worker: one newline-delimited JSON job request per stdin line,
;;     one newline-delimited JSON job response per stdout line.
;;   - stdout is reserved for protocol JSON ONLY. All diagnostics go to stderr.
;;   - Job request : {id, feature_json, generated_dir, work_dir, timeout}
;;   - Job response: {id, outcome, output, error, duration}
;;       outcome in #{"test_success" "test_failure" "infrastructure_error"}
;;       duration is nanoseconds.
;;
;; This adapter's protocol plumbing is complete and ready. The single project
;; integration point is `run-generated-acceptance` below: it must execute the
;; generated acceptance entry points against the mutated IR (feature_json) and
;; return a pass/fail. Until the acceptance runtime + generated entry points
;; exist for this project, it reports `infrastructure_error` so a mutation run
;; fails loudly and diagnosably rather than silently passing.

(require '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[babashka.classpath :as cp]
         '[babashka.fs :as fs])

;; Put the project acceptance sources (runtime + step handlers) on the classpath
;; so a mutated IR is executed IN-PROCESS in this one persistent worker -- no
;; per-mutation subprocess -- which keeps long soft-mutation runs fast. Resolve
;; relative to this script so cwd does not matter.
(def ^:private src-dir
  (str (fs/path (fs/parent (fs/canonicalize *file*)) "src")))
(cp/add-classpath src-dir)
(require '[aps-project.runtime :as rt]
         '[aps-project.handlers :as handlers])

(defn- respond! [resp]
  ;; One protocol line to stdout, flushed. Never write anything else to stdout.
  (println (json/generate-string resp))
  (flush))

(defn- log! [& xs]
  (binding [*out* *err*]
    (apply println xs)
    (flush)))

(defn- summary-line [result]
  (str (:passed result) " passed, " (:failed result) " failed"))

(defn- failure-detail [result]
  (->> (:results result)
       (remove :ok)
       (map (fn [r] (str (:name r) ": " (:message r))))
       (str/join "\n")))

(defn run-generated-acceptance
  "PROJECT INTEGRATION POINT.
   Execute the project acceptance suite against the mutated IR at
   (:feature_json job) -- a JSON IR file the mutator has written -- and map the
   run to the runner-worker outcome:
     all scenarios pass -> test_success  (the mutation SURVIVED: assertions did
                                          not detect the changed example value);
     any scenario fails  -> test_failure  (the mutation was KILLED);
     the run cannot start/complete -> infrastructure_error.
   The IR is run in-process via aps-project.runtime, whose step handlers drive
   the real Android-free product classes through bin/product-harness."
  [job]
  (let [ir-path (:feature_json job)]
    (cond
      (str/blank? (str ir-path))
      {:outcome "infrastructure_error" :output "" :error "job had no feature_json path"}

      (not (fs/exists? ir-path))
      {:outcome "infrastructure_error" :output ""
       :error (str "mutated IR not found: " ir-path)}

      :else
      (let [result (rt/run-ir ir-path handlers/handlers)]
        (if (:ok result)
          {:outcome "test_success" :output (summary-line result) :error ""}
          {:outcome "test_failure" :output (summary-line result)
           :error (failure-detail result)})))))

(defn- handle [job]
  (let [start (System/nanoTime)
        result (try
                 (run-generated-acceptance job)
                 (catch Throwable t
                   {:outcome "infrastructure_error"
                    :output ""
                    :error (str "runner-adapter threw: " (.getMessage t))}))]
    (-> result
        (assoc :id (:id job)
               :duration (- (System/nanoTime) start))
        (update :output #(or % ""))
        (update :error #(or % "")))))

(defn -main []
  (with-open [r (io/reader *in*)]
    (doseq [line (line-seq r)]
      (when-not (clojure.string/blank? line)
        (let [job (try (json/parse-string line true)
                       (catch Throwable t
                         (log! "runner-adapter: bad job line:" (.getMessage t))
                         nil))]
          (when job
            (respond! (handle job))))))))

(-main)
