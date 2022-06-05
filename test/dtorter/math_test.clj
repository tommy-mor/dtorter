(ns test.dtorter.math
  (:require [dtorter.math :as math]
            [dtorter.util])
  (:use [clojure.test]))

(defn vote-on-pair [{:keys [left-item right-item]}]
  {:left-item (:id left-item)
   :right-item (:id right-item)
   :magnitude (rand-int 101)})

(def info (-> dtorter.queries/rawinfo dtorter.util/strip))

(-> info
    keys)


(defn finish-voting [ctx]
  "run through the tag until everything is sorted.."
  (loop [ctx ctx]
    (let [vote (vote-on-pair (math/getpair ctx))]
      (def ctx ctx)
      (-> ctx
          keys)
      
      vote)))
(binding [dtorter.queries/*testing* true]
  (dtorter.queries/tag-info-calc [nil nil (:allvotes ctx) (:allitems ctx)] {:attribute "default"}))
(finish-voting (-> dtorter.queries/rawinfo dtorter.util/strip))
(->> (vote-on-pair (math/getpair)))


