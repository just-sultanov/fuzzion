(ns fuzzion.cli
  (:require
    [babashka.cli :as cli]
    [clojure.java.io :as io]
    [clojure.string :as str]))


(def metadata
  (->> (io/resource "fuzzion/meta.edn")
       (slurp)
       (read-string)))


(def version
  (:version metadata))


(def spec
  {:spec
   {;; ;; TODO: [2025-04-28, Ilshat Sultanov] Temporarily disable some options
    ;; :config {:ref "<FILE>"
    ;;          :desc "Load configuration from a file (default: ./fuzzion.edn)"
    ;;          :alias :c
    ;;          }
    :classpath {:ref "<CLASSPATH>"
                :desc "Specifies the classpath for fuzzer targets and dependencies (default: java.class.path system property)"
                :alias :cp}
    :timeout {:ref "<DURATION>"
              :desc "Automatically abort fuzzing target if no new branches are discovered during the specified period (default: 5m)"
              :default "5m"
              :alias :t}
    :dry-run {:coerce :boolean
              :desc "Load fuzzers, build and print fuzzing plan without execution (default: false)"}
    :version {:desc "Display version information"
              :alias :v}
    :help {:desc "Show this help message and exit"
           :alias :h}}})


(def help
  (format
    "Version: %s

Available options:
%s" version (cli/format-opts (assoc spec :order (vec (keys (:spec spec)))))))


(defn parse
  [args]
  (cli/parse-args args spec))
