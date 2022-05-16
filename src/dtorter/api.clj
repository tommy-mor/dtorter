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
(defn grab-user [ctx] (-> ctx :request :session :user-id))

(defn resolver-map [node]
  (comment "how to get one open-db per lacinia request...")
  {:query/tag-by-id
   (fn [ctx {:keys [id]} value]
     (strip (queries/tag-info ctx node id)))
   
   :query/all-tags
   (fn [ctx _ value]
     (strip (queries/all-tags ctx node)))
   
   :Tag/items
   (fn [_ {} value]
     (or (:items value)
         (throw (ex-info "not implemented" value))))
   
   :Tag/votes
   (fn [ctx {:keys [attribute]} value]
     (let [votes (if (and (:items value)
                          (:allvotes value))
                   (let [id->item (apply hash-map
                                         (flatten
                                          (map (juxt :id identity) (:items value))))]
                     (map #(assoc %
                                  :left-item (id->item (:left-item %))
                                  :right-item (id->item (:right-item %))) (:allvotes value)))
                   (throw (ex-info "not implemented" value)))]
       (filter #(= (:owner %) (grab-user ctx)) votes)))
   
   :Tag/votecount (fn [_ _ value]
                    (def t value)
                    (-> t keys)
                    (if (:allvotes value)
                      (count (:allvotes value))
                      (throw (ex-info "not implemented" value))))
   :Tag/usercount (fn [_ _ value]
                    (if (:votes value)
                      (->> value
                           :votes
                           (map :owner)
                           distinct
                           count)
                      (throw (ex-info "not implemented" value))))
   :Tag/users (fn [_ _ value]
                (if (:votes value)
                  (->> value
                       :votes
                       (map :owner)
                       distinct
                       (xt/pull-many (xt/db node) '[*])
                       strip)
                  (throw (ex-info "can't do this yet" value))))
   :Tag/itemcount (fn [_ _ value]
                    (if (:items value)
                      (count (:items value))
                      (throw (ex-info "not implemented" value))))
   

   :Vote/tag
   (fn [ctx _ value]
     (strip (queries/tag-by-id ctx node (:tag value))))

   :Vote/left-item
   (fn [ctx _ value]
     (if (string? (:left-item value))
       (strip (queries/item-by-id ctx node (:left-item value)))
       (:left-item value)))
   
   :Vote/right-item
   (fn [ctx _ value]
     (if (string? (:right-item value))
       (strip (queries/item-by-id ctx node (:right-item value)))
       (:right-item value)))
   
   ;; does calculations
   :Tag/sorted
   (fn [ctx {:keys [attribute user] :as args} value]
     (let [{:keys [items allvotes voted-ids]} value]
       (if (and items allvotes)
         (strip (queries/sorted-calc (filter #(voted-ids (:id %)) items)
                                     (filter #(and (= (:attribute %) attribute)
                                                   (or (not user)
                                                       (= (:owner %) user)))
                                             allvotes)))
         (throw (ex-info "this data is wrong" value)))))
   
   :Tag/unsorted
   (fn [ctx {:keys [attribute]} value]
     (let [{:keys [votes items voted-ids]} value]
       (if (and votes items)
         (queries/unsorted-calc items votes voted-ids)
         (throw (ex-info "not implemented" value)))))
   
   :Tag/attributes
   (fn [ctx {} value]
     (if (:frequencies value)
       (map first (:frequencies value))
       (throw (ex-info "what" value))))
   
   :Tag/attributecounts
   (fn [ctx {} value]
     (if (:frequencies value)
       (map second (:frequencies value))
       (throw (ex-info "don't know how to calculate this rn" value))))
   
   :Tag/pair
   (fn [ctx {} value]
     (strip (queries/pair-for-tag ctx node (:id value))))

   :Item/tags
   (fn [ctx {} item]
     (strip (map #(queries/tag-by-id ctx node %) (:tags item))))

   :All/owner
   (fn [ctx {} item]
     (if (string? (:owner item))
       (strip (queries/user-by-id ctx node (:owner item)))
       (:owner item)))


   :mutation/vote
   (fn [ctx {:keys [tagid] :as args} _]
     (do (mutations/vote ctx node args)
         (strip (queries/tag-info ctx node tagid))))
   
   :mutation/delvote
   (fn [ctx args _]
     (let [tagid (mutations/delvote ctx node args)]
       (strip (queries/tag-info ctx node tagid))))
   
   :mutation/additem
   (fn [ctx args _]
     (do (mutations/add-item ctx node args)
         (strip (queries/tag-info ctx node (:tagid args)))))})

(defn load-schema [node]
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers node)
      schema/compile))

(defn q [query-string]
    (def schema (load-schema))
    (lacinia/execute schema query-string nil nil))

(comment (q "{ tag_by_id(id: \"foo\") {id name}}"))




