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

;; https://gist.github.com/a2ndrade/5651419
 
(def user-schema [{:db/ident :user/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity}
                  
                  {:db/ident :user/password-hash
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}

                  {:db/ident :user/owns
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many}
                  ])

(def users-tx
  (vec (for [user users]
         {:db/id (:id user)
          :user/name (:username user)
          :user/password-hash (:password_hash user)})))

(def tags (->>
           (parse-file "tags")
           (filter #(not (= "porter test" (:title %))))))

(def id->tag (into {} (for [tag tags] [(:id tag) tag])))

(def tag-schema [{:db/ident :tag/name
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/fulltext true}
                 {:db/ident :tag/description
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one}
                 
                 {:db/ident :tag/member
                  :db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/many}])

(def tags-tx (concat
              (for [tag tags]
                {:db/id (:id tag)
                 :tag/name (:title tag)
                 :tag/description (:description tag)})
              (for [tag tags]
                {:db/id (:user_id tag)
                 :user/owns (:id tag)})))

(defn duplicates [col]
  (filter (fn [[k v]] (> v 1)) (frequencies col)))

(duplicates (map :title tags))

(def items-in-tags (parse-file "items_in_tag"))
(def item->tag (into {} (for [row items-in-tags]
                          [(:item_id row) (:tag_id row)])))

(def items (->>
            (parse-file "items")
            (filter #(item->tag (:id %)))
            (filter #(nil? (:github_id (:content %))))))


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
                 :db/cardinality :db.cardinality/one}])))

(distinct (map (comp keys :content) items))
(def items-tx
  (apply concat
         (for [item items]
           (let [ownerid (item->tag (:id item))
                 itemid (:id item)]
             [(merge {:db/id itemid
                      :item/name (:name item)}
                     (when-let [pgraph (get-in item [:content :paragraph])]
                       {:item/paragraph pgraph})
                     (when-let [url (get-in item [:content :url])]
                       {:item/url url}))
              {:db/id (:user_id item) :user/owns itemid}
              {:db/id ownerid :tag/member itemid}]))))

(def votes (parse-file "votes"))
(pprint (first votes))

(def vote-schema
  [{:db/ident :vote/magnitude
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :vote/left-item
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident :vote/right-item
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :vote/attribute
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   
   {:db/ident :vote/tag
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def votes-tx
  (apply concat
         (filter identity
                 (for [vote votes]
                   (when (:user_id vote)
                     [(merge
                       {:db/id (:id vote)
                        :vote/left-item (:item_a vote)
                        :vote/right-item (:item_b vote)
                        :vote/tag (:tag_id vote)
                        :vote/magnitude (:magnitude vote)}
                       (when-let [atr (:attribute vote)] {:vote/attribute atr}))
                      {:db/id (:user_id vote)
                       :user/owns (:id vote)}])))))

;; TODO change to user/owns for every attr...
(def all-schema
  (concat user-schema tag-schema item-schema vote-schema))
(def all-data
  (concat users-tx tags-tx items-tx votes-tx))

(defn file-pprint [thing fname]
  (pprint (sort-by first thing) (clojure.java.io/writer (str "/home/tommy/Desktop/" fname ".txt"))))

(def cfg {:server-type :peer-server
          :access-key "myaccesskey"
          :secret "mysecret"
          :endpoint "localhost:8998"
          :validate-hostnames false})

(def client (d/client cfg))

(def conn (d/connect client {:db-name "hello"}))

(d/transact conn {:tx-data all-schema})

(def real-ids (set (map :db/id all-data)))

(defn has-ids [tx]
  (select-keys tx [:vote/left-item
                   :vote/right-item
                   :vote/tag
                   :user/owns
                   :tag/member]))

(def garbage (clojure.set/difference (set (apply concat (for [tx all-data]
                                                       (vals (has-ids tx))))) real-ids))
(defn which-is-garbage [mp]
  (filter boolean (for [[k v] mp]
                    (if (contains? garbage v)
                      [k v]))))

(def hasgarbage
  (set (map :db/id (filter boolean (for [tx all-data]
                          (if (not (empty? (clojure.set/intersection garbage (set (vals (has-ids tx))))))
                            tx
                            nil))))))
;; they are all votes..
(def clean-data (->> all-data
                     (filter #(not (contains? hasgarbage (:db/id %))))
                     (filter #(not (contains? hasgarbage (:user/owns %))))))
;; TODO clean {:user}

;; garbage collect data. thing sthat don't reference good things are killed.
(d/transact conn {:tx-data clean-data})

(pprint (duplicates (map keys votes)))

;; TODO SYMEX, something that jumps toplevel blocks.
;; TODO UNIXWIKI: site that shows every TODO i have, scans all gh repos and wiki/keep?

;; TODO



;; TODO use URI attribute type for urls..
;; PROBLEM: not all image urls have a valid extension..., in particular twitter

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
