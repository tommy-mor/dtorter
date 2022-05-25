(ns tdsl.parse
  (:require [clojure.string :as str]
            [babashka.fs :as fs]))

(def notes (slurp "/home/tommy/programming/tdsl/test/notes.tdsl" ))

(defn parse-block [st]
  (def st st)
  (let [[kw rst] (str/split st #"\s" 2)]
    (hash-map (keyword kw)
              rst)))

(defn parse-blocks [st] (map parse-block (str/split st #"\n:")))
(defn parse-file [file] (with-meta
                          (parse-blocks (slurp (fs/file file)))
                          {:source-file file}))


(defn parse-files []
  (for [f (fs/glob "/home/tommy/programming/tdsl/" "**/*.tdsl")]
    (parse-file f)))

(first (parse-files))


