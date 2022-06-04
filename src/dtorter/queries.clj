(ns dtorter.queries
  (:require [xtdb.api :as xt]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]))

(defn user-by-id [ctx node e]
  (ffirst (xt/q (xt/db node)
                '[:find (pull e [*])
                  :in e
                  :where
                  [e :user/name _]]
                e)))

(defn item-by-id [ctx node e]
  (ffirst (xt/q (xt/db node)
                '[:find (pull e [*])
                  :in e
                  :where
                  [e :item/owner _]]
                e)))

(defn tag-by-id [ctx node e]
  (ffirst (xt/q (xt/db node)
                '[:find (pull e [*])
                  :in e
                  :where
                  [e :tag/owner _]]
                e)))

(defn all-tags [ctx node]
  (map first
       (xt/q (xt/db node)
             '[:find (pull e [*])
               :where
               [e :tag/name nme]
               [e :tag/description desc]])))

(defn items-for-tag [ctx node tid]
  (map first (xt/q (xt/db node)
                   '[:find (pull e [*])
                     :in tid
                     :where
                     [e :item/tags tid]]
                   tid)))

(defn votes-for-tag [ctx node tid attribute]
  (map first (xt/q (xt/db node)
                   '[:find (pull vote [*])
                     :in tid
                     :where
                     [vote :vote/tag tid]]
                   tid
                   attribute)))

(comment
  "perfornance testing..."
  
  (def mtg "fdd74412-92e4-460f-ae80-19d6befef509")
  (use 'criterium.core)
  (with-progress-reporting
    (first (quick-bench (doall (for [vote (votes-for-tag dtorter.http/db mtg nil)]
                                 (let [left (item-by-id dtorter.http/db (:vote/left-item vote))
                                       right (item-by-id dtorter.http/db (:vote/right-item vote))]
                                   {:l left :r right}))))))
  "402 ms"
  (with-progress-reporting
    (count (quick-bench (votes-for-tag dtorter.http/db mtg nil))))
  "32.932 ms"
  (with-progress-reporting
    (count (quick-bench (xt/q dtorter.http/db '[:find (pull e [*]) (pull item1 [*]) (pull item2 [*])
                                                :in tid
                                                :where
                                                [e :vote/tag tid]
                                                [e :vote/left-item item1]
                                                [e :vote/right-item item2]]
                              mtg))))
  "90ms"
  (-> (xt/q dtorter.http/db '[:find (distinct owner) (pull e [*]) (pull item1 [*]) (pull item2 [*])
                              :in tid
                              :where
                              [e :vote/tag tid]
                              [e :vote/left-item item1]
                              [e :vote/right-item item2]

                              [tid :tag/owner owner]
                              ]
            mtg)
      count)
  "idk"
  (with-progress-reporting
    (quick-bench (-> (xt/q dtorter.http/db '[:find (pull tid [*]) (pull owner [*])
                                             (pull tid [{:vote/_tag [*]}])
                                             (pull tid [{:item/_tags [*]}])
                                             (pull tid [{:item/_tags [*]}])
                                             :in tid
                                             :where
                                             [tid :tag/owner owner]
                                             ]
                           mtg)
                     first
                     (nth 3))))
  "27 ms, it includes tag, owner, all votes. counts just use count function."
  
  )



(defn count-votes [ctx db tid attribute]
  (or (ffirst (xt/q db
                    '[:find (count vote)
                      :in tid
                      :where
                      [vote :vote/tag tid]
                      [vote :vote/attribute attribute]]
                    tid
                    attribute))
      0))

(defn count-users [ctx db tid]
  (or (ffirst (xt/q db
                    '[:find (count-distinct user)
                      :in tid
                      :where
                      [vote :vote/tag tid]
                      [vote :vote/owner user]]
                    tid))
      0))

(defn count-items [ctx db tid]
  (or (ffirst (xt/q db
                    '[:find (count item)
                      :in tid
                      :where
                      [item :item/tags tid]]
                    tid))
      0))

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


(defn sorted-calc [items votes]
  (reverse (for [[elo item] (math/getranking (vec items) (vec votes))]
             (assoc item :elo elo))))


(defn tag-info [ctx node {{:keys [attribute user tagid]} :info :as args}]
  (comment "TODO must have permissions on this query... use xtdb query functions")
  (xt/sync node)
  (let [[tag owner votes items] (first (xt/q (xt/db node) '[:find
                                                            (pull tid [*])
                                                            (pull owner [*])
                                                            (pull tid [{:vote/_tag [*]}])
                                                            (pull tid [{:item/_tags [*]}])
                                                            :in tid
                                                            :where
                                                            [tid :tag/owner owner]]
                                             tagid))]

    (when (some nil? [tag owner votes items])
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
      (math/getpair rawinfo)
      rawinfo)))

(defn pair-for-tag [ctx node tid]
  (def items (items-for-tag ctx node tid))
  (if (> (count items) 2)
    {:left (first items) :right (second items)}
    nil))

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
