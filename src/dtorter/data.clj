(ns dtorter.data
  (:require [cheshire.core :refer :all]
            [datomic.client.api :as d]))

(def tags (slurp "/home/tommy/programming/dtorter/src/dtorter/data/tags.json"))

(declare decode-v)
(defn decode-kv [[left right]]
  (let [decoded-left (decode-v left)
        decoded-right (decode-v right)]
    (if (nil? decoded-left)
      nil
      {(if (string? decoded-left)
         (keyword decoded-left)
         decoded-left)
       decoded-right})))

(defn decode-v [[left right]]
  (case left
    "table" (let [mapped (into {} (map decode-kv right))]
              (if (every? #(number? (key %)) mapped)
                (vec (map second mapped))
                mapped))
    "number" (Integer/parseInt right)
    "string" right
    "boolean" (Boolean/valueOf right)
    "metatable" nil
    "recursion" nil
    "function" nil
    ))

(defn parse-file [typ]
  (let [slurped (slurp (str "/home/tommy/programming/dtorter/src/dtorter/data/" typ ".json"))]
    (-> slurped
        (parse-string true)
        :lines
        ffirst
        decode-v)))

(def users (parse-file "users"))
(def id->username (into {} (for [user users] [(:id user) user])))

(pprint (second (parse-file "users")))

(def cfg {:server-type :peer-server
          :access-key "myaccesskey"
          :secret "mysecret"
          :endpoint "localhost:8998"
          :validate-hostnames false})

(def client (d/client cfg))

(def conn (d/connect client {:db-name "hello"}))

;; https://gist.github.com/a2ndrade/5651419
 
(def user-schema [{:db/ident :user/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity}
                  
                  {:db/ident :user/password-hash
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  ])

(d/transact conn {:tx-data user-schema})

(def users-tx
  (vec (for [user users]
         {:user/name (:username user)
          :user/password-hash (:password_hash user)})))

(d/transact conn {:tx-data users-tx})

(def db (d/db conn))

(defn hash-by-name [name]
  (ffirst (d/q '[:find ?hash
                 :in $ ?name
                 :where
                 
                 [?e :user/name ?name]
                 [?e :user/password-hash ?hash]]
               db
               name)))

(hash-by-name "tommy")

(def tags (parse-file "tags"))

(def id->tag (into {} (for [tag tags] [(:id tag) tag])))

(def tag-schema [{:db/ident :tag/name
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/fulltext true}
                 {:db/ident :tag/description
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one}
                 {:db/ident :tag/owner
                  :db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/one}

                 {:db/ident :tag/member
                  :db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/many}])

(d/transact conn {:tx-data tag-schema})

(defn user-by-id [id]
  (ffirst (d/q
           '[:find ?e
             :in $ ?username
             :where [?e :user/name ?username]]
           db
           (:username (id->username id)))))

(def tag-data (for [tag tags]
                {:tag/name (:title tag)
                 :tag/description (:description tag)
                 :tag/owner (user-by-id (:user_id tag))}))

(d/transact conn {:tx-data tag-data})

(def db (d/db conn))


(def tag-names (d/q '[:find ?e ?name
                      :where [?e :tag/name ?name]]
                    db))
tag-names

(defn duplicates [col]
  (filter (fn [[k v]] (> v 1)) (frequencies col)))

(duplicates (map :title tags))

(def items-in-tags (parse-file "items_in_tag"))
(first items-in-tags)

(def item->tag (into {} (for [row items-in-tags]
                          [(:item_id row) (:tag_id row)])))

(def items (->>
            (parse-file "items")
            (filter #(item->tag (:id %)))
            (filter #(nil? (:github_id (:content %))))))


(count items)

(def item-schema
  (vec (concat [{:db/ident :item/name
                 :db/valueType :db.type/string
                 :db/cardinality :db.cardinality/one
                 :db/fulltext true}
                
                {:db/ident :item/paragraph
                 :db/valueType :db.type/string
                 :db/cardinality :db.cardinality/one}
                
                {:db/ident :item/url
                 :db/valueType :db.type/string
                 :db/cardinality :db.cardinality/one}

                
                
                ])))

(d/transact conn {:tx-data item-schema})
(count items)

(distinct (map (comp keys :content) items))

(defn item->tagparent [item]
  (let [tagid (item->tag (:id item))
        tags (first (filter #(= (:id %) tagid) tags))]
    (ffirst (d/q '[:find ?e
                   :in $ ?nme
                   :where
                   [?e :tag/name ?nme]]
                 db
                 (:title tags)))))

(item->tagparent (first items))

(def item-tx
  (apply concat
         (for [item items]
           (let [ownerid (item->tagparent item)
                 itemid (:id item)]
             (filter boolean
                     [[:db/add itemid :item/name (:name item)]
                      (when-let [pgraph (get-in item [:content :paragraph])]
                        [:db/add itemid :item/paragraph pgraph])
                      (when-let [url (get-in item [:content :url])]
                        [:db/add itemid :item/url url])
                      [:db/add ownerid :tag/member itemid]])))))

(first item-tx)

(d/transact conn {:tx-data item-tx})

(def db (d/db conn))

(map :title tags)
(distinct (map first (d/q '[:find ?hash
                   :where
                   [?e :item/name ?hash]]
                 db)))

(d/q '[:find ?thing
       :where
       [?tag :tag/name "Fruits"]
       [?tag :tag/member ?item]
       [?item :item/name ?thing]
       ] db)

;; just have url field, let clients decide (even for links)
;; in tags, have "restrictions" field, which restrict/allow types of links
;; test if all images in image tag have urls with image extensions.

;; not sure how to handle url schema
;; option 1:
;;  attributes [link-url, spotify-url, youtube-url, regex-url] in item
;;  +follows niceness of clojure philosophy
;;  +flat
;;  +- can have multiple urls?
;; option 2:
;;  attribute [url] in item, refs a {:link-url :spotify-url :youtube-url} entity
;;  +will make it pretty easy to index by url..
;;  ----makes no sense to have both :spotify-url and  :youtube-url in one url place
;; option 3:
;;  attribute [url] in item, is a (kind, url) pair
;;  +easily extensible to regex kinds
;; option 4:
;;  attribute [link-url, image-url]
;;  only have link and image-link, like reddit.
;;  all other types of special link can be distinguised at time of client
;;  how can this work with schemas?
;;   is this true? images/videos also have distinguishing urls...

;; verdict: delay deciscion until schema system is made

;; how to deal with non-unique titles?
;; just ignore them (only one). maybe will have to think of something better for votes?
;; how to deal with types of content? don't make them part of content map but just attributes.
;; display item based on what data is availible. make api have access to only a 30 character long string for its own purposes.
;; QUESTION: how to deal with image url and normal url. make them different (mutually exclusive?) attributes.
;; mutual exclusion property can be loose (only enforced on frontend input and render, but could be stronger (datomic function))

;; TODO
;; items
;; votes
;; items in tags
;; permissions / schema.

;; user ownership
