(ns dtorter.main
  (:require
   [xtdb.api :as xt]
   [dtorter.data :as data]))

(defn uuid [st] (java.util.UUID/fromString st))

(def node (xt/start-node {}))

(xt/submit-tx node
              [[::xt/put {:xt/id :kaarlang/clients
                          :clients [:blue-energy :gold-harmony :tombaugh-resources]}]])

(xt/sync node) 

(xt/q (xt/db node)
      '{:find [entity swag]
        :where [[entity :vote/tag swag]]})

(xt/sync node)

(pprint (distinct (map keys (get-transactions))))

(xt/submit-tx node (for [tx (get-transactions)]
                     [::xt/put tx]))

(defn get-transactions []

  (def users (data/parse-file "users"))
  (def id->username (into {} (for [user users] [(:id user) user])))
  (def users-tx
    (vec (for [user users]
           {:xt/id (:id user)
            :user/name (:username user)
            :user/password-hash (:password_hash user)})))

  (def tags (->>
             (data/parse-file "tags")
             (filter #(not (= "porter test" (:title %))))))

  (def id->tag (into {} (for [tag tags] [(:id tag) tag])))

  (def tags-tx (for [tag tags]
                 {:xt/id (:id tag)
                  :tag/name (:title tag)
                  :tag/description (:description tag)
                  :tag/owner (:user_id tag)}))

  (data/duplicates (map :title tags))

  (def items-in-tags (data/parse-file "items_in_tag"))
  (def item->tag (into {} (for [row items-in-tags]
                            [(:item_id row) (:tag_id row)])))
  (def items (->>
              (data/parse-file "items")
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

  (def votes (data/parse-file "votes"))

  (pprint (distinct (map keys votes)))

  (def votes-tx (filter identity
                        (for [vote votes]
                          (when (:user_id vote)
                            (merge
                             {:xt/id (:id vote)
                              :vote/left-item (:item_a vote)
                              :vote/right-item (:item_b vote)
                              :vote/tag (:tag_id vote)
                              :vote/magnitude (:magnitude vote)
                              :vote/owner (:user_id vote)}
                             (if-let [atr (:attribute vote)]
                               {:vote/attribute atr}
                               {:vote/attribute :default}))))))

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

