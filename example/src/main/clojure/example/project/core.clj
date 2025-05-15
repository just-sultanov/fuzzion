(ns example.project.core
  (:require
    [jsonista.core :as json]))


(defn parse-json
  [s]
  (json/read-value s json/keyword-keys-object-mapper))


(defn square
  [x]
  (* x x))
