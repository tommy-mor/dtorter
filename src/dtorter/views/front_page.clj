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

            [tdsl.show]))

(defn render-tag [{:keys [href-for]} [tag]]
  (def href-for href-for)
  (def tag tag)
  (def votecount (count (-> tag :vote/_tag)))
  (def itemcount (count (-> tag :item/_tags)))
  (def size (Math/sqrt (* 3 (+ votecount itemcount))))
  [:div.tag-small
   {:style (str "font-size: " (max size 12) "px")}
   [:a {:href (href-for :tag-page {:id (:xt/id tag)})}
    (:tag/name tag)]])


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
                    [tid :owner userid]
                    [tid :tag/name _]]
                  user))
  (xt/q (xt/db node)
        '[:find
                    (pull uid [*])
                    :in uid
                    :where
                    [tid :owner userid]
                    [tid :tag/name _]]
                  user)
  [:div.frontpage
   [:b "goals"]
   (when (= "tommy" (:user/name (xt/pull (xt/db node) '[*] user)))
     [:pre ":goals/before-school
  GOAL: by #inst \"2022-08-29T07:00:00.000-00:00\"
    https://github.com/tommy-mor/dtorter/milestone/1
     (combining tag which combines sorter todos and school todos (TDSL?))

    ability to use markwhen to plan

    STRETCH:
      time features
      (due dates, completed or not things, LATE marking)
      calendar integration? planned/actual scheduling (daily and planning)
      (integration with this page (my homepage))"
      ])
   [:b "all tags"]
   
   [:ul.frontpage-tag-container
    (for [tag tags]
      (render-tag request tag))]])

;; TODO put these into arguments to init/bang, not here



