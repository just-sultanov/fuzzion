(ns fuzzion.main
  (:require
    [fuzzion.cli :as cli]))


(defn run
  [opts]
  (cond
    (:help opts) (println cli/help)
    (:version opts) (println cli/version)
    :else (println (pr-str opts))))


(defn -main
  [& args]
  (run (:opts (cli/parse args))))
