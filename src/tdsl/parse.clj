(ns tdsl.parse
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(defn parse-block [st]
  (def st st)
  (let [[kw rst] (str/split st #"\s" 2)]
    (hash-map (keyword kw)
              rst)))

(defn parse-blocks [st] (map parse-block (str/split st #"\n:")))
(defn parse-file [file] (with-meta
                          (parse-blocks (slurp (fs/file file)))
                          {:source-file file}))


(defn parse-files [dir]
  (for [f (into (fs/glob (str "../" dir "/") "**/*.tdsl") (fs/glob "." "*.tdsl"))]
    (parse-file f)))

(defn update-files [dir]
  (def dir dir)
  (if (fs/directory? (str "../" dir))
    (do
      (shell/sh "git" "commit" "-am\"clicked button on website\"" :dir (str "../" dir))
      
      (shell/sh "git" "pull" :dir (str "../" dir)))
    #_ (shell/sh "git" "clone" "git@github.com:tommy-mor/tdsl.git" :dir "../")))
