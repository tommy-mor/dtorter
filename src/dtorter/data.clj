(ns dtorter.data
  (:require [cheshire.core :refer :all]
            [datomic.client.api :as d]))

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

(defn parse-file [typ]
  (let [slurped (slurp (str "/home/tommy/programming/dtorter/src/dtorter/data/" typ ".json"))]
       (-> slurped
           (parse-string true)
           :lines
           ffirst
           decode-v)))

(def users (parse-file "users"))
(def id->name (into {} (for [user users] [(:id user) (:username user) ])))

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

(d/q '[:find ?hash
       :where
       [?e :user/name "tommy"]
       [?e :user/password-hash ?hash]]
     db)



