(ns dtorter.clean-data
  (:require [dtorter.http :refer [reset]]
            [dtorter.data :as d]
            [martian.core :as martian]
            [martian.clj-http :as martian-http]
            [clojure.set :as se]))

;; this data is super clean usable for my daily workflow. will become whole eventually but rn its just me
(reset)
(def m (martian-http/bootstrap-openapi "http://localhost:8080/api/swagger.json"))

(defn resp [a & [b]]
  (let [r (-> (martian/response-for m a b)
              :body)]
    (if (and (map? r) (= (keys r)
                         [:xt/id]))
      (:xt/id r)
      r)))

(def userid->name (into {} (map (juxt :id :username) d/users)))
(def name->userid (into {} (map (juxt :username :id) d/users)))

(def itemid->item (into {} (map (juxt :id identity) d/items)))

(comment (def user->votecount (into {} (for [ [id c] (frequencies (map :user_id d/votes))]
                                 [ (userid->name id) c])))) 

(def title->id (into {} (map (juxt :title :id) d/tags)))
(def tagid->tag (into {} (map (juxt :id identity) d/tags)))

(resp :user/list-all)


(def tommy (resp :user/new
                 {:user/name "tommy"
                  :user/password "tommy1"}))
(def blobbed (resp :user/new
                   {:user/name "blobbed"
                    :user/password "blobbed1"}))

(resp :user/list-all)

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

(def votes (gather-votes "Fruits" ["tommy" "blobbed"]))
(def items (gather-items votes))

(defn olditem->item [old]
  old)

(def fruits (resp :tag/new {:tag/name "fruits"
                            :tag/description "fruits quiz"
                            :owner tommy}))

(def oldid->newid (into {} (for [item items]
                             [(:id item) (resp :item/new {:item/name (:name item)
                                                          :item/tags [fruits]
                                                          :owner tommy})])))
(for [vote votes]
  (resp :vote/new
        {:vote/left-item (-> vote :item_a oldid->newid)
         :vote/right-item (-> vote :item_b oldid->newid)
         :vote/magnitude (-> vote :magnitude)
         :vote/attribute "deliciousness"
         :vote/tag fruits
         :owner tommy}))

(-> (resp :tag/sorted {:id fruits :attribute "deliciousness"})
    :tag.filtered/unvoted-items)
