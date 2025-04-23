(ns fuzzion.main
  (:require
    [fuzzion.cli :as cli]))


(defn -main
  [& args]
  (let [{:keys [opts] :as parsed} (cli/parse args)]
    (cond
      (:help opts) (println cli/help)
      (:version opts) (println cli/version)
      :else (println (pr-str parsed)))))
