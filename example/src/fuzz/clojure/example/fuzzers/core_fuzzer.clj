(ns example.fuzzers.core-fuzzer
  (:require
    [example.project.core :as sut]
    [fuzzion.core :as f]))


;; (f/deftarget parse-json
;;   {:skip true}
;;   [input]
;;   (try
;;     (sut/parse-json (f/consume-remaining-as-string input))
;;     (catch Exception _)))


(f/deftarget square
  [input]
  (try
    (when (= 4 (sut/square (f/consume-long input)))
      (throw (f/issue :high "You found a bug")))
    (catch ArithmeticException _)))


;; (f/deftarget square2
;;   [input]
;;   (try
;;     (when (= 4 (sut/square (f/consume-long input)))
;;       (throw (f/issue :high "You found a bug")))
;;     (catch ArithmeticException _)))
