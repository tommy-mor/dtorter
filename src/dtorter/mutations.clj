(ns dtorter.mutations
  (:require [xtdb.api :as xt]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]
            [dtorter.queries :as queries]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn grab-user [ctx] (-> ctx :request :session :user-id))

(defn vote [ctx node {{:keys [tagid left_item right_item attribute magnitude] :as args} :vote_info}]
  (comment "TODO add checks here, using spec")
  
  (when (some nil? [tagid left_item right_item attribute magnitude])
    (throw (ex-info "missing arguments" args)))
  
  (let [userid (grab-user ctx)]

    (when (not userid) (throw (Exception. "must be logged in to vote")))
    
    ;; upsert TODO add user to both parts of this
    (let [uuid (or (ffirst (xt/q (xt/db node) '{:find [uuid]
                                                :in [id id2 atr tagid]
                                                :where
                                                
                                                [[e :vote/tag tagid]
                                                 [e :xt/id uuid]
                                                 [e :vote/attribute atr]
                                                 [e :vote/owner uid]
                                                 
                                                 (or (and [e :vote/right-item id2]
                                                          [e :vote/left-item id])
                                                     (and [e :vote/right-item id]
                                                          [e :vote/left-item id2]))]}
                                 left_item
                                 right_item
                                 attribute
                                 tagid
                                 userid))
                   (uuid))
          tx (xt/submit-tx node  [[::xt/put {:xt/id uuid
                                             :vote/left-item left_item
                                             :vote/right-item right_item
                                             :vote/magnitude magnitude
                                             :vote/owner userid
                                             :vote/attribute attribute
                                             :vote/tag tagid}]])]
      (xt/await-tx node tx)
      {:id uuid})))

(defn delvote [ctx node {:keys [voteid] :as args}]
  ;; TODO make sure we own this document...
  (let [tagid (ffirst (xt/q (xt/db node) '{:find [tagid]
                                           :in [vid owner]
                                           :where
                                           [[vid :vote/tag tagid]
                                            [vid :vote/owner owner]]}
                            voteid
                            (grab-user ctx)))]
    (if tagid
      (do
        (xt/await-tx node (xt/submit-tx node [[::xt/delete voteid]]))
        {:id voteid})
      "TODO error here better?")))

(defn add-item [ctx node
                {{:keys [name url description tagid] :as args} :item_info}]
  
  (let [userid (grab-user ctx)
        uuid (uuid)
        tx (xt/submit-tx node
                         [[::xt/put {:xt/id uuid
                                     :item/name name
                                     :item/url url
                                     :item/description description
                                     :item/tags [tagid]
                                     :item/owner userid}]])]
    (xt/await-tx node tx)
    {:id uuid}))






