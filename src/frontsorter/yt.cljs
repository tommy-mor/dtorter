(ns frontsorter.yt
  (:require-macros [cljs.core.async.macros :refer [go]])
  
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [frontsorter.events :as events]
            [martian.re-frame :as martian]))

(def settings
  {:client-id "331497482324-ofis5vjjnn8jmgq2np8f7ralnpndqthm.apps.googleusercontent.com"
   :client-secret "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"})

(def tokens (or (let [tokens (http/parse-query-params
                              (subs js/window.location.hash 1))]
                  (js/localStorage.setItem "yt-oauth" (subs js/window.location.hash 1))
                  (set! js/window.location.hash "")
                  (if (:access_token tokens)
                    tokens
                    nil))
                
                (http/parse-query-params
                 (js/localStorage.getItem "yt-oauth"))))



(def yt-scopes ["https://www.googleapis.com/auth/youtube"
                "https://www.googleapis.com/auth/youtube.channel-memberships.creator"
                "https://www.googleapis.com/auth/youtube.force-ssl"
                "https://www.googleapis.com/auth/youtube.readonly"
                "https://www.googleapis.com/auth/youtube.upload"
                "https://www.googleapis.com/auth/youtubepartner"
                "https://www.googleapis.com/auth/youtubepartner-channel-audit"])

(def qs (http/generate-query-string {:client_id (:client-id settings)
                                     :redirect_uri "http://localhost:8080/intg/youtube"
                                     :response_type "token"
                                     :scope (str/join " " yt-scopes)}))

(def videos (r/atom []))
(def pagetoken (r/atom nil))
(def yttag (r/atom nil))

"

{continue with design}
want a purple MAKE THIS INTO TAG button on playlists...             
not sure how to handle indexing of everything.

want to try index first design...

add a MAKE LIKED PLAYLIST TAG button.



rn:
  use martian to find all my tags
  find the tag that says YTLKIED.
  if it does not find it, then create it.
  then add button to add every item in the tag
  and also color indicator if video is added alrdy

"

(defn login-with-google []
  (js/console.log "ss")
  (set! js/window.location (str "https://accounts.google.com/o/oauth2/v2/auth?" qs)))

(defn make-yt-request []
  (go (let [whole (<! (http/get "https://www.googleapis.com/youtube/v3/videos"
                                {:with-credentials? false
                                 :query-params (cond-> {:access_token (:access_token tokens)
                                                        :part "snippet"
                                                        :myRating "like"
                                                        :mine "true"}
                                                 @pagetoken (assoc :pageToken @pagetoken))}))
            resp (:body whole)]
        (js/console.log "uhrstrsth" whole)
        (def whole whole)
        (case (:status whole)
          200 (do
                (swap! videos into (:items resp))
                (reset! pagetoken (:nextPageToken resp)))
          401 (login-with-google)))))

(defn video [ytvv]
  (def ytvv ytvv)
  [:div {:style {:display "flex"}}
   [:button {:on-click #(rf/dispatch [::add-video ytvv])}"add"]
   [:img {:src (-> ytvv :snippet :thumbnails :default :url)}]
   [:a {:href (str "https://youtube.com/watch?v=" (-> ytvv :id))}(-> ytvv :snippet :title)]
   [:p "----"]
   [:p (-> ytvv :snippet :channelTitle)]])

(defn look-for-tag []
  (rf/dispatch [::find-yttag]))

(rf/reg-event-fx
 ::find-yttag
 (fn [{:keys [db]}
      _]
   {:dispatch [::martian/request
               :tag/list-all
               {}
               [::listed-tags]
               [::events/http-failure]]}))

(rf/reg-event-fx
 ::listed-tags
 (fn [{:keys [db]} [_ {:keys [body]}]]
   (def body body)
   (let [search (filter (comp #{"yt liked"} :tag/name) body)]
     (if (empty? search)
       {:dispatch [::martian/request
                   :tag/new
                   {:tag/name "yt liked"
                    :tag/description "youtube liked tag"
                    :owner @(rf/subscribe [:session/user-id])}
                   [::tag-created]
                   [::events/http-failure]]}
       (do
         (js/console.log "erhm")
         (reset! yttag (first search))
         {})))))

(rf/reg-event-fx
 ::add-video
 (fn [{:keys [db]} [_ video]]
   (def video video)
   {:dispatch [::martian/request
               :item/new
               {:item/name (-> video :snippet :title)
                :item/url (str "https://youtube.com/watch?v=" (-> video :id))
                :item/tags [(:xt/id @yttag)]
                :owner @(rf/subscribe [:session/user-id])}
               [::added-video]
               [::events/http-failure]]}))

(rf/reg-event-fx
 ::added-video
 (fn [{:keys [db]} _]
   {}))

(defn ytv
  []
  (when-not @yttag (look-for-tag))
  [:div
   [:p "youtbe api :)"]
   (when @yttag [(resolve 'frontsorter.page/render-tag) @yttag])
   [:ul (doall (for [v @videos]
                 [:li [video v]]))]
   
   (when (nil? tokens)
     [:button {:on-click login-with-google}
      "login with google"])
   (when tokens
     
     [:button {:on-click make-yt-request} "make request with google"])])


