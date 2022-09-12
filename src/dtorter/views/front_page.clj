(ns dtorter.views.front-page
  (:require [io.pedestal.http.body-params :as body-params]
            [hiccup.core :refer [html]]
            [cryptohash-clj.encode :as enc]
            [xtdb.api :as xt]
            [dtorter.hashing :as hashing]
            [dtorter.queries :as queries]
            [cheshire.core :as json]
            [dtorter.views.tag :as tag]
            [dtorter.views.common :as common]

            [tdsl.show]
            [clojure.string :as str]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayOutputStream]))

(defn encode-transit-string [data]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer data)
    (str/escape (.toString out)
                {\" "\\\"" 
                 \\ "\\\\"})))

(defn page [request]
  (def request request)
  (def user (-> request :session :user-id))
  (def node (:node request))
  (def tags (xt/q (xt/db node)
                  '[:find
                    (pull tid [*
                               {:item/_tags [*]}
                               {:vote/_tag [*]}])
                    :in userid
                    :where
                    [tid :type :tag]]
                  user))

  (def initial-state {:page/tags (for [tag tags
                                       :let [tag (first tag)]]
                                   (assoc tag
                                          :item/_tags (count (:item/_tags tag))
                                          :vote/_tag (count (:item/_tag tag))))
                      :session/user-id user})
  
  [:div.frontpage
   [:script {:src "/js/frontpage.js" :type "text/javascript"}]
   [:div#app]
   [:script {:type "text/javascript"}
    (str "frontsorter.page.init_BANG_('"
         (encode-transit-string initial-state)
         "')")]])

;; TODO put these into arguments to init/bang, not here



