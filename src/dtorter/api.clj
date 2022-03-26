(ns dtorter.api
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.edn  :as edn]
            [clojure.walk :refer [postwalk]]

            [dtorter.queries :as queries]))

(defn show [e]
  (clojure.pprint/pprint e)
  e)

(defn strip-namespaces [kw]
  (when (keyword? kw)
    (keyword (name kw)))) 

(defn strip [map]
  (postwalk (some-fn strip-namespaces identity) map))

(def resolver-map
  {:query/tag-by-id
   (fn [{:keys [db]} {:keys [id]} value]
     (strip (queries/tag-by-id db id)))
   
   :query/items-for-tag
   (fn [{:keys [db]} {} value]
     (strip (queries/items-for-tag db (:id value))))
   
   :query/votes-for-tag
   (fn [{:keys [db]} {} value]
     (strip (queries/votes-for-tag db (:id value))))
   
   :query/all-tags
   (fn [{:keys [db]} _ value]
     (strip (queries/all-tags db)))

   :query/parent-tag
   (fn [{:keys [db]} _ value]
     (strip (queries/tag-by-id db (:tag value))))

   :query/left-item
   (fn [{:keys [db]} _ value]
     (strip (queries/item-by-id db (:left-item value))))
   
   :query/right-item
   (fn [{:keys [db]} _ value]
     (strip (queries/item-by-id db (:right-item value))))
   
   ;; does calculations
   :query/sorted
   (fn [{:keys [db]} {:keys [attribute]} value]
     (strip (queries/sorted db value)))
   })

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)      schema/compile))

(defn q [query-string]
  (def schema (load-schema))
  (lacinia/execute schema query-string nil nil))

(comment (q "{ tag_by_id(id: \"foo\") {id name}}"))


