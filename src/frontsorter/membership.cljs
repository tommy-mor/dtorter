(ns frontsorter.membership
  (:require [re-frame.core :as rf :refer [dispatch subscribe]]
            [martian.re-frame :as martian]))

(rf/reg-sub ::tag ::tag)

(rf/reg-event-fx
 ::load-tag
 (fn [_ [_ match]]
   (js/console.log match)
   (def match match)
   {:dispatch [::martian/request
               :tag/get
               {:id (-> match
                        :frontsorter.router/match
                        :path-params
                        :tagid)}
               [::tag-loaded]
               [:frontsorter.events/http-failure]]}))

(rf/reg-event-db
 ::tag-loaded
 (fn [db [_ {:keys [body]}]]
   (def body body)
   (assoc db ::tag body)))

(defn small-item [i]
  [:div.item  {:on-click #(dispatch [:frontsorter.router/navigate
                                     :frontsorter.router/item-view
                                     {:itemid (:xt/id i)}])}
   [:p (:item/name i)]])

(defn small-tag [t]
  [:div.tag
   {:on-click #(dispatch [:frontsorter.router/navigate
                          :frontsorter.router/tag-item-view
                          {:id (:xt/id t)
                           :itemid (:xt/id @(subscribe [:frontsorter.item/toplevel-item]))}])}
   [:p (:tag/name t)]])

(defn show-membership []
  [:div "the item"
   [small-item @(subscribe [:frontsorter.item/toplevel-item])]
   "is in tag"
   [small-tag @(subscribe [::tag])]

   "TODO remove it?"
   "TODO comments?"])
