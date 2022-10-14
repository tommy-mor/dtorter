(ns dtorter.pathom
  (:require  [com.wsscode.pathom3.cache :as p.cache]
             [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
             [com.wsscode.pathom3.connect.built-in.plugins :as pbip]
             [com.wsscode.pathom3.connect.foreign :as pcf]
             [com.wsscode.pathom3.connect.indexes :as pci]
             [com.wsscode.pathom3.connect.operation :as pco]
             [com.wsscode.pathom3.connect.operation.transit :as pcot]
             [com.wsscode.pathom3.connect.planner :as pcp]
             [com.wsscode.pathom3.connect.runner :as pcr]
             [com.wsscode.pathom3.error :as p.error]
             [com.wsscode.pathom3.format.eql :as pf.eql]
             [com.wsscode.pathom3.interface.async.eql :as p.a.eql]
             [com.wsscode.pathom3.interface.eql :as p.eql]
             [com.wsscode.pathom3.interface.smart-map :as psm]
             [com.wsscode.pathom3.path :as p.path]
             [com.wsscode.pathom3.plugin :as p.plugin]
             
             [xtdb.api :as xt]))


(frequencies (map (comp :type first) (xt/q (xt/db dtorter.http/node)
                               '{:find [(pull e [*])]
                                 :where [[e :type _]]})))

(ffirst (xt/q (xt/db dtorter.http/node)
       '{:find [(pull e [*])]
         :where [[e :type :tag]]}))

(defn fix [obj]
  (cond
    (map? obj)
    (let [correct-keyname (keyword (name (:type obj)) "id")]
      (-> obj
          (dissoc :xt/id)
          (assoc correct-keyname (:xt/id obj))))
    (seq? obj) (map fix obj)))

(pco/defresolver uid->user
  [{:keys [db]} {:keys [:user/id]}]
  {::pco/output [:user/name]}
  (fix (xt/pull db '[*] id)))

(pco/defresolver username->uid
  [{:keys [db]} {:keys [:user/name]}]
  {::pco/output [:user/id]}
  {:user/id (ffirst (xt/q db
                          '{:find [id]
                            :in [nme]
                            :where [[id :type :user]
                                    [id :user/name nme]]}
                          name))})

(def tag-attributes [:tag/id :tag/name :tag/description :owner])
(pco/defresolver uid->tags
  [{:keys [db]} {:keys [:user/id]}]
  {::pco/output [{:tags tag-attributes}]}
  {:tags (fix (map first (xt/q db
                               '{:find [(pull t [*])]
                                 :in [id]
                                 :where [[t :type :tag]
                                         [t :owner id]]}
                               id)))})

(pco/defresolver tid->tag
  [{:keys [db]} {:keys [:tag/id]}]
  {::pco/output tag-attributes}
  (xt/pull db '[*] id))

(def item-attributes [:item/id :owner :item/name :item/name :item/url])
(pco/defresolver tag->items
  [{:keys [db]} {:keys [:tag/id]}]
  {::pco/output [{:tag/items item-attributes}]}
  {:tag/items (fix (map first (xt/q db
                                    '{:find [(pull item [*])]
                                      :in [tag]
                                      :where [[membership :tag tag]
                                              [membership :item item]
                                              [membership :type :membership]
                                              [item :type :item]]}
                                    id)))})

(def vote-attributes [:owner :vote/left-item :vote/right-item :vote/magnitude :vote/attribute
                      :vote/tag])

(pco/defresolver tag->votes
  [{:keys [db]} {:keys [:tag/id]}]
  {::pco/output [{:tag/votes vote-attributes}]}
  {:tag/votes (fix (map first (xt/q db
                                    '{:find [(pull vote [*])]
                                      :in [tag]
                                      :where [
                                              [vote :type :vote]
                                              [vote :vote/tag tag]]}
                                    id)))})


(def env (pci/register [uid->user
                        username->uid
                        uid->tags
                        tag->items
                        tid->tag
                        tag->votes]))

(comment
  (p.eql/process (assoc env :db (xt/db dtorter.http/node))
                 {:user/name "tommy"}
                 [:user/id])
  (p.eql/process (assoc env :db (xt/db dtorter.http/node))
                 {:user/name "tommy"}
                 [{:tags [:tag/name :tag/description :owner :tag/id]}])
  
  (p.eql/process (assoc env :db (xt/db dtorter.http/node))
                 {:user/id "5b04c3e9-727d-47e0-8680-a91ad87e0756"}
                 [:user/name])
  (def tid (-> (p.eql/process (assoc env :db (xt/db dtorter.http/node))
                              {:user/name "tommy"}
                              [{:tags [:tag/name :tag/description :owner :tag/id]}])
               :tags
               first
               :tag/id))
  
  (map :item/name (:tag/items (p.eql/process (assoc env :db (xt/db dtorter.http/node))
                                             {:tag/id tid}
                                             [:tag/items :tag/name])))
  
  (p.eql/process (assoc env :db (xt/db dtorter.http/node))
                 {:tag/id tid}
                 [:tag/votes]))






