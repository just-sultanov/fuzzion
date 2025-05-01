(ns example.core-fuzzer
  (:require
    [example.core :as sut]
    [fuzzion.core :as f]))


(f/deftarget parse-json
  [input]
  (try
    (sut/parse-json (f/consume-remaining-as-string input))
    (catch Exception _)))


(f/deftarget square
  [input]
  (try
    (when (= 4 (sut/square (f/consume-long input)))
      (throw (f/issue :high "You found a bug")))
    (catch ArithmeticException _)))
