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
  (set
   (flatten (map (juxt :left-item :right-item) (strip votes)))))

(defn tag-info [ctx node tid]
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
                                             tid))]
    (let [votes (:vote/_tag votes)]
      (merge tag {:owner owner
                  :allvotes votes
                  :items (:item/_tags items)
                  :voted-ids (get-voted-ids votes)
                  :frequencies (sort-by second (frequencies (map :vote/attribute votes)))}))))

(defn pair-for-tag [ctx node tid]
  (def items (items-for-tag ctx node tid))
  (if (> (count items) 2)
    {:left (first items) :right (second items)}
    nil))

(defn sorted-calc [items votes]
  (reverse (for [[elo item] (math/getranking (vec items) (vec votes))]
             (assoc item :elo elo))))

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
