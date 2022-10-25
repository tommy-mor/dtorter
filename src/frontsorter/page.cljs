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
      (doall (for [tag @(subscribe [:page/tags])]
               [c/render-tag (assoc tag :key (:xt/id tag))]))
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


