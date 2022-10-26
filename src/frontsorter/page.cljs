(ns frontsorter.page
  (:require [reagent.dom :as d]
            [reagent.core :as r]
            [frontsorter.common :as c]
            [re-frame.core :as rf :refer [dispatch dispatch-sync subscribe]]
            [frontsorter.router :as router]
            [frontsorter.events]
            [frontsorter.subs]
            [frontsorter.attributes]
            [frontsorter.item :as item]
            [frontsorter.views :as views]
            [frontsorter.yt :as yt]
            [frontsorter.membership :as membership]))

(defn app []
  [:div
   [:div.tag-small {:style {:color "red"}
                    :on-click #(dispatch [::router/navigate ::router/yt-view])}
    "youtube"]
   (case (-> @(subscribe [::router/current-route]) :data :name)
     ::router/yt-view [yt/ytv]
     ::router/membership-view [membership/show-membership]
     [:<>
      [:table.tag-table
       [:thead
        [:tr [:td "Name"] [:td "Description"] [:td "Items"] [:td "Votes"]]]
       [:tbody
        (doall (for [tag @(subscribe [:page/tags])]
                 (do
                   (def tag tag)
                   [:tr {:on-click #(dispatch [::router/navigate
                                               ::router/tag-view
                                               {:id (:xt/id tag)}])
                         :style {:cursor "pointer"}}
                    [:td (:tag/name tag)]
                    [:td (:tag/description tag)]
                    [:td (:vote/_tag tag)]
                    [:td (:item/_tags tag)]])))]]
      (when @(subscribe [:tag-loaded?])
        [views/tag-page])
      (when @(subscribe [::item/toplevel-item-loaded?])
        [item/itempanel])])])


(defn render []
  (d/render [app]
            (.getElementById js/document "app")))

(defn ^:export init! [initial-state]
  (dispatch-sync [:init-db-str initial-state])
  (router/init-routes!)
  (render))


