(ns frontsorter.views
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as str]
            [frontsorter.common :as c]
            [frontsorter.attributes :as attrs]
            
            [inside-out.forms :as forms])
  (:require-macros [inside-out.reagent :refer [with-form]]))

(defn addpanel []
  (with-form [item {:name ?name
                    :url ?url
                    :description ?desc}
              :required [?name]]
    (let [format @(subscribe [:format])
          submit #(dispatch [:add-item @item])]
      [:div
       [:input.addinput
        {:value @?name
         :on-change (fn [e] (reset! ?name (.. e -target -value)))
         :placeholder "name of item"}]
       [:br]
       (when (:url format)
         [:<> [:input.addinput
               {:value @?url
                :on-change (fn [e] (reset! ?url (.. e -target -value)))
                :placeholder "https://example.com"}]]
         [:br])
       (when (:description format)
         [:<> [:textarea.addinput
               {:value @?desc
                :on-change (fn [e] (reset! ?desc (.. e -target -value)))
                :placeholder "this website is about ...."}]]
         [:br])
       [:button {:on-click submit} "add item"] 
       [:pre (prn-str @item)]
       [:pre (prn-str (forms/messages ?name))]])))


(comment (defn addpanel []
   (let [fields (c/fields-from-format
                 @(subscribe [:format]))]
     [:input.addinput {:type "text" :placeholder "item title"}])))

(defn tag-info []
  (let [{:keys [name description
                numusers numitems numvotes
                creator]} @(subscribe [:tag])
        {:keys [edit_tag]} @(subscribe [:show])]
    [c/editable-link
     
     "TAG"
     edit_tag
     (str "/t/" js/tagid "/edit")
     [:div {:style {:padding-left "10px"}}
      
      [:h1 name]
      [:i description]
      [:br]
      "created by user " [:a {:href (:url creator)} (:name creator)]
      [:br]
      [:b numitems] " items "
      [:b numvotes] " votes by " [:b numusers] " users"
      ;; TODO make this use correct plurality/inflection
      ]]))

(defn item [item]
  [c/hoveritem ^{:key (:xt/id item)}
   {:on-click #(let [url (str "/t/" js/tagid "/" (:id item))]
                 (set! js/window.location.href url))
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
            [item item-i])))]]]))

(defn votelist []

 [:table
   [:thead
    [:tr [:th "left"] [:th "pts"] [:th "right"] [:th "pts"]]]
   [:tbody
    (let [idtoname @(subscribe [:idtoname])
          votes @(subscribe [:votes])]
      
      (doall (map (fn [i]
                    (def i i)
                    i
                    [:tr.vote 
                     {:key (:id i)}
                     [:td (-> i :vote/left-item :item/name)]
                     [:td (- 100 (:vote/magnitude i))]
                     [:td (-> i :vote/right-item :item/name)]
                     [:td (:magnitude i)]
                     (if (:vote_edit @(subscribe [:show]))
                       [:td [c/smallbutton "delete" #(dispatch [:delete-vote i])]])])
                  votes)))]])

(defn errors []
  (let [errors @(subscribe [:errors])]
    [:div {:style {:color "red"}}
     (doall
      (for [error errors]
        [:pre error]))]))

(defn tag-page []
  (let [show @(subscribe [:show])]
    [:div
     

     [errors]
     [tag-info]
     
     (when (:add_items show) ;; TODO convert everything reading show dict to be a sub
       [c/collapsible-cage
        true
        "ADD"
        "itempanel"
        [addpanel]])

     (when true
       [c/collapsible-cage
        true
        "ATTRIBUTE"
        ""
        [attrs/attributes-panel]])

     (when (:vote_panel show)
       [c/pairvoter])
     
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
      
      (when @(subscribe [:votes-not-empty])
        [c/collapsible-cage
         false
         (str "MY VOTES (" @(subscribe [:votes-count]) ") on attribute "
              (or @(subscribe [:current-attribute])
                  "default"))
         "votinglistpanel"
         [votelist]])]]))

