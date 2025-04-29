(ns fuzzion.core
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.string :as str])
  (:import
    (com.code_intelligence.jazzer.api
      FuzzedDataProvider)))


;; Target class generator

(defn generate-class-name
  [package-name target-name]
  (format "%s.%s"
          (csk/->snake_case_string package-name)
          (csk/->PascalCaseString target-name)))


(defmacro deftarget
  ([target params body]
   `(deftarget ~target {} ~params ~body))
  ([target metadata [input] body]
   (let [target-name (name target)
         package-name (ns-name *ns*)
         class-name (generate-class-name package-name target-name)
         impl-prefix (format "-%s-" target-name)
         attr-map (assoc metadata ::target true, ::target-name target-name, ::target-class class-name)]
     (case (:input attr-map)
       :bytes
       `(do
          (gen-class
            :name ~class-name
            :methods ~'[^{:static true} [fuzzerTestOneInput [bytes] void]]
            :prefix ~impl-prefix
            :main false)

          (defn ~(symbol (str impl-prefix "fuzzerTestOneInput"))
            ~attr-map
            [^{:tag 'bytes} ~input]
            ~body))

       ;; FuzzedDataProvider by default
       `(do
          (gen-class
            :name ~class-name
            :methods ~'[^{:static true} [fuzzerTestOneInput [com.code_intelligence.jazzer.api.FuzzedDataProvider] void]]
            :prefix ~impl-prefix
            :main false)

          (defn ~(symbol (str impl-prefix "fuzzerTestOneInput"))
            ~attr-map
            [^{:tag 'com.code_intelligence.jazzer.api.FuzzedDataProvider} ~input]
            ~body))))))


;; Wrappers for FuzzedDataProvider
;; Link: https://github.com/CodeIntelligenceTesting/jazzer/blob/main/src/main/java/com/code_intelligence/jazzer/api/FuzzedDataProvider.java

(defn consume-boolean
  ^Boolean [^FuzzedDataProvider in]
  (.consumeBoolean in))


(defn consume-booleans
  ^"[Z" [^FuzzedDataProvider in ^Integer max-length]
  (.consumeBooleans in max-length))


(defn consume-byte
  (^Byte [^FuzzedDataProvider in]
   (.consumeByte in))
  (^Byte [^FuzzedDataProvider in ^Byte min ^Byte max]
   (.consumeByte in min max)))


(defn consume-bytes
  ^"[B" [^FuzzedDataProvider in ^Integer max-length]
  (.consumeBytes in max-length))


(defn consume-remaining-as-string
  ^String [^FuzzedDataProvider in]
  (.consumeRemainingAsString in))
