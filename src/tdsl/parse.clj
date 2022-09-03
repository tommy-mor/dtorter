(ns tdsl.parse
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.set :refer [index]]
            [medley.core :refer [map-vals]]
            [lambdaisland.deep-diff2 :as ddiff]))


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

(defn find-todos [things]
  (->> things
       (filter #(-> %
                    :name
                    name
                    (str/includes? "todo")))
       (map (fn [item]
              (map (fn [i t]
                     (dissoc (assoc item
                                    :task t
                                    :task-id i)
                             :body))
                   (range)
                   (str/split (:body item) #"\n\n"))))
       flatten))

(defn kw-pop [kw]
  (when kw
    (def kw kw)
    (if (namespace kw)
      (let [nss (str/split (namespace kw)
                           #"\.")]
        [(first nss) (if (empty? (rest nss))
                       (keyword (name kw))
                       (keyword (str/join "." (rest nss)) (name kw)))])
      [(name kw) nil])))

(assert (= ["test" :swag/thing] (kw-pop :test.swag/thing)))
(assert (= ["swag" :thing] (kw-pop (second (kw-pop :test.swag/thing)))))
(assert (= ["epic" nil] (kw-pop :epic)))




(def collapse-li)
(defn collapse-li [li]
  (def li li)
  (if (= nil (first (distinct (map :name li))))
    li
    (if (empty? li)
      li
      (map-vals
       (fn [vals] (collapse-li
                   (map #(assoc % :name (second (kw-pop (:name %)))) vals)))
       (group-by (comp first kw-pop :name) li)))))

(def construct)
(defn construct [m ns]
  (flatten (for [[k v] m]
             (cond
               (map? v) (construct v (conj ns k))
               (seq? v) (map #(assoc % :name (if (empty? ns)
                                               (keyword k)
                                               (keyword (str/join "." ns) k)))
                             v)
               
               :else :woops)
             )))




(comment (defn p [lom]
           (sort-by (juxt :file :position)
                    lom))

         (ddiff/pretty-print
          (ddiff/diff (p todos)
                      (p (construct
                          (collapse-li (p todos))
                          [])))))



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
