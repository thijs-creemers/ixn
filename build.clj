(ns build
  (:require [clojure.tools.build.api :as b]))

(def build-folder "target")
(def jar-content (str build-folder "/classes"))
(def app-name "ixn")
(def version "0.1.1")
(def jar-file-name (format "%s/%s-%s.jar" build-folder (name app-name) version))
(def uber-file (format "%s/%s-%s.jar" build-folder (name app-name) version))
;(def copy-srcs ["src" "resources"])
(def basis (b/create-basis {:project "deps.edn"}))
(def class-dir "target/classes")


(defn clean [_]
      (b/delete {:path build-folder})                       ; removing artifacts folder with (b/delete)
      (println (format "Build folder \"%s\" removed" build-folder)))


(defn uber
      "create uber jar for running"
      [_]
      (clean nil)
      (b/copy-dir {:src-dirs ["src" "resources"]
                   :target-dir class-dir})
      (b/compile-clj {:basis basis
                      :src-dirs ["src"]
                      :class-dir class-dir})
      (b/uber {:class-dir class-dir
               :uber-file uber-file
               :basis basis
               :main 'ixn.core})

      (println (format "Uber file created: \"%s\""uber-file)))
