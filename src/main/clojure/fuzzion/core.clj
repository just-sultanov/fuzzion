(ns fuzzion.core
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.string :as str])
  (:import
    (com.code_intelligence.jazzer.api
      FuzzedDataProvider
      FuzzerSecurityIssueCritical
      FuzzerSecurityIssueHigh
      FuzzerSecurityIssueLow
      FuzzerSecurityIssueMedium)))


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
  "Consumes a `boolean` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Returns a `boolean`"
  [^FuzzedDataProvider in]
  (.consumeBoolean in))


(defn consume-booleans
  "Consumes a `boolean` array from the fuzzer input.

  The array will usually have `length`, but might be shorter if the fuzzer input
  is not sufficiently long.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `max-length` - the maximum length of the array

  Returns a `boolean` array of length at most `length`"
  ^"[Z" [^FuzzedDataProvider in max-length]
  (.consumeBooleans in max-length))


(defn consume-byte
  "Consumes a `byte` or `byte` between `min` and `max` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `min` - the inclusive lower bound on the returned value
    * `max` - the inclusive upper bound on the returned value

  Returns a `byte` or `byte` in range `[min, max]`"
  ([^FuzzedDataProvider in]
   (.consumeByte in))
  ([^FuzzedDataProvider in min max]
   (.consumeByte in min max)))


(defn consume-bytes
  "Consumes a `byte` array from the fuzzer input.

  The array will usually have `length`, but might be shorter if the fuzzer input
  is not sufficiently long.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `max-length` - the maximum length of the array

  Returns a `byte` array of length at most `length`"
  ^"[B" [^FuzzedDataProvider in max-length]
  (.consumeBytes in max-length))


(defn consume-remaining-as-bytes
  "Consumes the remaining fuzzer input as a `byte` array.

  **Note:** After calling this method, further calls to methods of this interface will
  return fixed values only.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Returns a `byte` array"
  ^"[B" [^FuzzedDataProvider in]
  (.consumeRemainingAsBytes in))


(defn consume-short
  "Consumes a `short` or `short` between `min` and `max` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `min` - the inclusive lower bound on the returned value
    * `max` - the inclusive upper bound on the returned value

  Returns a `short` or `short` in range `[min, max]`"
  ([^FuzzedDataProvider in]
   (.consumeShort in))
  ([^FuzzedDataProvider in min max]
   (.consumeShort in min max)))


(defn consume-shorts
  "Consumes a `short` array from the fuzzer input.

  The array will usually have `length`, but might be shorter if the fuzzer input
  is not sufficiently long.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `max-length` - the maximum length of the array

  Returns a `short` array of length at most `length`"
  ^"[S" [^FuzzedDataProvider in max-length]
  (.consumeShorts in max-length))


(defn consume-int
  "Consumes a `int` or `int` between `min` and `max` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `min` - the inclusive lower bound on the returned value
    * `max` - the inclusive upper bound on the returned value

  Returns a `int` or `int` in range `[min, max]`"
  ([^FuzzedDataProvider in]
   (.consumeInt in))
  ([^FuzzedDataProvider in min max]
   (.consumeInt in min max)))


(defn consume-ints
  "Consumes a `int` array from the fuzzer input.

  The array will usually have `length`, but might be shorter if the fuzzer input
  is not sufficiently long.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `max-length` - the maximum length of the array

  Returns a `int` array of length at most `length`"
  ^"[I" [^FuzzedDataProvider in max-length]
  (.consumeInts in max-length))


(defn consume-long
  "Consumes a `long` or `long` between `min` and `max` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `min` - the inclusive lower bound on the returned value
    * `max` - the inclusive upper bound on the returned value

  Returns a `long` or `long` in range `[min, max]`"
  (^long [^FuzzedDataProvider in]
   (.consumeLong in))
  (^long [^FuzzedDataProvider in ^long min ^long max]
   (.consumeLong in min max)))


(defn consume-longs
  "Consumes a `long` array from the fuzzer input.

  The array will usually have `length`, but might be shorter if the fuzzer input
  is not sufficiently long.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `max-length` - the maximum length of the array

  Returns a `long` array of length at most `length`"
  ^"[J" [^FuzzedDataProvider in ^long max-length]
  (.consumeLongs in max-length))


(defn consume-float
  "Consumes a `float` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Returns a `float` that may have a special value (e.g. a `NaN` or `Infinity`)"
  [^FuzzedDataProvider in]
  (.consumeFloat in))


(defn consume-regular-float
  "Consumes a `float` or `float` between `min` and `max` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `min` - the inclusive lower bound on the returned value
    * `max` - the inclusive upper bound on the returned value

  Returns a `float` or `float` in range `[min, max]` that is not a special value (e.g. not a `NaN`` or `Infinity`)"
  ([^FuzzedDataProvider in]
   (.consumeRegularFloat in))
  ([^FuzzedDataProvider in min max]
   (.consumeRegularFloat in min max)))


(defn consume-probability-float
  "Consumes a `float` between `0.0` and `1.0` (inclusive) from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Return a `float` in the range `[0.0, 1.0]`"
  [^FuzzedDataProvider in]
  (.consumeProbabilityFloat in))


(defn consume-double
  "Consumes a `double` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Returns a `double` that may have a special value (e.g. a `NaN` or `Infinity`)"
  ^double [^FuzzedDataProvider in]
  (.consumeDouble in))


(defn consume-regular-double
  "Consumes a `double` or `double` between `min` and `max` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `min` - the inclusive lower bound on the returned value
    * `max` - the inclusive upper bound on the returned value

  Returns a `double` or `double` in range `[min, max]` that is not a special value (e.g. not a `NaN`` or `Infinity`)"
  ([^FuzzedDataProvider in]
   (.consumeRegularDouble in))
  ([^FuzzedDataProvider in min max]
   (.consumeRegularDouble in min max)))


(defn consume-probability-double
  "Consumes a `double` between `0.0` and `1.0` (inclusive) from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Return a `double` in the range `[0.0, 1.0]`"
  [^FuzzedDataProvider in]
  (.consumeProbabilityDouble in))


(defn consume-char
  "Consumes a `char` or `char` between `min` and `max` from the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `min` - the inclusive lower bound on the returned value
    * `max` - the inclusive upper bound on the returned value

  Returns a `char` or `char` in range `[min, max]`"
  ([^FuzzedDataProvider in]
   (.consumeChar in))
  ([^FuzzedDataProvider in ^char min ^char max]
   (.consumeChar in min max)))


(defn consume-char-no-surragates
  "Consumes a `char` from the fuzzer input that is never a `UTF-16` surrogate character.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Returns a `char`"
  [^FuzzedDataProvider in]
  (.consumeCharNoSurrogates in))


(defn consume-string
  "Consumes a `String` from the fuzzer input.

  The returned string may be of any length between `0` and `max-length`, even if there is
  more fuzzer input available.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `max-length` - the maximum length of the string

  Returns a `String` of length between `0` and `max-length` (inclusive)"
  ^String [^FuzzedDataProvider in max-length]
  (.consumeString in max-length))


(defn consume-remaining-as-string
  "Consumes the remaining fuzzer input as a `String`.

  **Note:** After calling this method, further calls to methods of this interface will
  return fixed values only.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Returns a `String`"
  ^String [^FuzzedDataProvider in]
  (.consumeRemainingAsString in))


(defn consume-ascii-string
  "Consumes an ASCII-only `String` from the fuzzer input.

  The returned string may be of any length between `0` and `max-length`, even if there is
  more fuzzer input available.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `max-length` - the maximum length of the string

  Returns a `String` of length between `0` and `max-length` (inclusive) that contains only ASCII characters"
  ^String [^FuzzedDataProvider in max-length]
  (.consumeAsciiString in max-length))


(defn consume-remaining-as-string
  "Consumes the remaining fuzzer input as an ASCII-only `String`.

  **Note:** After calling this method, further calls to methods of this interface will
  return fixed values only.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Returns a `String` that contains only ASCII characters"
  ^String [^FuzzedDataProvider in]
  (.consumeRemainingAsAsciiString in))


(defn remaining-bytes
  "Returns the number of unconsumed bytes in the fuzzer input.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`

  Returns the number of unconsumed bytes in the fuzzer input"
  [^FuzzedDataProvider in]
  (.remainingBytes in))


(defn pick-value
  "Picks an element from `collection` based on the fuzzer input.

  **Note:** The distribution of picks is not perfectly uniform.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `coll` - collection to pick an element from

  Returns an element from `coll` chosen based on the fuzzer input"
  [^FuzzedDataProvider in coll]
  (.pickValue in coll))


(defn pick-values
  "Picks `n` elements from `collection` based on the fuzzer input.

  **Note:** The distribution of picks is not perfectly uniform.

  Params:
    * `in` - instance of `com.code_intelligence.jazzer.api.FuzzedDataProvider`
    * `coll` - collection to pick an element from
    * `n` - the number of elements to pick

  Returns an array of size `n` from `coll` chosen based on the fuzzer input"
  [^FuzzedDataProvider in coll n]
  (.pickValues in coll n))


;; Wrappers for issues

(defn issue
  "Thrown to indicate that a fuzz target has detected a `:low | :medium | :high | :critical` severity security issue
  rather than a normal bug.

  There is only a semantical but no functional difference between throwing exceptions of this type or any other.
  However, automated fuzzing platforms can use the extra information to handle the detected issues appropriately

  Params:
    * `level` - severity of security issue `:low | :medium | :high | :critical`
    * `msg` - message
    * `throwable` - instance of `Throwable`

  Returns one of this instances:
    * `com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow`
    * `com.code_intelligence.jazzer.api.FuzzerSecurityIssueMedium`
    * `com.code_intelligence.jazzer.api.FuzzerSecurityIssueHigh`
    * `com.code_intelligence.jazzer.api.FuzzerSecurityIssueCritical`"
  ([]
   (issue :low))
  ([level]
   (case level
     :low (FuzzerSecurityIssueLow.)
     :medium (FuzzerSecurityIssueMedium.)
     :high (FuzzerSecurityIssueHigh.)
     :critical (FuzzerSecurityIssueCritical.)))
  ([level msg-or-throwable]
   (case level
     :low (FuzzerSecurityIssueLow. msg-or-throwable)
     :medium (FuzzerSecurityIssueMedium. msg-or-throwable)
     :high (FuzzerSecurityIssueHigh. msg-or-throwable)
     :critical (FuzzerSecurityIssueCritical. msg-or-throwable)))
  ([level ^String msg ^Throwable throwable]
   (case level
     :low (FuzzerSecurityIssueLow. msg throwable)
     :medium (FuzzerSecurityIssueMedium. msg throwable)
     :high (FuzzerSecurityIssueHigh. msg throwable)
     :critical (FuzzerSecurityIssueCritical. msg throwable))))
