(ns dtorter.math
  (:require [clojure.core.matrix :as m]
            [clojure.core.matrix.random :as mr]
            [kixi.stats.core :refer [standard-deviation correlation]]
            [kixi.stats.distribution :refer [draw sample binomial]]
            [clojure.data.priority-map :as pm]
            [dtorter.util :as util]))

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

;; note, must be keywordless. this will confuse ppl
(defn getranking [items votes]
  (def nitems (count items))
  (def nvotes (count votes))
  
  (if (or (< nitems 1)
          (< nvotes 1))
    (sorted-map)
    (do
      
      (def A (m/ensure-mutable (m/new-matrix nitems nitems)))
      (def item->idx (into {} (mapv (fn [i n] [(:id i) n]) items (range))))
      
      (doseq [{:keys [id left-item right-item magnitude] :as vote} votes]
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
                           [(m/slice stable (item->idx (:id item)))
                            item])))))

;; TODO fix, this is also in queries..
(defn sorted-calc [items votes]
  (reverse (for [[elo item] (getranking (vec items) (vec votes))]
             (assoc item :elo elo))))


(defn draw-coll [coll dist]
  (when (not-empty coll)
    (nth coll (int (* (count coll) (draw dist))))))

(defn sample-coll [coll n dist]
  (when (not-empty coll)
    (->> (sample (* n 2) dist)
         (map #(int (* (count coll) %)))
         set
         (take n)
         (select-keys coll)
         vals)))

(defn sigmoid [y]
  "https://stackoverflow.com/questions/10097891/inverse-logistic-function-reverse-sigmoid-function
   takes ratio into [0,1] "
  (/ 1 (inc (Math/exp (- y)))))

(defn getpair [ctx {:keys [voteditems filteredvotes itemvotecounts unvoteditems id->item
                           sorted] :as args}]
  (def ctx ctx)
  (def voteditems voteditems)
  (def filteredvotes filteredvotes)
  (def itemvotecounts itemvotecounts)
  (def unvoteditems unvoteditems)
  (def sorted sorted)
  (def id->item id->item)
  (def itemid->elo (into {} (map (juxt :id :elo) sorted)))
  
  ;; item->{votes}
  (def itemid->votes (apply merge-with into
                            (for [{left :left-item right :right-item :as vote}  filteredvotes]
                              {left #{vote} right #{vote}})))

  (defn vote-ratio [itemid votes]
    (apply /
           (reduce (fn [[numerator denominator] {:keys [left-item right-item magnitude]}]
                     (let [edutingam (- 100 magnitude)]
                       (condp = itemid
                         left-item [(+ numerator edutingam)
                                    (+ denominator magnitude)]
                         right-item [(+ numerator magnitude)
                                     (+ denominator edutingam)]))) [0 0] votes)))
  ;; use include in ratio the amount of votes, prioritize ones with less.
  ;; PROBLEM, the ones near top will have winning/unbalanced ratios...
  ;; maybe this strategy only works well at the beginning.
  (defn sample-size [denominator]
    (max (int (/ (count voteditems) denominator))
         1))

  (def sorted-ratios
    (into (pm/priority-map) (for [{itemid :id} (-> sorted vec (sample-coll (sample-size 2) (kixi.stats.distribution/beta {:a 2 :b 3.3})))]
                              {itemid (let [x (vote-ratio itemid (itemid->votes itemid))]
                                        (if (< x 1.0)
                                          (Math/pow x -1.0)
                                          (float x)))})))
  
  ;; todo make way to switch between these ors
  (def leftitem (or
                 ;; TODO make this remember the score, to give rightitem a relative direction?
                 (when (empty? voteditems)
                   (rand-nth unvoteditems))
                 (-> (rand-nth (take (sample-size 3) (reverse sorted-ratios)))
                     first
                     id->item)
                 (comment
                   (id->item (some (fn [[itemid votecount]]
                                     (when (< votecount 3) itemid))
                                   itemvotecounts))
                   (draw-coll voteditems (kixi.stats.distribution/beta {:a 2 :b 3.3})))))
  
  (def voted-pairs (->> filteredvotes
                        (filter (fn [{:keys [left-item right-item]}]
                                  (or (= left-item (:id leftitem))
                                      (= right-item (:id leftitem)))))
                        (map (fn [{:keys [left-item right-item]}]
                               [left-item right-item]))
                        flatten
                        set))
  
  (def rightitem (or (draw-coll unvoteditems (kixi.stats.distribution/uniform 0 1))
                     (->> (into (pm/priority-map)
                                (for [item (filter #(voted-pairs (:id %)) sorted)]
                                  {(:id item) (Math/abs (- (:elo item)
                                                           (itemid->elo (:id leftitem))))}))
                          (drop 1)
                          (take (sample-size 12))
                          rand-nth
                          first
                          id->item)))

  ;; TODO make this do loop
  (assert (not (= leftitem rightitem)))
  ;; TODO match items who want losses with items who want wins

  (if (contains? voted-pairs #{(:id leftitem) (:id rightitem)})
    (do (println "we already found pair \n"
                 leftitem
                 "\n"
                 rightitem
                 "\n trying again..")
        (getpair args ctx)))

  #_(simple pair chosing for beginning. if an item has no losses, elomtach
          as soon as it gets a loss, move on.
          )
  

  ;; if less than 2 exists, find those. then do unequal ratio strategy
  ;; 
  {:left leftitem
   :right rightitem}

  ;; TODO even more heavy preference for items with no losing matchups...

  ;; TODO have to see how this works with baby tags, maybe 3 is too high for baby tags
  ;; QUESTION: left item prioritize >3 votes over distrobution

  ;; righ item: gather empty, otherwise focus on low item scores
  
  
  


  ;; httpus://www.desmos.com/calculator/pct1rbpkgv
  )

; random idea: clojure defn macro that makes all args global ...
;; defnFREEZE
(comment
  (def info dtorter.views.tag/info)
  ;; (getpair (-> dtorter.queries/rawinfo dtorter.util/strip))
  
  (binding [*epsilon* 0.000001]
    (for [n (range 10)]
      (apply max (keys (getranking items votes))))))
