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
            [frontsorter.yt :as yt]))

(defn render-tag [tag]
  (def tag tag)
  (def size (Math/sqrt (* 10 (+ (:item/_tags tag) (:vote/_tag tag)))))
  [:div.tag-small
   {:style {:font-size (max size 12)}}
   [:a {:on-click #(dispatch [::router/navigate ::router/tag-view {:id (:xt/id tag)}])}
    (:tag/name tag)]])

(defn app []
  [:div
   [:div.tag-small {:style {:color "red"}
                    :on-click #(dispatch [::router/navigate ::router/yt-view])}
    "youtube"]
   (case (-> @(subscribe [::router/current-route]) :data :name)
     ::router/yt-view [yt/ytv]
     [:<>
      (doall (for [tag @(subscribe [:page/tags])]
               [render-tag (assoc tag :key (:xt/id tag))]))
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


