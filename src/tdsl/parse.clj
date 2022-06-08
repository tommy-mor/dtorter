(ns tdsl.parse
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(defn parse-block [st]
  (let [[kw rst] (str/split st #"\s" 2)]
    (hash-map (keyword kw)
              rst)))

(defn parse-blocks [st] (map parse-block (str/split st #"\n:")))
(defn parse-file [file]
  (def file file)
  (with-meta
    (parse-blocks (str/trim (str/replace-first (slurp (fs/file file)) #"\(ns.*\n" "")))
    {:source-file (-> file
                      fs/file
                      io/as-url)}))


(defn parse-files []
  (for [f (into (fs/glob "../tdsl/" "**/*.tdsl") (fs/glob "." "*.tdsl"))]
    (parse-file f)))

(defn update-files []
  (if (fs/directory? "../tdsl")
    (shell/sh "git" "pull" :dir "../tdsl")
    (shell/sh "git" "clone" "git@github.com:tommy-mor/tdsl.git" :dir "../")))

(-> (into {} (parse-file "/home/tommy/programming/tdsl/test/joke.tdsl"))
    keys
    )
