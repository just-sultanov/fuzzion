(ns fuzzion.main
  (:gen-class)
  (:require
    [fuzzion.cli :as cli]
    [fuzzion.runner :as runner]))


(defn -main
  [& args]
  (let [{:keys [opts]} (cli/parse args)]
    (cond
      (:help opts) (println cli/help)
      (:version opts) (println cli/version)
      :else (let [{:keys [exit]} (runner/run opts)]
              (System/exit exit)))))
