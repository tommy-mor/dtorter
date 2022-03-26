(ns dtorter.queries
  (:require [xtdb.api :as xt]
            [dtorter.main :as main]))

(defn alltags [db]
  (xt/q db
        '[:find e nme desc
          :where
          [e :tag/name nme]
          [e :tag/description desc]]))

(defn itemsfortag [db tid]
  (xt/q db
        '[:find e name description
          :in tid
          :where
          [e :item/tags tid]
          [e :item/name name]
          [e :item/name description]]
        tid))

(defn votesfortag [db tid]
  (xt/q db
        '[:find vote i1 i2 mag
          :in tid
          :where
          [vote :vote/tag tid]
          [i1 :item/tags tid]
          [i2 :item/tags tid]
          [vote :vote/left-item i1]
          [vote :vote/right-item i2]
          [vote :vote/magnitude mag]]
        tid))

(comment 

  (xt/sync main/node)

  (def db (do (xt/sync main/node)
              (xt/db main/node)))

  (def tid (ffirst (alltags db)))

  (def items (itemsfortag db tid))

  (count (votesfortag db tid)))
