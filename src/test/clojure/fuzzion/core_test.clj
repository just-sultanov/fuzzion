(ns fuzzion.core-test
  (:require
    [clojure.test :refer [deftest is]]
    [fuzzion.core :as sut]))


(deftest ^:unit generate-class-name-test
  (is (= "example.core.DummyFuzzer"
         (sut/generate-class-name "example.core" "DummyFuzzer")
         (sut/generate-class-name "example.core" "dummy-fuzzer"))))
