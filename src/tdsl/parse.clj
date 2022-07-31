(ns tdsl.parse
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(defn parse-block [fname st i]
  (def st st)
  (let [[kw rst] (str/split st #"\s" 2)]
    {:name (keyword kw)
     :body (str/trimr rst)
     :position i
     :file fname}))

(defn parse-blocks [st fname]
  (map (partial parse-block fname)
       (filter (complement empty?) (str/split st #"(^|\n):"))
       (range)))

(defn parse-file [file] (parse-blocks (slurp (fs/file file)) (str file)))


(defn parse-files [dir]
  (flatten (for [f (into (fs/glob (str "../" dir "/") "**/*.tdsl") (fs/glob "." "*.tdsl"))]
             (parse-file f))))

(comment
  (def things (parse-files "../programming/tdsl")))

(defn update-files [dir]
  (def dir dir)
  (if (fs/directory? (str "../" dir))
    (do
      (shell/sh "git" "commit" "-am\"clicked button on website\"" :dir (str "../" dir))
      
      (shell/sh "git" "pull" :dir (str "../" dir)))
    #_ (shell/sh "git" "clone" "git@github.com:tommy-mor/tdsl.git" :dir "../")))






(defn rewrite [thoughts]
  (doall
   (for [[f thoughts]
         (group-by :file thoughts)
         :let [thoughts (sort-by :position thoughts)]]
     (spit f (str/join "\n\n" (map #(str (:name %)
                                         "\n"
                                         (:body %))
                                   thoughts))))))
