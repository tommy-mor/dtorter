(ns dtorter.api
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :as resolve]
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


(def stest (atom nil))
;; TODO might be easier to to have tag-by-id return entire tag with all caluclations at once. would save some duplicate queries we are having...
(def resolver-map
  {:query/tag-by-id
   (fn [{:keys [db]} {:keys [id]} value]
     (strip (queries/tag-by-id db id)))
   
   :query/all-tags
   (fn [{:keys [db]} _ value]
     (strip (queries/all-tags db)))
   
   :Tag/items
   (fn [{:keys [db]} {} value]
     (strip (queries/items-for-tag db (:id value))))
   
   :Tag/votes
   (fn [{:keys [db]} {:keys [attribute]} value]
     (strip (queries/votes-for-tag db (:id value) attribute)))
   
   :Tag/votecount (fn [{:keys [db]} _ value] (strip (queries/count-votes db (:id value) nil)))
   :Tag/usercount (fn [{:keys [db]} _ value] (strip (queries/count-users db (:id value))))
   :Tag/itemcount (fn [{:keys [db]} _ value] (strip (queries/count-items db (:id value))))
   

   :Vote/tag
   (fn [{:keys [db]} _ value]
     (strip (queries/tag-by-id db (:tag value))))

   :Vote/left-item
   (fn [{:keys [db]} _ value]
     (strip (queries/item-by-id db (:left-item value))))
   
   :Vote/right-item
   (fn [{:keys [db]} _ value]
     (strip (queries/item-by-id db (:right-item value))))
   
   ;; does calculations
   :Tag/sorted
   (fn [{:keys [db]} {:keys [attribute]} value]
     (strip (queries/sorted db value attribute)))
   
   :Tag/unsorted
   (fn [{:keys [db]} {:keys [attribute]} value]
     (strip (queries/unsorted db value attribute)))
   
   :Tag/attributes
   (fn [{:keys [db]} {} value]
     (strip (queries/attributes db value)))
   
   :Tag/pair
   (fn [{:keys [db]} {} value]
     (strip (queries/pair-for-tag db (:id value))))

   :Item/tags
   (fn [{:keys [db]} {} item]
     (strip (map #(queries/tag-by-id db %) (:tags item))))

   :All/owner
   (fn [{:keys [db]} {} item]
     (strip (queries/user-by-id db (:owner item))))})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)      schema/compile))

(defn q [query-string]
  (def schema (load-schema))
  (lacinia/execute schema query-string nil nil))

(comment (q "{ tag_by_id(id: \"foo\") {id name}}"))


