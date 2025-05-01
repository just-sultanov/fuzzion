(ns build
  (:require
    [clojure.string :as str]
    [clojure.tools.build.api :as b]))


(def lib 'io.github.just-sultanov/fuzzion)
(def version (-> (slurp "version") (str/trim)))
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def src-dirs ["src/main/clojure" "src/main/resources"])

(def basis (delay (b/create-basis {:project "deps.edn"})))


(defn clean
  [_]
  (println "Cleaning...")
  (doseq [path ["target" "coverage"]]
    (b/delete {:path path})))


(defn jar
  [_]
  (println "Writing pom...")
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis @basis
                :src-dirs src-dirs})
  (println "Copying sources...")
  (b/copy-dir {:src-dirs src-dirs
               :target-dir class-dir})
  (println "Making jar file...")
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))


(defn install
  [_]
  (println "Installing to `~/.m2`...")
  (b/install {:class-dir class-dir
              :lib lib
              :version version
              :basis @basis
              :jar-file jar-file}))
