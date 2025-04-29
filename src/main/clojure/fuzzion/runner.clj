(ns fuzzion.runner
  (:require
    [babashka.process :as p]
    [clj-reload.core :as reload]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [fuzzion.core :as f]
    [fuzzion.reporter :as r])
  (:import
    (java.time
      Duration
      LocalDateTime)
    (java.time.temporal
      Temporal)))


(defn load-namespaces
  []
  (reload/init {:output :quiet})
  (mapv
    (fn [sym]
      (require sym)
      (find-ns sym))
    (reload/find-namespaces #".*-fuzzer$")))


(defn find-targets
  [nses]
  (reduce
    (fn [acc ns]
      (->> (ns-interns ns)
           (vals)
           (filter (comp ::f/target meta))
           (into acc)))
    [] nses))


(defn parse-duration
  [s]
  (Duration/parse (format "PT%s" (str/upper-case s))))


(defn time-between
  [^Temporal start ^Temporal end]
  (let [duration (Duration/between start end)]
    (if (or (.isZero duration)
            (.isNegative duration))
      "00:00:00.000"
      (format
        "%02d:%02d:%02d.%03d"
        (abs (.toHours duration))
        (abs (.toMinutesPart duration))
        (abs (.toSecondsPart duration))
        (abs (.toMillisPart duration))))))


(defn new?
  [s]
  (boolean (re-find #"#\d+\s+(NEW)\s+" s)))


(defn inited?
  [s]
  (boolean (re-find #"#\d+\s+(INITED)\s+" s)))


(defn execute
  [{:keys [target-name timeout shutdown-handler exit-handler]
    :or {timeout "5m"}} cmd]
  (let [*interrupt-at (atom nil)
        *interrupted? (atom false)
        timeout (parse-duration timeout)
        process (p/process cmd {:out :pipe
                                :err :out
                                :shutdown shutdown-handler
                                :exit-fn exit-handler
                                :pre-start-fn #(r/log (format "Running: %s" (str/join \space (:cmd %))))})]
    ;; jazzer output handler
    (future
      (try
        (with-open [reader (io/reader (:out process))]
          (loop []
            (when-let [line (.readLine reader)]
              (let [now (LocalDateTime/now)]
                (cond
                  ;; jazzer has been inited or a new branch has been found
                  (or (inited? line) (new? line))
                  (let [interrupt-at (.plus now timeout)]
                    (reset! *interrupt-at interrupt-at)
                    (r/log (format "[%s] %s - %s" target-name (time-between now interrupt-at) line)))

                  ;; timer hasn't been initialized
                  (nil? @*interrupt-at) (r/log (format "[%s] - %s" target-name line))

                  ;; otherwise
                  :else (r/log (format "[%s] %s - %s" target-name (time-between now @*interrupt-at) line))))

              (when-not @*interrupted?
                (recur)))))
        (catch Exception e
          (let [now (LocalDateTime/now)]
            (r/log (format "[%s] %s - ERROR - %s" target-name (time-between now @*interrupt-at) (ex-message e)))))))

    ;; timeout handler
    (future
      (loop []
        (let [now (LocalDateTime/now)
              interrupt-at @*interrupt-at]
          (cond
            ;; timer hasn't been initialized
            (nil? interrupt-at)
            (do
              (Thread/sleep 1000)
              (recur))

            ;; handle timeout
            (.isAfter now interrupt-at)
            (do
              (r/log (format "[%s] %s - TIMEOUT" target-name (time-between now interrupt-at)))
              (reset! *interrupted? true)
              (p/destroy process))

            ;; otherwise
            :else (when-not @*interrupted?
                    (let [interrupt-at (.minusSeconds interrupt-at 1)]
                      (r/log (format "[%s] %s" target-name (time-between now interrupt-at)))
                      (reset! *interrupt-at interrupt-at)
                      (Thread/sleep 1000)
                      (recur)))))))

    (try
      (p/check process)
      (catch Exception e
        (case (ex-message e)
          "Stream closed" {:exit 77}
          {:exit 1}))
      (finally
        (reset! *interrupted? true)
        (when (p/alive? process)
          (p/destroy process))))))


(def dashes
  (str/join (repeat 40 "-")))


(defn run-jazzer
  [{:keys [timeout classpath targets]}]
  (doseq [target targets]
    (let [{:keys [ns] ::f/keys [target-name target-class]} (meta target)
          target-name (format "%s/%s" ns target-name)
          _ (r/log dashes)
          _ (r/log (format "Target: %s" target-name))
          _ (r/log (format "Timeout: %s" timeout))
          cmd (format "jazzer --cp=%s --target_class=%s" classpath target-class)
          started-at (LocalDateTime/now)
          res (execute {:timeout timeout, :target-name target-name} cmd)
          finished-at (LocalDateTime/now)]
      (r/log (format "[%s] Started at: %s" target-name started-at))
      (r/log (format "[%s] Finished at: %s" target-name finished-at))
      (r/log (format "[%s] Lead time: %s" target-name (time-between started-at finished-at)))
      (r/log dashes)
      res)))


(defn run
  [{:keys [timeout]}]
  (let [nses (load-namespaces)
        targets (find-targets nses)
        started-at (LocalDateTime/now)
        {:keys [exit]} (run-jazzer {:classpath (System/getProperty "java.class.path"), :targets targets, :timeout timeout})
        finished-at (LocalDateTime/now)]
    (r/log dashes)
    (r/log (format "Started at: %s" started-at))
    (r/log (format "Finished at: %s" finished-at))
    (r/log (format "Total lead time: %s" (time-between started-at finished-at)))
    (r/log dashes)
    (System/exit (or exit 0))))
