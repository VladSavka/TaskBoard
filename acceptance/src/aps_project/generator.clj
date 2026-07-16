(ns aps-project.generator
  "Acceptance entrypoint generator: turns parser JSON IR into a runnable
  acceptance test entry point plus conforming metadata. The generated entry
  point loads the JSON IR and delegates all step behavior to the acceptance
  runtime and project step handlers. See ../.aps/acceptance-generator.md."
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [babashka.fs :as fs])
  (:import [java.security MessageDigest]))

(defn metadata-name
  "Map a feature path to its metadata filename: lowercase, collapse each run of
  non-alphanumeric characters to a single hyphen, trim hyphens, append .json."
  [feature-path]
  (-> feature-path
      str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-+|-+$" "")
      (str "" ".json")))

(defn- sha256 [^String s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes s "UTF-8"))]
    (str "sha256:" (apply str (map #(format "%02x" %) digest)))))

(defn- ir-stem [ir-path]
  (-> ir-path fs/file-name (str/replace #"\.json$" "")))

(defn- feature-path-for
  "Derive the source feature path from the IR path using the project convention
  features/<stem>.feature."
  [ir-path]
  (str "features/" (ir-stem ir-path) ".feature"))

(defn- generated-test-source [ir-path]
  (str/join
   "\n"
   ["#!/usr/bin/env bb"
    ";; GENERATED acceptance entry point. Do not edit by hand."
    ";; Regenerate with: bb generate <json-ir> <out-dir>"
    "(require '[aps-project.runtime :as rt]"
    "         '[aps-project.handlers :as handlers])"
    (str "(def ir-path \"" ir-path "\")")
    "(let [result (rt/run-ir ir-path handlers/handlers)]"
    "  (doseq [r (:results result)]"
    "    (println (if (:ok r) \"PASS\" \"FAIL\") (:name r)"
    "             (if (:ok r) \"\" (str \"- \" (:message r)))))"
    "  (println (:passed result) \"passed,\" (:failed result) \"failed\")"
    "  (System/exit (if (:ok result) 0 1)))"
    ""]))

(defn generate
  "Generate the acceptance entry point and metadata for one IR file into out-dir.
  Returns a map with :exit, :test-file, :feature-path, :implementation-hash."
  [ir-path out-dir]
  (let [stem (ir-stem ir-path)
        feature-path (feature-path-for ir-path)
        test-file (str out-dir "/" stem "_acceptance_test.clj")
        source (generated-test-source ir-path)
        hash (sha256 source)
        rel-test-file (str (fs/file-name out-dir) "/" stem "_acceptance_test.clj")]
    (fs/create-dirs out-dir)
    (fs/create-dirs (str out-dir "/metadata"))
    (spit test-file source)
    (spit (str out-dir "/metadata/" (metadata-name feature-path))
          (json/generate-string
           {:schema_version 1
            :feature_path feature-path
            :ir_path ir-path
            :implementation_hash hash
            :hash_scope "generated_files"
            :generated_files [rel-test-file]}
           {:pretty true}))
    {:exit 0
     :test-file test-file
     :feature-path feature-path
     :implementation-hash hash}))

(defn run
  "Entrypoint returning an exit code. 0 success, 1 IO/generation error, 2 usage."
  [args]
  (if (not= 2 (count args))
    2
    (let [[ir-path out-dir] args]
      (cond
        (not (fs/exists? ir-path))
        (do (binding [*out* *err*]
              (println "gherkin IR not found:" ir-path))
            1)
        :else
        (try (:exit (generate ir-path out-dir))
             (catch Exception e
               (binding [*out* *err*] (println "generation error:" (.getMessage e)))
               1))))))

(defn -main [& args]
  (System/exit (run (vec args))))
