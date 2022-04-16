(ns frontsorter.spotify
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [reagent.core :as r]
   [reagent.dom :as d]))

(defn extract-key []
  (let [str (.-location.hash js/window)]
    (nth (re-find  #"#access_token=([A-z0-9-]*)" str) 1)))

(defn authreq [] {
                  :with-credentials? false
                  :oauth-token (extract-key)})

(def playlists (r/atom []))

(defn handleresponse [resp]
  (js/console.log  "meme")
  (reset! playlists (-> resp :body :items)))

(def testurl (r/atom ""))
(def fr (r/atom ""))

(defn collectsongs [songssofar url]
  (go
    (let [response (<! (http/get url (authreq)))
          songs (concat songssofar (-> response :body :items))
          next (-> response :body :next)]
      (if next
        (<! (collectsongs songs next))
        songs))))

(defn maketag [url name userurl]
  (go
    (js/console.log "big")
    (let [url url
          response (<! (http/get url (authreq)))
          songs (<! (collectsongs [] url))
          finalresponse (<! (http/post "/priv/spotify/data"
                                       {:json-params {:items songs :name name :desc userurl}}))
          ]
      (js/console.log userurl)
      (js/console.log finalresponse)
      (if (and (:success finalresponse) (-> finalresponse :body :newtagid))
        (js/window.location.replace (-> finalresponse :body :newtagid))))))



(defn auth []
  (go
    (let [url "https://api.spotify.com/v1/me/playlists"
          response (<! (http/get url (authreq)))]
      (handleresponse response))))

(defn home-page []
  [:div
   (for [list @playlists]
     [:div {:key (:id list)
            :on-click #(maketag (-> list :tracks :href)
                                (:name list)
                                (-> list :external_urls :spotify))} (:name list)])])

(defn mount-root []
  (auth)
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
