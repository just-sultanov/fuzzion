(ns example.core-fuzzer
  (:require
    [example.core :as sut]
    [fuzzion.core :as f]))


(f/deftarget parse-json
  [input]
  (try
    (sut/parse-json (f/consume-remaining-as-string input))
    (catch Exception _)))


(comment
  (binding [*compile-path* "target/classes"]
    (compile 'example.core-fuzzer)))
