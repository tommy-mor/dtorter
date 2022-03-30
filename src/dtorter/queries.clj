(ns dtorter.queries
  (:require [xtdb.api :as xt]
            [dtorter.math :as math]))

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

(defn votes-for-tag [db tid]
  (map first (xt/q db
                   '[:find (pull vote [*])
                     :in tid
                     :where
                     [vote :vote/tag tid]
                     [i1 :item/tags tid]
                     [i2 :item/tags tid]
                     [vote :vote/left-item i1]
                     [vote :vote/right-item i2]
                     [vote :vote/magnitude mag]]
                   tid)))

(defn show [a]
  (clojure.pprint/pprint a)
  a)

(defn sorted [db tag attribute]
  (def items (items-for-tag db (:id tag)))
  (def votes (filter #(= (:vote/attribute %) attribute)
                     (votes-for-tag db (:id tag))))
  (reverse (for [[elo item] (math/getranking (vec items) (vec votes))]
             (assoc item :elo elo))))

(sorted dtorter.http/db dtorter.main/tag "default")


(defn attributes [db tag]
  (def votes (votes-for-tag db (:id tag)))
  (distinct (map :vote/attribute votes)))

;; (def db dtorter.http/db)
;; (def tag {:id "1a260ec6-9580-4dec-ab44-256a9c5c43b1"})

;(sorted dtorter.http/db tag)

(comment 

  (xt/sync main/node)

  (def db (do (xt/sync main/node)
              (xt/db main/node)))

  (def tid (ffirst (alltags db)))

  (def items (itemsfortag db tid))

  (count (votesfortag db tid)))
