(ns dtorter.data
  (:require [cheshire.core :refer :all]
            [clojure.pprint :refer [pprint]]
            [dtorter.hashing :as hashing]
            [clojure.set]))

(defn uuid [st] (java.util.UUID/fromString st))

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

;; https://gist.github.com/a2ndrade/5651419
 
(defn duplicates [col]
  (filter (fn [[k v]] (> v 1)) (frequencies col)))

(defn get-transactions []

  (def users (parse-file "users"))
  (def id->username (into {} (for [user users] [(:id user) user])))
  (def users-tx
    (vec (for [user users]
           {:xt/id (:id user)
            :user/name (:username user)
            :user/password-hash (hashing/hash-pw (:username user))})))

  (def tags (->>
             (parse-file "tags")
             (filter #(not (= "porter test" (:title %))))))

  (def id->tag (into {} (for [tag tags] [(:id tag) tag])))

  (def tags-tx (for [tag tags]
                 {:xt/id (:id tag)
                  :tag/name (:title tag)
                  :tag/description (:description tag)
                  :tag/owner (:user_id tag)}))

  (duplicates (map :title tags))

  (def items-in-tags (parse-file "items_in_tag"))
  (def item->tag (into {} (for [row items-in-tags]
                            [(:item_id row) (:tag_id row)])))
  (def items (->>
              (parse-file "items")
              (filter #(item->tag (:id %)))
              (filter #(nil? (:github_id (:content %))))))
  (def items-tx
    (vec (for [item items]
           (let [ownerid (item->tag (:id item))
                 itemid (:id item)]
             (merge {:xt/id itemid
                     :item/name (:name item)
                     :item/owner (:user_id item)
                     :item/tags [ownerid]}
                    (when-let [pgraph (get-in item [:content :paragraph])]
                      {:item/paragraph pgraph})
                    (when-let [url (get-in item [:content :url])]
                      {:item/url url}))))))

  (def votes (parse-file "votes"))

  (pprint (distinct (map keys votes)))

  (def votes-tx (filter identity
                        (for [vote votes]
                          (when (:user_id vote)
                            (merge
                             {:xt/id (:id vote)
                              :vote/left-item (:item_a vote)
                              :vote/right-item (:item_b vote)
                              :vote/tag (:tag_id vote)
                              :vote/magnitude (max 0 (min 100 (:magnitude vote)))
                              :vote/owner (:user_id vote)}
                             (if-let [atr (:attribute vote)]
                               {:vote/attribute atr}
                               {:vote/attribute "default"}))))))

  (def all-data (concat users-tx tags-tx items-tx votes-tx))
  (def real-ids (set (map :xt/id all-data)))

  (= (count real-ids)
     (count (filter identity real-ids)))

  (defn isuuid [st]
    (try
      (java.util.UUID/fromString st)
      (catch Exception e false)))

  (def garbage (clojure.set/difference (set (filter isuuid (apply concat (map vals all-data))))
                                       real-ids))

  (def hasgarbage (set (filter boolean (for [tx all-data]
                                         (if (not (empty? (clojure.set/intersection garbage
                                                                                    (set (filter isuuid (vals tx))))))
                                           tx
                                           nil)))))

  (def clean-data
    (filter (complement hasgarbage)
            all-data))

  (= (count all-data)
     (+ (count clean-data)
        (count hasgarbage)))
  all-data)


;; TODO SYMEX, something that jumps toplevel blocks.
;; TODO UNIXWIKI: site that shows every TODO i have, scans all gh repos and wiki/keep?


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

;; how to deal with types of content? don't make them part of content map but just attributes.
;; display item based on what data is availible. make api have access to only a 30 character long string for its own purposes.
;; QUESTION: how to deal with image url and normal url. make them different (mutually exclusive?) attributes.
;; mutual exclusion property can be loose (only enforced on frontend input and render, but could be stronger (datomic function))

;; TODO
;; items in tags
;; permissions / schema.

;; user ownership

;; TODO
;; maybe use uuid type everywhere?

;; YOOOOOOOOOOOOO restrictions on data is done by dynamically cretaing a spec, or even just putting an entire spec in the DB as data....
