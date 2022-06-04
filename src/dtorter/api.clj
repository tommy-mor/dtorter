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
   (fn [ctx args _]
     (strip (queries/tag-info ctx node args)))
   
   :query/item-by-id
   (fn [ctx {{:keys [itemid]} :info} _]
     (strip (queries/item-by-id ctx node itemid)))
   
   :query/all-tags
   (fn [ctx _ value]
     (strip (queries/all-tags ctx node)))
   
   :Tag/items
   (fn [_ {} value]
     (or (:allitems value)
         (throw (ex-info "not implemented" value))))
   
   :Tag/votes
   (fn [ctx {{:keys [attribute]} :info} {:keys [allitems allvotes id->item] :as value}]
     (def allitems allitems)
     (let [votes (if (and allitems allvotes)
                   (map #(assoc %
                                :left-item (id->item (:left-item %))
                                :right-item (id->item (:right-item %))) allvotes)
                   (throw (ex-info "not implemented" value)))
           user (grab-user ctx)]
       (filter #(and (= (:owner %) user)
                     (= (:attribute %) attribute)) votes)))
   
   :Tag/votecount (fn [_ _ value]
                    (if (:allvotes value)
                      (count (:allvotes value))
                      (throw (ex-info "not implemented" value))))
   :Tag/usercount (fn [_ _ value]
                    (if-not (nil? (:allvotes value))
                      (->> value
                           :allvotes
                           (map :owner)
                           distinct
                           count)
                      (throw (ex-info "not implemented" value))))
   :Tag/users (fn [_ _ value]
                (if (:allvotes value)
                  (->> value
                       :allvotes
                       (map :owner)
                       distinct
                       (xt/pull-many (xt/db node) '[*])
                       strip)
                  (throw (ex-info "can't do this yet" value))))
   :Tag/itemcount (fn [_ _ value]
                    (if (:allitems value)
                      (count (:allitems value))
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
   (fn [ctx _ {:keys [sorted] :as value}]
     (if (and sorted)
       (strip sorted)
       
       (throw (ex-info "this data is wrong" value))))
   
   :Tag/unsorted
   (fn [ctx _ {:keys [unvoteditems] :as value}]
     (if (and unvoteditems)
       unvoteditems
       (throw (ex-info "not implemented" value))))
   
   :Tag/attributes
   (fn [ctx _ value]
     (if (:frequencies value)
       (map first (:frequencies value))
       (throw (ex-info "what" value))))
   
   :Tag/attributecounts
   (fn [ctx _ value]
     (if (:frequencies value)
       (map second (:frequencies value))
       (throw (ex-info "don't know how to calculate this rn" value))))
   
   :Tag/pair
   (fn [ctx _ value]
     (strip (queries/pair-for-tag ctx node (:id value))))

   :Item/tags
   (fn [ctx _ item]
     (strip (map #(queries/tag-by-id ctx node %) (:tags item))))

   :All/owner
   (fn [ctx _ item]
     (if (string? (:owner item))
       (strip (queries/user-by-id ctx node (:owner item)))
       (:owner item)))


   
   
   
   :mutation/vote
   (fn [ctx args _]
     (mutations/vote ctx node args))
   
   :mutation/delvote
   (fn [ctx args _]
     (mutations/delvote ctx node args))
   
   :mutation/additem
   (fn [ctx args _]
     (mutations/add-item ctx node args))})

(defn load-schema [node]
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map node))
      schema/compile))

(defn q [query-string]
    (def schema (load-schema))
    (lacinia/execute schema query-string nil nil))

(comment (q "{ tag_by_id(id: \"foo\") {id name}}"))




