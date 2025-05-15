(defproject example "0.0.0-SNAPSHOT"
  :dependencies [[metosin/jsonista "0.3.13"]]

  :source-paths ["src/main/clojure"]
  :resource-paths ["src/main/resources"]

  :profiles {:fuzz {:source-paths ["src/fuzz/clojure"]
                    :resource-paths ["src/fuzz/resources"]
                    :target-path "target"
                    :compile-path "target/classes"
                    :aot [example.fuzzers.core-fuzzer]
                    :main fuzzion.main
                    :dependencies [[io.github.just-sultanov/fuzzion "0.0.1-SNAPSHOT"]]}}

  :aliases {"fuzz:clean" ["with-profile" "+fuzz" "clean"]
            "fuzz:compile" ["with-profile" "+fuzz" "compile"]
            "fuzz" ["with-profile" "+fuzz" "run"]})
