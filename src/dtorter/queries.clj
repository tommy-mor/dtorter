(ns dtorter.queries
  (:require [xtdb.api :as xt]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]))

(defn get-voted-ids [votes]
  (frequencies
   (flatten (map (juxt :left-item :right-item) (strip votes)))))


(defn biggest-attribute [ctx node {:keys [tagid]}]
  (def node node)
  (def tagid tagid)
  (->> (xt/q (xt/db node)
             '[:find atr e
               :in tid
               :where
               [e :vote/tag tid]
               [e :vote/attribute atr]] tagid)
       (map first)
       frequencies
       (sort-by second)
       last
       first))


(def ^:dynamic *testing* false)

(defn sorted-calc [items votes]
  (reverse (for [[elo item] (math/getranking (vec items) (vec votes))]
             (assoc item :elo elo))))

(defn tag-info-calc [ctx query {{:keys [attribute user]} :info :as args}]
  (let [[tag owner votes items] query]

    (when (and (not *testing*) (some nil? [tag owner votes items]))
      (throw (ex-info (str "found a null" (prn-str args)) args)))
    
    
    
    (let [votes (strip (or (:vote/_tag votes) []))
          items (strip (or (:item/_tags items) []))

          freqs (sort-by second (frequencies (map :attribute votes)))
          filteredvotes (filter #(and (= (:attribute %) attribute) 
                                      (or (not user)
                                          (= (:owner %) user)))
                                votes)
          item-vote-counts (get-voted-ids filteredvotes)
          items (map #(assoc % :votecount (item-vote-counts (:id %))) items)
          stuff (group-by #(nil? (item-vote-counts (:id %))) items)
          voted-items (or (get stuff false) [])
          unvoted-items (or (get stuff true) [])
          id->item (into {} (map (juxt :id identity) items))
          sorted (sorted-calc voted-items filteredvotes)]
      (def t items)
      (def rawinfo (merge tag {:owner owner
                               :allvotes votes
                               :allitems items
                               :filteredvotes filteredvotes
                               :voteditems voted-items
                               :unvoteditems unvoted-items
                               :itemvotecounts item-vote-counts
                               :frequencies freqs
                               :id->item id->item
                               :sorted sorted}))

      (merge rawinfo {:pair (math/getpair ctx rawinfo)}))))


(defn tag-info [ctx node {{:keys [tagid]} :info :as args }]

  
  (comment "TODO must have permissions on this query... use xtdb query functions")
  (xt/sync node)
  (let [query (first (xt/q (xt/db node) '[:find
                                          (pull tid [*])
                                          (pull owner [*])
                                          (pull tid [{:vote/_tag [*]}])
                                          (pull tid [{:item/_tags [*]}])
                                          :in tid
                                          :where
                                          [tid :tag/owner owner]]
                           tagid))]

    (tag-info-calc ctx query args)))

(comment 

  (def db dtorter.http/db)

  (def tid (:xt/id (nth (all-tags db) 7))) 

  (def items (items-for-tag db tid))
  (first items)

  (count (votes-for-tag db tid "default"))
  (count-votes db tid nil))
