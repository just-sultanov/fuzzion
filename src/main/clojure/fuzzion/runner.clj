(ns fuzzion.runner
  (:require
    [babashka.fs :as fs]
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
  [ns-patterns]
  (let [re (->> ns-patterns
                (str/join "|")
                (re-pattern))]
    (reload/init {:output :quiet})
    (mapv
      (fn [sym]
        (require sym)
        (find-ns sym))
      (reload/find-namespaces re))))


(defn fuzz-target?
  [{:keys [skip-meta focus-meta]} target]
  (let [m (meta target)]
    (cond
      (seq focus-meta) (and (::f/target m)
                            (seq (select-keys m focus-meta)))
      (seq skip-meta) (and (::f/target m)
                           (empty? (select-keys m skip-meta)))
      :else (::f/target m))))


(defn find-targets
  [opts nses]
  (reduce
    (fn [acc ns]
      (->> (ns-interns ns)
           (vals)
           (filter (partial fuzz-target? opts))
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


(defn target-name->path
  [target-name]
  (-> target-name
      (str/replace #"/|\." fs/file-separator)
      (str/replace "-" "_")))


(defn build-opts
  [{:keys [config target classpath]
    :or {classpath (System/getProperty "java.class.path")}}]
  (let [m (meta target)
        target-name (::f/target m)
        target-class (::f/target-class m)
        target-path (target-name->path target-name)
        report-dir (fs/path (:report-dir config) target-path)
        corpus-dir (fs/path (:corpus-dir config) target-path)
        dict-dir (fs/path (:dict-dir config) target-path)
        dict-file (fs/file dict-dir "dict")
        coverage-dir (fs/path (:coverage-dir config) target-path)
        reproducer-dir (fs/path (:reproducer-dir config) target-path)
        crash-dir (fs/path (:crash-dir config) target-path)
        user-opts (reduce-kv
                    (fn [acc k v]
                      (if (re-matches (re-pattern k) target-name)
                        (merge acc v)
                        acc))
                    {} (:overrides config))
        default-opts (cond-> {;; libFuzzer opts
                              "-create_missing_dirs" 1
                              "-artifact_prefix" (str crash-dir fs/file-separator)
                              "-dict" dict-file
                              ;; jazzer opts
                              "--reproducer_path" (str reproducer-dir fs/file-separator)
                              "--cp" classpath
                              "--target_class" target-class}
                       (fs/exists? dict-file) (assoc "-dict" dict-file))
        jazzer-opts (->> (merge default-opts user-opts)
                         (reduce-kv
                           (fn [acc opt value]
                             (conj acc (format "%s=%s" opt value)))
                           [])
                         (str/join \space))]
    {:jazzer-cmd (format "jazzer %s %s" jazzer-opts corpus-dir)
     :ensure-dirs [report-dir corpus-dir dict-dir coverage-dir reproducer-dir crash-dir]}))


(defn run-jazzer
  [{:as opts :keys [dry-run targets]}]
  (reduce
    (fn [acc target]
      (let [started-at (LocalDateTime/now)
            opts (-> opts (dissoc :nses :targets) (assoc :target target))
            _ (r/report :begin-fuzz-target opts)
            {:keys [jazzer-cmd ensure-dirs]} (build-opts opts)
            {:keys [exit cmd]} (if dry-run
                                 {:exit 0, :cmd jazzer-cmd}
                                 (do
                                   (run! fs/create-dirs ensure-dirs)
                                   (execute opts jazzer-cmd)))
            finished-at (LocalDateTime/now)
            res (assoc opts :exit exit :cmd cmd :started-at started-at :finished-at finished-at)]
        (r/report :end-fuzz-target res)
        (assoc acc target res)))
    {} targets))


(defmethod r/report :begin-run-jazzer
  [_type {:keys [nses targets timeout config]}]
  (let [{:keys [ns-patterns skip-meta focus-meta]} config]
    (r/log "\nConfiguration:")
    (r/log (format "  - Timeout: %s" timeout))
    (r/log (format "  - NS patterns: %s" ns-patterns))
    (cond
      (seq focus-meta) (r/log (format "  - Focus meta: %s" focus-meta))
      (seq skip-meta) (r/log (format "  - Skip meta: %s" skip-meta)))
    (r/log (format "\nFound %s target(s) in %s namespace(s):" (count targets) (count nses)))
    (r/log (->> targets
                (mapv #(format "  - %s" (-> % meta ::f/target)))
                (str/join \newline)))))


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


(def default-config
  {:report-dir "fuzz/reports"
   :corpus-dir "fuzz/corpus"
   :dict-dir "fuzz/dicts"
   :coverage-dir "fuzz/coverage"
   :reproducer-dir "fuzz/reproducers"
   :crash-dir "fuzz/crashes"
   :ns-patterns [".+-fuzzer$"]
   :skip-meta [:skip]
   :focus-meta []})


(defn parse-config
  [file]
  (if-not (fs/exists? file)
    default-config
    (->> file
         (slurp)
         (read-string)
         (merge default-config))))


(defn run
  [opts]
  (let [opts (update opts :config parse-config)
        {:keys [ns-patterns skip-meta focus-meta]} (:config opts)
        nses (load-namespaces ns-patterns)
        targets (find-targets {:skip-meta skip-meta, :focus-meta focus-meta} nses)
        opts (assoc opts :nses nses :targets targets)
        started-at (LocalDateTime/now)
        _ (r/report :begin-run-jazzer (assoc opts :started-at started-at))
        reports (run-jazzer opts)
        finished-at (LocalDateTime/now)
        res (assoc opts :exit 0 :reports reports :started-at started-at :finished-at finished-at)]
    (r/report :end-run-jazzer res)
    res))
