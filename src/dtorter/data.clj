(ns dtorter.data
  (:require [cheshire.core :refer :all]
            [clojure.pprint :refer [pprint]]))

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


;; TODO change to user/owns for every attr...


; (pprint (duplicates (map keys votes)))

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
