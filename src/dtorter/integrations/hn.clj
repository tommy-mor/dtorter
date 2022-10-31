(ns dtorter.integrations.hn
  (:require [hato.client :as hc]
            [martian.core :as martian]
            [martian.clj-http :as martian-http]
            [clojure.set :refer [rename-keys]]
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

(defn get-top-stories
  "Get the top stories from Hacker News as json"
  []
  (:body (hc/get "https://hacker-news.firebaseio.com/v0/beststories.json" {:as :json})))

(defn get-story
  "Get a story from Hacker News as json"
  [id]
  (:body (hc/get (str "https://hacker-news.firebaseio.com/v0/item/" id ".json") {:as :json})))

(comment

  (def stories (doall (for [storyid (take 20 (get-top-stories))]
                        (get-story storyid)))))

(defn make-hn-tag []
  (if (xt/q (xt/db dtorter.http/node) '{:find [?s] :where [[?s :tag/name "hacker news"]]})
    "create tag"
    "return tag"))

(defn sync-hn []
  "find hn tags"
  "find hn stories"
    ))

(-> (first stories)
    (rename-keys {:by :author
                  :id :story-id
                  :kids :comments
                  :score :points
                  :time :created-at
                  :title :story-title
                  :type :story-type
                  :url :story-url}))


;; TODO
;; run these through the api
;; make two tags: hn daily tags, hn total tags.
;; make it run every day at 8am
;; give its own tab in the app??? or don't idk
;; it should have its own first class item type... foreign resources integrate well
;; (and have their own colors).. maybe thats what color means in design=foreign resource type



