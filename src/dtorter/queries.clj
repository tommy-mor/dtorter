(ns dtorter.queries
  (:require [datomic.client.api :as d]))

(def conn dtorter.data/conn)

(def db (d/db conn))

(defn alltags []
  (d/q '[:find ?e ?name ?description
         :where
         [?e :tag/name ?name]
         [?e :tag/description ?description]]
       db))

(defn itemsfortag [tid]
  (d/q '[:find ?e ?name ?description
         :in $ ?tid
         :where
         [?tid :tag/member ?e]
         [?e :item/name ?name]
         [?e :item/name ?description]]
       db
       tid))

(defn votesfortag [tid]
  (d/q '[:find ?vote ?i1 ?i2 ?mag
         :in $ ?tid
         :where
         [?tid :tag/member ?i1]
         [?tid :tag/member ?i2]
         [?vote :vote/left-item ?i1]
         [?vote :vote/right-item ?i2]
         [?vote :vote/magnitude ?mag]]
       db
       tid))
