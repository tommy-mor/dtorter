(ns dtorter.views.front-page
  (:require [io.pedestal.http.body-params :as body-params]
            [hiccup.core :refer [html]]
            [cryptohash-clj.encode :as enc]
            [xtdb.api :as xt]
            [dtorter.hashing :as hashing]
            [dtorter.queries :as queries]
            [cheshire.core :as json]
            [dtorter.views.tag :as tag]
            [dtorter.views.common :refer [layout]]

            [dtorter.views.common :as common]

            [tdsl.show]))

(defn render-tag [{:keys [href-for]} tag]
  (def href-for href-for)
  (def tag tag)
  [:div.tag-small [:a {:href (href-for :tag-page {:id (:xt/id tag)})} (:tag/name tag)]])


(defn page [request]
  (def request request)
  (def user (-> request :session :user-id))
  (def tags (xt/q (xt/db node)
                  '[:find (pull tid [*])
                    :in userid
                    :where
                    [tid :owner userid]
                    [tid :tag/name _]]
                  user))
  
  [:div
   [:h1 "front page"]
   [:ul.frontpage-tag-container
    (for [tag tags]
      (render-tag request (first tag)))]])

;; TODO put these into arguments to init/bang, not here



