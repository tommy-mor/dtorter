(ns dtorter.api
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :as resolve]
            [clojure.edn  :as edn]
            [clojure.walk :refer [postwalk]]

            [dtorter.queries :as queries]
            [dtorter.mutations :as mutations]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]
            [xtdb.api :as xt]))

;; TODO might be easier to to have tag-by-id return entire tag with all caluclations at once. would save some duplicate queries we are having...
(defn show [x]
  (def s x)
  
  x)

(defn grab-user [ctx] (-> ctx :request :session :user-id))

(def resolver-map
  {:query/tag-by-id
   (fn [ctx {:keys [id]} value]
     (strip (queries/tag-info ctx id)))
   
   :query/all-tags
   (fn [{:keys [node]} _ value]
     (strip (queries/all-tags (xt/db node))))
   
   :Tag/items
   (fn [{:keys [node]} {} value]
     (or (:items value)
         (strip (queries/items-for-tag (xt/db node) (:id value)))))
   
   :Tag/votes
   (fn [{:keys [node] :as ctx} {:keys [attribute]} value]
     (let [votes (if (and (:items value)
                          (:votes value))
                   (let [id->item (apply hash-map
                                         (flatten
                                          (map (juxt :id identity) (:items value))))]
                     (map #(assoc %
                                  :left-item (id->item (:left-item %))
                                  :right-item (id->item (:right-item %))) (:votes value)))
                   (strip (queries/votes-for-tag (xt/db node) (:id value) attribute)))]
       (filter #(= (:owner %) (grab-user ctx)) votes)))
   
   :Tag/votecount (fn [{:keys [node]} _ value]
                    (if (:votes value)
                      (count (:votes value))
                      (queries/count-votes (xt/db node) (:id value) nil)))
   :Tag/usercount (fn [{:keys [node]} _ value]
                    (if (:votes value)
                      (->> value
                           :votes
                           (map :owner)
                           distinct
                           count)
                      (strip (queries/count-users (xt/db node) (:id value)))))
   :Tag/itemcount (fn [{:keys [node]} _ value]
                    (if (:items value)
                      (count (:items value))
                      (strip (queries/count-items (xt/db node) (:id value)))))
   

   :Vote/tag
   (fn [{:keys [node]} _ value]
     (strip (queries/tag-by-id (xt/db node) (:tag value))))

   :Vote/left-item
   (fn [{:keys [node]} _ value]
     (if (string? (:left-item value))
       (strip (queries/item-by-id (xt/db node) (:left-item value)))
       (:left-item value)))
   
   :Vote/right-item
   (fn [{:keys [node]} _ value]
     (if (string? (:right-item value))
       (strip (queries/item-by-id (xt/db node) (:right-item value)))
       (:right-item value)))
   
   ;; does calculations
   :Tag/sorted
   (fn [{:keys [node]} {:keys [attribute]} value]
     (let [{:keys [items votes voted-ids]} value]
       (if (and items votes)
         (strip (queries/sorted-calc (filter #(voted-ids (:id %)) items)
                                     (filter #(= (:attribute %) attribute) votes)))
         (strip (queries/sorted (xt/db node) value attribute)))))
   
   :Tag/unsorted
   (fn [{:keys [node]} {:keys [attribute]} value]
     (let [{:keys [votes items voted-ids]} value]
       (if (and votes items)
         (queries/unsorted-calc items votes voted-ids)
         (strip (queries/unsorted (xt/db node) value attribute)))))
   
   :Tag/attributes
   (fn [{:keys [node]} {} value]
     (if (:frequencies value)
       (map first (:frequencies value))
       (strip (queries/attributes (xt/db node) value))))
   
   :Tag/attributecounts
   (fn [{:keys [node]} {} value]
     (if (:frequencies value)
       (map second (:frequencies value))
       (strip (queries/attributes (xt/db node) value))))
   
   :Tag/pair
   (fn [{:keys [node]} {} value]
     (strip (queries/pair-for-tag (xt/db node) (:id value))))

   :Item/tags
   (fn [{:keys [node]} {} item]
     (strip (map #(queries/tag-by-id (xt/db node) %) (:tags item))))

   :All/owner
   (fn [{:keys [node]} {} item]
     (if (string? (:owner item))
       (strip (queries/user-by-id (xt/db node) (:owner item)))
       (:owner item)))


   :mutation/vote
   (fn [{:keys [node] :as ctx} {:keys [tagid] :as args} _]
     (do (mutations/vote node args (grab-user ctx))
         (strip (queries/tag-info ctx tagid))))
   
   :mutation/delvote
   (fn [ctx args _]
     (let [tagid (mutations/delvote ctx args)]
       (strip (queries/tag-info ctx tagid))))
   
   :mutation/additem
   (fn [ctx args _]
     (do (mutations/add-item ctx args)
         (strip (queries/tag-info ctx (:tagid args)))))})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)      schema/compile))

(defn q [query-string]
    (def schema (load-schema))
    (lacinia/execute schema query-string nil nil))

(comment (q "{ tag_by_id(id: \"foo\") {id name}}"))


