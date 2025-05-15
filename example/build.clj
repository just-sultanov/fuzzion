(ns build
  (:require
    [clojure.tools.build.api :as b]))


(defn clean
  [_]
  (println "Cleaning...")
  (b/delete {:path "target"}))


(defn fuzz:compile
  [_]
  (println "Copying sources...")
  (b/copy-dir {:src-dirs ["src/main/clojure"  "src/main/resources"
                          "src/fuzz/clojure" "src/fuzz/resources"]
               :target-dir "target/classes"})
  (println "Compiling...")
  (b/compile-clj {:basis (b/create-basis {:project "deps.edn"
                                          :aliases [:fuzz]})
                  :ns-compile '[example.fuzzers.core-fuzzer]
                  :class-dir "target/classes"}))
