(ns fuzzion.main
  (:require
    [fuzzion.cli :as cli]
    [fuzzion.runner :as runner]))


(defn -main
  [& args]
  (let [{:keys [opts]} (cli/parse args)]
    (cond
      (:help opts) (println cli/help)
      (:version opts) (println cli/version)
      :else (runner/run opts))))
