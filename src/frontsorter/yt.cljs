(ns frontsorter.yt
  (:require-macros [cljs.core.async.macros :refer [go]])
  
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]))

(def settings
  {:client-id "331497482324-ofis5vjjnn8jmgq2np8f7ralnpndqthm.apps.googleusercontent.com"
   :client-secret "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"})

(def tokens (or (http/parse-query-params
                 (js/localStorage.getItem "yt-oauth"))
                (let [tokens (http/parse-query-params
                              (subs js/window.location.hash 1))]
                  (js/localStorage.setItem "yt-oauth" (subs js/window.location.hash 1))
                  (set! js/window.location.hash "")
                  tokens)))



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

(comment)

(defn make-yt-request []
  (go (http/get (str "https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true&access_token="
                     (:access_token tokens))
                {:with-credentials? false})))

(defn login-with-google []
  (js/console.log "ss")
  (set! js/window.location (str "https://accounts.google.com/o/oauth2/v2/auth?" qs)))

(defn ytv
  []
  [:div
   [:p "youtbe api :)"]
   (when (nil? tokens)
     [:button {:on-click login-with-google}
      "login with google"])
   (when tokens
     [:button {:on-click make-yt-request} "make request with google"])])


