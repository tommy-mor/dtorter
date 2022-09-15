(ns dtorter.clean-data
  (:require [dtorter.data :as d]
            [martian.core :as martian]
            [martian.clj-http :as martian-http]
            [clojure.set :as se]
            [tentacles.repos :as tr]
            [tentacles.issues :as ti]
            [clojure.data :as data]
            [lambdaisland.deep-diff2 :as ddiff]
            [ring.util.response :as ring-resp]
            [xtdb.api :as xt]))

(defn resp-m [m a & [b]]
  (let [r (-> (martian/response-for m a b)
              :body)]
    (if (and (map? r) (= (keys r)
                         [:xt/id]))
      (:xt/id r)
      r)))


(def host (str "http://"
               (or "localhost" "sorter.isnt.online")
               ":8080/api/swagger.json"))

(comment (def m (martian-http/bootstrap-openapi host))
         (def resp (partial resp-m m))
         (def userid->name (into {} (map (juxt :id :username) d/users)))
         (def name->userid (into {} (map (juxt :username :id) d/users)))

         (def itemid->item (into {} (map (juxt :id identity) d/items)))
         (def title->id (into {} (map (juxt :title :id) d/tags)))

         (def user->votecount (into {} (for [ [id c] (frequencies (map :user_id d/votes))]
                                         [ (userid->name id) c])))

         (def title->tag (into {} (map (juxt :title identity) d/tags)))

         (def users (resp :user/list-all))
         (defn lookup-user [name]
           (:xt/id (ffirst (xt/q (xt/db dtorter.http/node) '{:find [(pull u [*])]
                                                             :where [ [u :user/name name]]
                                                             :in [name]}
                                 name))))
         (def tommy
           (or
            (lookup-user "tommy")
            (resp :user/new
                  {:user/name "tommy"
                   :user/password "tommy1"})))
         (def blobbed
           (or (lookup-user "tommy")
               (resp :user/new
                     {:user/name "blobbed"
                      :user/password "blobbed1"})))
         
         (def eli
           (or (lookup-user "eli")
               (resp :user/new
                     {:user/name "eli"
                      :user/password "eli1"})))

         (def olduser->newuser {(name->userid "tommy") tommy
                                (name->userid "blobbed") blobbed})

         (defn gather-votes [tagname users]
           (def tagname tagname)
           (def users users)

           (def votes (->> d/votes
                           (filter (comp (partial = (title->id tagname)) :tag_id))
                           (filter (comp (set (map name->userid users)) :user_id))))
           votes)

         (defn gather-items [votes]
           (->> votes
                (map (juxt :item_a :item_b))
                flatten
                distinct
                (map itemid->item)))

         (defn import-tag [tagname users attribute]
           (def existing-tags (resp :tag/list-all))
           
           (if (some (comp #{tagname} :tag/name) existing-tags)
             "tag already exists, not doing anything"
             (do 
               
               (def votes (gather-votes tagname users))
               (def items (gather-items votes))
               
               (defn olditem->item [old]
                 old)

               (def tag (title->tag tagname))

               (def fruits (resp :tag/new {:tag/name (:title tag)
                                           :tag/description (:description tag)
                                           :owner tommy}))

               (def oldid->newid (into {} (for [item items]
                                            [(:id item) (resp :item/new {:item/name (:name item)
                                                                         :item/tags [fruits]
                                                                         :owner tommy})])))

               (doall (for [vote votes]
                        (resp :vote/new
                              {:vote/left-item (-> vote :item_a oldid->newid)
                               :vote/right-item (-> vote :item_b oldid->newid)
                               :vote/magnitude (min 100 (max 0 (-> vote :magnitude)))
                               :vote/attribute attribute
                               :vote/tag fruits
                               :owner (olduser->newuser (:user_id vote))}))))))


         (import-tag "Fruits" ["tommy" "blobbed"]
                     "deliciousness")

         (import-tag "ways to laugh while texting"
                     ["tommy" "blobbed"]
                     "humor level"))

(defn ghissue->item [tommy tagid issue]
  {:item/name (str "gh#" (:number issue) ": " (:title issue))
   :item/url (:html_url issue)
   :item/tags [tagid]
   :owner tommy})

(defn update-issues [tagid]
  (def m (martian-http/bootstrap-openapi host))
  (def resp (partial resp-m m))

  
  (def tommy (:xt/id (first (filter (comp (partial = "tommy") :user/name) (resp :user/list-all)))))

  (def token {:oauth-token "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"})

  (def issues (ti/issues "tommy-mor" "dtorter" (assoc token
                                                      :all-pages true)))
  ;; this data is super clean usable for my daily workflow. will become whole eventually but rn its just me
  (def issues (into {} (map (juxt :item/url identity))
                    (map (partial ghissue->item tommy tagid) issues)))
  (def items (into {} (map (juxt :item/url identity))
                   (resp :tag/items {:id tagid})))
  (def issuekeys (set (keys issues)))
  (def itemkeys (set (keys items)))

  (def to-change (->> (se/intersection issuekeys itemkeys)
                      (filter #(not= (dissoc (items %) :xt/id :type) (issues %)))))
  (def to-add (se/difference issuekeys itemkeys))
  (def to-delete (se/difference itemkeys issuekeys))

  (->> to-add
       (map issues)
       (map (partial resp :item/new))
       doall)
  
  (->> to-delete
       (map items)
       (map :xt/id)
       (map #(resp :item/delete {:id %}))
       doall)
  
  (->> to-change
       (map (juxt items issues))
       (map (fn [[item issue]]
              (resp :item/put (assoc issue :id (:xt/id item)))))
       doall)
  [to-change, to-add, to-delete])

(defn ghtag []
  
  (def m (martian-http/bootstrap-openapi host))
  
  (def resp (partial resp-m m))
  (def tommy (:xt/id (first (filter (comp (partial = "tommy") :user/name) (resp :user/list-all)))))
  
  (if-let [tag (first (filter (comp #{"gh issues"} :tag/name) (resp :tag/list-all)))]
    (update-issues (:xt/id tag))
    (do
      (def tagid (resp :tag/new {:tag/name "gh issues"
                                 :tag/description "synced"
                                 :owner tommy}))
      (update-issues tagid))))

(defn delete-ghtags []
  (map (comp #(resp :tag/delete {:id %}) :xt/id )
       (filter (comp #{"gh issues"} :tag/name) (resp :tag/list-all))))

(def refresh {:no-doc true
              :get {:handler
                    (fn [req]
                      (def req req)
                      (ghtag)
                      (ring-resp/redirect (str "/t/" (-> req
                                                         :path-params
                                                         :tagid))))}})
(comment (ghtag))



