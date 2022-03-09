(ns dtorter.queries
  (:require [datomic.client.api :as d]
            [clojure.core.matrix :as m]
            [clojure.core.matrix.random :as mr]))

(def ^:dynamic *padding* 0.2)

(def conn dtorter.data/conn)

(def db (d/db conn))

(defn alltags []
  (d/q '[:find ?e ?name ?description
         :where
         [?e :tag/name ?name]
         [?e :tag/description ?description]]
       db))

(def test-tag (first (filter #(= "ASCII smiley" (second %)) (alltags))))
(or test-tag)

(defn itemsfortag [tid]
  (d/q '[:find ?e ?name ?description
         :in $ ?tid
         :where
         [?tid :tag/member ?e]
         [?e :item/name ?name]
         [?e :item/name ?description]]
       db
       tid)
  )

(count (itemsfortag (first test-tag)))

(defn votesfortag [tid]
  (d/q '[:find ?vote ?i1 ?i2 ?mag
         :in $ ?tid
         :where
         [?tid :tag/member ?i1]
         [?tid :tag/member ?i2]
         [?vote :vote/left-item ?i1]
         [?vote :vote/right-item ?i2]
         [?vote :vote/magnitude ?mag]]
       db
       tid)
  )

(count (votesfortag (first test-tag)))
(m/set-current-implementation :vectorz)

(def arr P)
(def ^:dynamic *epsilon* 0.0000001)
(defn stationary [arr]
  (def col (mr/sample-uniform (second (m/shape arr))))
  (def oldcol (m/assign! (m/clone col) 0.0))

  (m/div! col (m/esum col))
  
  (while (not (m/equals col oldcol *epsilon*))
    (m/assign! oldcol col)
    (m/assign! col (m/inner-product arr col))
    (m/pm col))

  col)

(defn getranking [items votes]
  (def nitems (count items))
  (def nvotes (count votes))
  (def A (m/ensure-mutable (m/new-matrix nitems nitems)))
  (first items)

  (def item->idx (into {} (mapv (fn [i n] [(first i) n]) items (range))))
  
  (doseq [[vid leftid rightid mag] votes]
    (def leftscore (- 100 mag))
    (def rightscore mag)

    (def leftidx (item->idx leftid))
    (def rightidx (item->idx rightid))

    ;; right giving energy to left
    (def oldval (m/mget A leftidx rightidx))
    (m/mset! A leftidx rightidx (+ oldval leftscore))

    ;; left giving energy to right
    (def oldval (m/mget A rightidx leftidx))
    (m/mset! A rightidx leftidx (+ oldval rightscore)))

  (def B (m/ensure-mutable (m/new-matrix nitems nitems)))
  (m/emap-indexed! (fn [[row col] _]
                     (if (= row col)
                       0.0
                       (let [denom (+ (m/mget A row col) (m/mget A col row))
                             ratio (if (not (zero? denom)) (/ (m/mget A row col)
                                                              denom)
                                       0.0)]
                         (if (and ratio
                                  (<= ratio 1.0)
                                  (>= ratio 0.0))
                           ratio
                           0.0))))
                   B)

  ;; normalize so each column sums to one

  (def colsums (vec (map m/esum (m/columns B))))
  (def maxcol (m/emax colsums))
  
  (m/emap-indexed! (fn [[row col] val]
                     (if (= row col)
                       (- 1
                          (/ (nth colsums col)
                             maxcol))
                       (/ val maxcol)))
                   B)

  (def P (m/ensure-mutable (m/new-matrix nitems nitems)))
  (m/div! (m/assign! P 1.0) nitems)
  (m/add! (m/mul! P *padding*)
          (m/mul! B (- 1 *padding*)))

  (def stable (stationary P))
  (m/mul! stable (* 10 nitems))

  (into (sorted-map) (for [item items]
                       [(m/slice stable (item->idx (first item)))
                        item])))

(def tid (first test-tag))

(def items (itemsfortag tid))
(def votes (votesfortag tid))
(count votes)

(binding [*epsilon* 0.000001]
  (for [n (range 10)]
    (apply max (keys (getranking items votes)))))

(rank (first test-tag))
(m/shape [1 2 3 4])

;; TODO do math part, then do permissions


