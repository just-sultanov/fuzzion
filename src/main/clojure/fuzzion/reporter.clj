(ns fuzzion.reporter)

(def ^:dynamic *fuzz-out* *out*)


(defmacro with-fuzz-out
  [& body]
  `(binding [*out* *fuzz-out*]
     ~@body))


(def ^:dynamic *log-fn* println)


(defn log
  [& args]
  (when *log-fn*
    (with-fuzz-out
      (apply *log-fn* args))))


(defmulti report
  (fn [type data]
    type))


(defmethod report
  :default
  [type data]
  (log type (pr-str data)))
