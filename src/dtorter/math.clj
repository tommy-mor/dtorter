(ns dtorter.math
  (:require [clojure.core.matrix :as m]
            [clojure.core.matrix.random :as mr]))

(m/set-current-implementation :vectorz)

(def ^:dynamic *epsilon* 0.0000001)
(def ^:dynamic *padding* 0.2)

(defn stationary [arr]
  (def col (mr/sample-uniform (second (m/shape arr))))
  (def oldcol (m/assign! (m/clone col) 0.0))

  (m/div! col (m/esum col))
  
  (while (not (m/equals col oldcol *epsilon*))
    (m/assign! oldcol col)
    (m/assign! col (m/inner-product arr col)))

  col)

(defn getranking [items votes]
  (def nitems (count items))
  (def nvotes (count votes))
  (def A (m/ensure-mutable (m/new-matrix nitems nitems)))
  (first items)

  (def item->idx (into {} (mapv (fn [i n] [(:xt/id i) n]) items (range))))
  (doseq [{:vote/keys [id left-item right-item magnitude] :as vote} votes]
    (def leftscore (- 100 magnitude))
    (def rightscore magnitude)

    (def leftidx (item->idx left-item))
    (def rightidx (item->idx right-item))
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
                       [(m/slice stable (item->idx (:xt/id item)))
                        item])))




(comment
  (def db dtorter.http/db)
  (def alltags (dtorter.queries/all-tags db))
  (distinct (map keys alltags))
  (def test-tag (first (filter #(= "ASCII smiley" (:tag/name %)) alltags)))
  (def tid (:xt/id test-tag))
  (def items (dtorter.queries/items-for-tag db tid))
  (def votes (dtorter.queries/votes-for-tag db tid))
  (count votes)

  (binding [*epsilon* 0.000001]
    (for [n (range 10)]
      (apply max (keys (getranking items votes))))))
