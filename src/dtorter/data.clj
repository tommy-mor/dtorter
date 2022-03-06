(ns dtorter.data
  (:require [cheshire.core :refer :all]))

(def tags (slurp "/home/tommy/programming/dtorter/src/dtorter/data/tags.json"))

(defn decode-kv [[left right]]
  (let [decoded-left (decode-v left)
        decoded-right (decode-v right)]
    (if (nil? decoded-left)
      nil
      {(if (string? decoded-left)
         (keyword decoded-left)
         decoded-left)
       decoded-right})))

(def test (clojure.core/atom 3))

(defn decode-v [[left right]]
  (case left
    "table" (let [mapped (into {} (map decode-kv right))]
              (if (every? #(do
                             (reset! test %)
                             ( number? (key %))) mapped)
                (vec (map second mapped))
                mapped))
    "number" (Integer/parseInt right)
    "string" right
    "boolean" (Boolean/valueOf right)
    "metatable" nil
    "recursion" nil
    "function" nil
    ))

(def jsontags (-> (parse-string tags true)
                  :lines
                  ffirst))

(pprint (second (decode-v jsontags)))




