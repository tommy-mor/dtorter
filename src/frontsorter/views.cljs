(ns frontsorter.views
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as str]
            [frontsorter.common :as c]
            [frontsorter.attributes :as attrs]
            
            [frontsorter.tagform.create :as tagform]
            [frontsorter.router :as router]
            [frontsorter.item :as item]))

(defn tag-info []
  (let [{:tag/keys [name description
                    itemcount usercount votecount]
         :interface/keys [owner]
         :as tag} @(subscribe [:tag])
        user-id @(subscribe [:session/user-id])]
    (def tag tag)
    [:div.tag {:on-click #(dispatch [::router/navigate ::router/tag-view {:id (:xt/id tag)}])}
     
     [:div.card-header "tag"]
     [:div.card-body
      
      
      [:h5 name]
      [:i description]
      [:br]
      "created by user " [:a (:user/name owner)]
      [:br]
      [:b itemcount] " items "
      [:b votecount] " votes by " [:b usercount] " users"]]))

(defn item [item]
  [c/hoveritem ^{:key (:xt/id item)}
   {:on-click #(let [route @(subscribe [:current-route]) ]
                 (dispatch [::router/navigate
                            ::router/tag-item-view
                            (assoc (:path-params route)
                                   :itemid (:xt/id item))
                            (:query-params route)]))
    :key (:xt/id item)}
   
   (when (:elo item)
     
     [:td {:key 1} (-> (:elo item)
                       (.toFixed 2))])
   ;; customize by type (display url for links?)
   
   [:td {:key 2} (:item/votecount item)]
   [:td {:key 3} (:item/name item)]])

(defn sortedlist [sorted sorted-count]
  (let [sorted @(subscribe [sorted])
        all-users @(subscribe [:users])
        selected-user @(subscribe [:current-user])]
    
    [:div "by user "
     [:form {:autoComplete "off"}
      [:select {:on-change #(dispatch [:user-selected (let [v (.. % -target -value)]
                                                        (case v
                                                          "-1" :interface.filter/all-users
                                                          v))])
                :value selected-user
                :autoComplete "nope"}  
       [:option {:value "-1"} "all users combined"]
       (for [user all-users]
         [:option {:key (:user/name user)
                   :value (:xt/id user)} (:user/name user)])]]

     
     [:table
      [:thead
       [:tr [:th ""] [:th ""] [:th ""]]]
      [:tbody
       (doall
        (for [item-i sorted]
          (let [item-i (assoc item-i :key (:xt/id item-i))]
            (def item-i item-i)
            [item item-i])))]]]))

(defn voterow [i]
  (def i i)
  [:tr.vote 
   {:key (:xt/id i)}
   [:td (-> i :vote/left-item :item/name)]
   [:td
    [c/mini-slider (:vote/magnitude i)]]
   [:td (-> i :vote/right-item :item/name)]
   (if (= (:owner i) js/userid)
     [:td [c/smallbutton "x" #(dispatch [:vote/delete i])]])])

(defn votelist []

 [:table
   [:thead
    [:tr [:th "left"] [:th "pts"] [:th "right"] [:th "pts"]]]
   [:tbody
    (let [idtoname @(subscribe [:idtoname])
          votes @(subscribe [:votes])]
      (doall (map (fn [i]
                    [voterow (assoc i :key (:xt/id i))])
                  votes)))]])

(defn errors []
  (let [errors @(subscribe [:errors])]
    [:div {:style {:color "red"}}
     (doall
      (for [error errors]
        [:pre error]))]))

(defn tag-page []
  (let [show {}]
    [:div
     

     [errors]
     [:div.row
      [:div.col [tag-info]]
      [:div.col
       [:div.tag
        [:div.card-header
         [attrs/attributes-panel]]
        [:div.card-body
         (when @(subscribe [:pair-loaded?])
           [c/pairvoter])]]]]
     
     
     (when @(subscribe [:frontsorter.item/loaded?])
       [item/itempanel])
     
     [:div.threepanels
      (when @(subscribe [:sorted-not-empty])
        [c/collapsible-cage
         true
         "RANKING"
         "itemranking"
         [sortedlist :sorted :sorted-count]])
      
      (when @(subscribe [:unsorted-not-empty])
        [c/collapsible-cage
         true
         "UNRANKED ITEMS"
         "itemranking"
         [sortedlist :unsorted :unsorted-count]])
      
      (when (and @(subscribe [:votes-not-empty])
                 (not @(subscribe [:frontsorter.item/loaded?])))
        [c/collapsible-cage
         false
         (str "MY VOTES (" @(subscribe [:votes-count]) ") on attribute "
              @(subscribe [:current-attribute]))
         "votinglistpanel"
         [votelist]])
      
      (when @(subscribe [:frontsorter.item/loaded?])
        [c/collapsible-cage
         true
         "item matchup"
         "itemranking"
         [item/ranklist]])]]))

