(ns tdsl.parse
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.set :refer [index]]))

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

(defn rewrite [thoughts new]
  
  (def thoughts thoughts)
  (def new new)
  (def updated (->> (merge (index thoughts [:position :file])
                           (index new [:position :file]))
                    vals
                    (map first)))
  (doall
   (for [[f thoughts]
         (group-by :file updated)
         :let [thoughts (sort-by :position thoughts)]]
     (spit f (str/join "\n\n" (map #(str (:name %)
                                         "\n"
                                         (:body %))
                                   thoughts))))))
