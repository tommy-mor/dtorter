(ns dtorter.queries
  (:require [xtdb.api :as xt]
            [dtorter.math :as math]))

(def tag-queries
  {:get-all (fn [node]
              (map first (xt/q (xt/db node) '[:find (pull tid [*])
                                              :where [tid :tag/name _]])))})

(def item-queries
  {:get-all (fn [node]
              (map first (xt/q (xt/db node) '[:find (pull tid [*])
                                              :where [tid :item/name _]])))})
(def vote-queries
  {:get-all (fn [node]
              (map first (xt/q (xt/db node) '[:find (pull tid [*])
                                              :where [tid :vote/attribute _]])))})

(defn get-voted-ids [votes]
  (frequencies
   (flatten (map (juxt :vote/left-item :vote/right-item) votes))))

(defn biggest-attribute [node {:keys [id]}]
  (def node node)
  (def tagid tagid)
  (->> (xt/q (xt/db node)
             '[:find atr e
               :in tid
               :where
               [e :vote/tag tid]
               [e :vote/attribute atr]] id)
       (map first)
       frequencies
       (sort-by second)
       last
       first))


(def ^:dynamic *testing* false)

(defn sorted-calc [items votes]
  (reverse (for [[elo item] (math/getranking (vec items) (vec votes))]
             (assoc item :elo elo))))

(defn tag-info-calc [query logged-in-user {:keys [attribute user] :as query-params}]
  (let [[tag owner votes items] query]
    (when (and (not *testing*) (some nil? [tag owner votes items]))
      (throw (ex-info "query failed" {:query query})))
    
    
    
    (let [votes (or (:vote/_tag votes) [])
          items (or (:item/_tags items) [])

          freqs (sort-by second (frequencies (map :vote/attribute votes)))
          filteredvotes (filter #(and (= (:vote/attribute %) attribute) 
                                      (or (not user)
                                          (= (:owner %) user)))
                                votes)
          item-vote-counts (get-voted-ids filteredvotes)
          items (map #(assoc % :item/votecount (item-vote-counts (:xt/id %))) items)
          stuff (group-by #(nil? (item-vote-counts (:xt/id %))) items)
          voted-items (or (get stuff false) [])
          unvoted-items (or (get stuff true) [])
          id->item (into {} (map (juxt :xt/id identity) items))
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

      (-> rawinfo
          :voteditems)

      (merge rawinfo {:pair (math/getpair rawinfo)}))))


(defn tag-info [req]
  (def req req)
  (def tagid (-> req :path-params :id))
  (def node (:node req) )
  (let [{:keys [node path-params query-params]} req
        tagid (:id path-params)
        query (first (xt/q (xt/db node) '[:find
                                          (pull tid [*])
                                          (pull owner [*])
                                          (pull tid [{:vote/_tag [*]}])
                                          (pull tid [{:item/_tags [*]}])
                                          :in tid
                                          :where
                                          [tid :owner owner]
                                          [tid :tag/name _]]
                           tagid))
        attribute (or (:attribute query-params)
                      (biggest-attribute node query-params))
        logged-in-user (:logged-in-username req)]

    (tag-info-calc query logged-in-user query-params)))

(defn unsorted-calc [items votes voted-ids]
  (def voted-ids)
  (filter #(not (voted-ids (:id %)))
          items))


(comment 

  (def db dtorter.http/db)

  (def tid (:xt/id (nth (all-tags db) 7))) 

  (def items (items-for-tag db tid))
  (first items)

  (count (votes-for-tag db tid "default"))
  (count-votes db tid nil))
