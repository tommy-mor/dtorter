(ns dtorter.queries
  (:require [xtdb.api :as xt]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]))

(defn user-by-id [db e]
  (ffirst (xt/q db
                '[:find (pull e [*])
                  :in e
                  :where
                  [e :user/name _]]
                e)))

(defn tag-by-id [db e]
  (ffirst (xt/q db
                '[:find (pull e [*])
                  :in e
                  :where
                  [e :tag/owner _]]
                e)))

(defn item-by-id [db e]
  (ffirst (xt/q db
                '[:find (pull e [*])
                  :in e
                  :where
                  [e :item/owner _]]
                e)))

(defn all-tags [db]
  (map first
       (xt/q db
             '[:find (pull e [*])
               :where
               [e :tag/name nme]
               [e :tag/description desc]])))

(defn items-for-tag [db tid]
  (map first (xt/q db
                   '[:find (pull e [*])
                     :in tid
                     :where
                     [e :item/tags tid]]
                   tid)))

(defn votes-for-tag [db tid attribute]
  (map first (xt/q db
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



(defn count-votes [db tid attribute]
(or (ffirst (xt/q db
                  '[:find (count vote)
                    :in tid
                    :where
                    [vote :vote/tag tid]
                    [vote :vote/attribute attribute]]
                  tid
                  attribute))
      0))

(defn count-users [db tid]
  (or (ffirst (xt/q db
                    '[:find (count-distinct user)
                      :in tid
                      :where
                      [vote :vote/tag tid]
                      [vote :vote/owner user]]
                    tid))
      0))

(defn count-items [db tid]
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

(defn show [a]
  (def s a)
  s
  a)
                                        ; TODO add pair chosing...
(defn tag-info [{:keys [node]} tid]
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
    (merge tag {:owner owner
                :votes (:vote/_tag votes)
                :items (:item/_tags items)
                :voted-ids (get-voted-ids (show (:vote/_tag votes)))})))

(defn pair-for-tag [db tid]
  (def items (items-for-tag db tid))
  (if (> (count items) 2)
    {:left (first items) :right (second items)}
    nil))

(defn sorted-calc [items votes]
  (reverse (for [[elo item] (math/getranking (vec items) (vec votes))]
             (assoc item :elo elo))))

(defn sorted [db tag attribute]
  (let [votes (strip (votes-for-tag db (:id tag) attribute))

        voted-ids (get-voted-ids votes)
        items (filter #(voted-ids (:id %))
                      (strip (items-for-tag db (:id tag))))]
    
    
    (sorted-calc items votes)))

(defn unsorted-calc [items votes voted-ids]
  (def voted-ids)
  (filter #(not (voted-ids (:id %)))
          items))

(defn unsorted [db tag attribute]
  (def items (items-for-tag db (:id tag)))
  (def votes (votes-for-tag db (:id tag) attribute))
  (unsorted-calc items votes (get-voted-ids votes)))

;; TODO make sure this nil works as intended..
;; TODO make this count attributes..
(defn attributes [db tag]
  (def votes (votes-for-tag db (:id tag) nil))
  (distinct (map :vote/attribute votes)))

;; (def db dtorter.http/db)
;; (def tag {:id "1a260ec6-9580-4dec-ab44-256a9c5c43b1"})

;(sorted dtorter.http/db tag)

(comment 

  (def db dtorter.http/db)

  (def tid (:xt/id (nth (all-tags db) 7))) 

  (def items (items-for-tag db tid))
  (first items)

  (count (votes-for-tag db tid "default"))
  (count-votes db tid nil))
