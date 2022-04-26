(ns dtorter.api
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :as resolve]
            [clojure.edn  :as edn]
            [clojure.walk :refer [postwalk]]

            [dtorter.queries :as queries]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]))

;; TODO might be easier to to have tag-by-id return entire tag with all caluclations at once. would save some duplicate queries we are having...
(def resolver-map
  {:query/tag-by-id
   (fn [{:keys [db]} {:keys [id]} value]
     (strip (queries/tag-info db id)))
   
   :query/all-tags
   (fn [{:keys [db]} _ value]
     (strip (queries/all-tags db)))
   
   :Tag/self
   (fn [{:keys [db]} _ {:keys [id]}]
     {:uhh id :votecount 10 :name "tommy"})
   
   :Tag/items
   (fn [{:keys [db]} {} value]
     (strip (queries/items-for-tag db (:id value))))
   
   :Tag/votes
   (fn [{:keys [db]} {:keys [attribute]} value]
     (if (and (:items value)
              (:votes value))
       (let [id->item (apply hash-map (flatten (map (juxt :id identity) (:items value))))]
         (do
           (println "i am being run!!")
           (map #(assoc %
                        :left-item (id->item (:left-item %))
                        :right-item (id->item (:right-item %))) (:votes value))))
       (strip (queries/votes-for-tag db (:id value) attribute))))
   
   :Tag/votecount (fn [{:keys [db]} _ value]
                    (if (:votes value)
                      (count (:votes value))
                      (queries/count-votes db (:id value) nil)))
   :Tag/usercount (fn [{:keys [db]} _ value]
                    (if (:votes value)
                      (->> value
                           :votes
                           (map :owner)
                           distinct
                           count)
                      (strip (queries/count-users db (:id value)))))
   :Tag/itemcount (fn [{:keys [db]} _ value]
                    (if (:items value)
                      (count (:items value))
                      (strip (queries/count-items db (:id value)))))
   

   :Vote/tag
   (fn [{:keys [db]} _ value]
     (strip (queries/tag-by-id db (:tag value))))

   :Vote/left-item
   (fn [{:keys [db]} _ value]
     (if (string? (:left-item value))
       (strip (queries/item-by-id db (:left-item value)))
       (:left-item value)))
   
   :Vote/right-item
   (fn [{:keys [db]} _ value]
     (if (string? (:left-item value))
       (strip (queries/item-by-id db (:right-item value)))
       (:left-item value)))
   
   ;; does calculations
   :Tag/sorted
   (fn [{:keys [db]} {:keys [attribute]} value]
     (let [{:keys [items votes]} value]
       (if (and items votes)
         (strip (queries/sorted-calc items votes))
         (strip (queries/sorted db value attribute)))))
   
   :Tag/unsorted
   (fn [{:keys [db]} {:keys [attribute]} value]
     (let [{:keys [votes items]} value]
       (if (and votes items)
         (queries/unsorted-calc items votes)
         (strip (queries/unsorted db value attribute)))))
   
   :Tag/attributes
   (fn [{:keys [db]} {} value]
     (if (:votes value)
       (distinct (map :attribute (:votes value)))
       (strip (queries/attributes db value))))
   
   :Tag/pair
   (fn [{:keys [db]} {} value]
     (strip (queries/pair-for-tag db (:id value))))

   :Item/tags
   (fn [{:keys [db]} {} item]
     (strip (map #(queries/tag-by-id db %) (:tags item))))

   :All/owner
   (fn [{:keys [db]} {} item]
     (if (string? (:owner item))
       (strip (queries/user-by-id db (:owner item)))
       (:owner item)))


   :mutation/vote
   (fn [{:keys [db node] :as ctx} {:keys [tagid left_item right_item attribute magnitude] :as args} _]
     (queries/vote node args)
     
     
     (strip (queries/tag-by-id db tagid)))})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)      schema/compile))

(defn q [query-string]
    (def schema (load-schema))
    (lacinia/execute schema query-string nil nil))

(comment (q "{ tag_by_id(id: \"foo\") {id name}}"))


