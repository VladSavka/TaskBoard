(ns aps-project.generator-spec
  (:require [speclj.core :refer :all]
            [aps-project.generator :as g]
            [cheshire.core :as json]
            [babashka.fs :as fs]))

(def sample-ir
  {"name" "Smoke"
   "scenarios" [{"name" "adds"
                 "steps" [{"keyword" "Then" "text" "the sum is <sum>" "parameters" ["sum"]}]
                 "examples" [{"sum" "5"}]}]})

(defn- with-tmp [f]
  (let [dir (fs/create-temp-dir)]
    (try (f (str dir))
         (finally (fs/delete-tree dir)))))

(describe "metadata-name mapping"
  (it "maps feature paths with the strict lowercase-and-hyphen rule"
    (should= "features-hunt-the-wumpus-feature.json"
             (g/metadata-name "features/Hunt The Wumpus.feature"))
    (should= "features-orders-cancel-order-feature.json"
             (g/metadata-name "features/orders/Cancel Order.feature"))
    (should= "features-api-v2-happy-path-feature.json"
             (g/metadata-name "Features/API v2/Happy Path.feature"))))

(describe "generate"
  (it "writes a deterministic runnable entry point that delegates to the runtime"
    (with-tmp
      (fn [dir]
        (let [ir-path (str dir "/smoke.json")]
          (spit ir-path (json/generate-string sample-ir))
          (let [out (str dir "/generated")
                r1 (g/generate ir-path out)
                content1 (slurp (:test-file r1))]
            (should= 0 (:exit r1))
            (should (fs/exists? (:test-file r1)))
            (should-contain "run-ir" content1)
            (should-contain "aps-project.runtime" content1)
            ;; deterministic for fixed IR
            (fs/delete-tree out)
            (let [r2 (g/generate ir-path out)]
              (should= content1 (slurp (:test-file r2)))
              (should= (:implementation-hash r1) (:implementation-hash r2))))))))

  (it "writes conforming metadata with a generated-files-only hash"
    (with-tmp
      (fn [dir]
        (let [ir-path (str dir "/smoke.json")]
          (spit ir-path (json/generate-string sample-ir))
          (let [out (str dir "/generated")
                r (g/generate ir-path out)
                meta-file (str out "/metadata/" (g/metadata-name (:feature-path r)))
                m (json/parse-string (slurp meta-file) true)]
            (should (fs/exists? meta-file))
            (should= 1 (:schema_version m))
            (should= "generated_files" (:hash_scope m))
            (should-contain "sha256:" (:implementation_hash m))
            (should= ir-path (:ir_path m))
            (should= 1 (count (:generated_files m)))))))))

(describe "run exit codes"
  (it "returns 2 on wrong argument count"
    (should= 2 (g/run []))
    (should= 2 (g/run ["only-one"]))
    (should= 2 (g/run ["a" "b" "c"])))

  (it "returns 1 when the JSON IR input does not exist"
    (with-tmp
      (fn [dir]
        (should= 1 (g/run [(str dir "/missing.json") (str dir "/out")]))))))

(run-specs)
