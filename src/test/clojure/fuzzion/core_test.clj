(ns fuzzion.core-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [fuzzion.core :as sut]))


(deftest ^:unit square-test
  (testing "dummy test"
    (is (= 4 (sut/square 2)))))
