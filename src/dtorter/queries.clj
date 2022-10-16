(ns dtorter.queries
  (:require [xtdb.api :as xt]
            [dtorter.math :as math]
            [clojure.spec.alpha :as s]
            [shared.specs :as sp]
            [expound.alpha :as expound]))

(defn get-voted-ids [votes]
  (frequencies
   (flatten (map (juxt :vote/left-item :vote/right-item) votes))))

(defn biggest-attribute [node tagid]
  (def node node)
  (def tagid tagid)
  (->> (xt/q (xt/db node)
             '[:find atr e
               :in tid
               :where
               [e :vote/tag tid]
               [e :vote/attribute atr]]
             tagid)
       (map first)
       frequencies
       (sort-by second)
       last
       first))

(defn get-vt-last-updated [db eid]
  "from https://github.com/xtdb/xtdb/issues/267 "
  (with-open [h (xt/open-entity-history db eid :desc)]
    (-> (iterator-seq h)
        first
        ::xt/valid-time)))

(def ^:dynamic *testing* false)

(defn sorted-calc [items votes]
  (reverse (for [[elo item] (math/getranking (vec items) (vec votes))]
             (assoc item :elo elo))))

(defn tag-info-calc
  [db query logged-in-user {:keys [attribute user] :as query-params} itemid]
  (let [[tag owner votes items] query]
    (def items items)
    (when (and (not *testing*) (some nil? [tag owner votes]))
      (throw (ex-info "query failed" {:query query})))
    
    (let [votes (or (:vote/_tag votes) [])
          items (or (map first items) [])
          itemid->items (into {} (map (juxt :xt/id identity) items))

          freqs (frequencies (map :vote/attribute votes))
          filteredvotes (filter #(and (itemid->items (:vote/left-item %))
                                      (itemid->items (:vote/right-item %))
                                      (= (:vote/attribute %) attribute) 
                                      (or (not user)
                                          (= (:owner %) user)))
                                votes)
          item-vote-counts (get-voted-ids filteredvotes)
          items (map #(assoc % :item/votecount (item-vote-counts (:xt/id %))) items)
          
          {voted-items false unvoted-items true :or {voted-items [] unvoted-items []}}
          (group-by #(nil? (item-vote-counts (:xt/id %))) items)
          
          id->item (into {} (map (juxt :xt/id identity) items))
          sorted (sorted-calc voted-items filteredvotes)
          userids (distinct (map :owner votes))]
      (def rawinfo (merge tag {:interface/owner (dissoc owner :user/password-hash)
                               :tag/votes votes
                               :tag/items items
                               :tag/votecount (count votes)
                               :tag/itemcount (count items)
                               :tag/usercount (count (distinct (map :owner votes)))
                               :tag.filtered/votes filteredvotes
                               :tag.filtered/items voted-items
                               :tag.filtered/unvoted-items unvoted-items
                               :tag/item-vote-counts item-vote-counts
                               :tag.filtered/sorted sorted
                               :interface.filter/attribute attribute
                               :interface.filter/user (or user :interface.filter/all-users)
                               :interface/attributes freqs
                               :interface/users (xt/pull-many db [:user/name :xt/id] userids)
                               :tag.session/votes (->> filteredvotes
                                                       (filter #(and (= (:owner %) logged-in-user)
                                                                     (or (not itemid)
                                                                         (or (= itemid (:vote/left-item %))
                                                                             (= itemid (:vote/right-item %))))))
                                                       (map #(-> %
                                                                 (update :vote/left-item id->item)
                                                                 (update :vote/right-item id->item))))}))
      (when (and itemid (not (id->item itemid)))
        (throw (ex-info "item not in tag" {:status 400})))
      
      
      (def rawinfo (if itemid
                     (merge rawinfo {:item (id->item itemid)})
                     (merge rawinfo {:pair (math/getpair (assoc rawinfo
                                                                :id->item id->item))})))
      (when (not (s/valid? ::sp/db rawinfo))
        (expound/expound ::sp/db rawinfo)
        (throw (ex-info "generated bad db"
                        {:status 500})))
      rawinfo)))



(defn tag-info [req]
  (def req req)
  (def tagid (-> req :path-params :id))
  (def db (-> req :node xt/db))
  (let [{:keys [node path-params query-params]} req
        params (merge path-params query-params)
        tagid (:id params)
        itemid (or (:itemid params) false)
        ;; todo move {:vote/_tag [*]} into first pull expression.. 
        db (xt/db node)
        query (first (xt/q db '[:find
                                (pull tid [*])
                                (pull owner [*])
                                (pull tid [{:vote/_tag [*]}])
                                items
                                :in tid
                                :where
                                [tid :owner owner]
                                [tid :type :tag]
                                [(q {:find [(pull item [*])]
                                     :in [tid]
                                     :where [                                           
                                             [memb :type :membership]
                                             [memb :tag tid]
                                             [memb :item item]]}
                                    tid)
                                 items]]
                           tagid))
        params (assoc params
                      :attribute (or (:attribute params)
                                     (biggest-attribute node tagid)
                                     :interface.filter/no-attribute))
        logged-in-user (-> req :session :user-id)]
    (tag-info-calc db query logged-in-user params itemid)))

(defn unsorted-calc [items votes voted-ids]
  (filter #(not (voted-ids (:id %)))
          items)) 


(comment 

  (def db dtorter.http/db)

  (def tid (:xt/id (nth (all-tags db) 7))) 

  (def items (items-for-tag db tid))
  (first items)

  (count (votes-for-tag db tid "default"))
  (count-votes db tid nil))
