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
  [{:keys [target timeout shutdown-handler exit-handler]
    :or {timeout "5m"}} cmd]
  (let [target-name (::f/target (meta target))
        *interrupt-at (atom nil)
        *interrupted? (atom false)
        timeout (parse-duration timeout)
        process (p/process cmd {:out :pipe
                                :err :out
                                :shutdown shutdown-handler
                                :exit-fn exit-handler
                                :pre-start-fn #(r/log (format "[%s] - Command: %s" target-name (str/join \space (:cmd %))))})]
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
        (let [cmd (:cmd process)]
          (case (ex-message e)
            "Stream closed" {:exit 77, :cmd cmd}
            {:exit 1, :cmd cmd})))
      (finally
        (reset! *interrupted? true)
        (when (p/alive? process)
          (p/destroy process))))))


(defmethod r/report :begin-fuzz-target
  [_type {:keys [target]}]
  (r/log (format "\n\nTarget: %s\n" (::f/target (meta target)))))


(defmethod r/report :end-fuzz-target
  [_type {:keys [exit cmd dry-run target started-at finished-at]}]
  (let [target-name (::f/target (meta target))]
    (when dry-run (r/log (format "[%s] Command: %s" target-name cmd)))
    (r/log (format "[%s] Started at: %s" target-name started-at))
    (r/log (format "[%s] Finished at: %s" target-name finished-at))
    (r/log (format "[%s] Lead time: %s" target-name (time-between started-at finished-at)))
    (r/log (format "[%s] Exit code: %s" target-name exit))))


(defn make-cmd
  [{:as opts :keys [target classpath]
    :or {classpath (System/getProperty "java.class.path")}}]
  (let [{::f/keys [target-class]} (meta target)]
    (format "jazzer --cp=%s --target_class=%s" classpath target-class)))


(defn run-jazzer
  [{:as opts :keys [dry-run targets]}]
  (reduce
    (fn [acc target]
      (let [started-at (LocalDateTime/now)
            opts (-> opts (dissoc :nses :targets) (assoc :target target))
            _ (r/report :begin-fuzz-target opts)
            cmd (make-cmd opts)
            {:keys [exit cmd]} (if dry-run
                                 {:exit 0, :cmd cmd}
                                 (execute opts cmd))
            finished-at (LocalDateTime/now)
            res (assoc opts :exit exit :cmd cmd :started-at started-at :finished-at finished-at)]
        (r/report :end-fuzz-target res)
        (assoc acc target res)))
    {} targets))


(defmethod r/report :begin-run-jazzer
  [_type {:keys [nses targets timeout]}]
  (r/log (format "\nFound %s target(s) in %s namespace(s):" (count targets) (count nses)))
  (r/log (->> targets
              (mapv #(format "  - %s" (-> % meta ::f/target)))
              (str/join \newline)))
  (r/log "\nConfiguration:")
  (r/log (format "  - Timeout %s" timeout)))


(defmethod r/report :end-run-jazzer
  [_type {:keys [nses targets reports started-at finished-at]}]
  (r/log (format "\n\nFuzzing has been completed for %s target(s) in %s namespace(s):" (count targets) (count nses)))
  (r/log (->> targets
              (mapv (fn [target]
                      (let [report (get reports target)
                            target-name (::f/target (meta target))]
                        (format "  - %s (%s)"
                                target-name
                                (time-between (:started-at report)
                                              (:finished-at report))))))
              (str/join \newline)))
  (r/log (format "\nStarted at: %s" started-at))
  (r/log (format "Finished at: %s" finished-at))
  (r/log (format "Total lead time: %s" (time-between started-at finished-at))))


(defn run
  [opts]
  (let [nses (load-namespaces)
        targets (find-targets nses)
        opts (assoc opts :nses nses :targets targets)
        started-at (LocalDateTime/now)
        _ (r/report :begin-run-jazzer (assoc opts :started-at started-at))
        reports (run-jazzer opts)
        finished-at (LocalDateTime/now)
        res (assoc opts :exit 0 :reports reports :started-at started-at :finished-at finished-at)]
    (r/report :end-run-jazzer res)
    res))
