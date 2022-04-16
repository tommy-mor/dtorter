(ns frontsorter.views
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as str]
            [frontsorter.common :as c]
            ["./../tagpage/CreateTagPage" :as foo]
            [frontsorter.attributes :as attrs]))


(defn addpanel []
  (let [fields (c/fields-from-format
                @(subscribe [:format]))]
    [:> foo/ItemCreator {:inputList fields}]))

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
  [c/hoveritem ^{:key (:id item)} {:on-click #(let [url (str "/t/" js/tagid "/" (:id item))]
                                                (set! js/window.location.href url))
                                   :key (:id item)}
   
   (when (:elo item)
     
     [:td {:key 1} (-> (* 10
                          @(subscribe [:sorted-count])
                          (:elo item))
                       (.toFixed 2))])
   ;; customize by type (display url for links?)
   
   [:td {:key 2} (:votecount item)]
   [:td {:key 3} (:name item)]])

(defn sortedlist [sorted sorted-count]
  (let [size @(subscribe [sorted-count])
        sorted @(subscribe [sorted])
        all-users @(subscribe [:all-users])
        selected-user @(subscribe [:selected-user])]
    
    [:div "by user "
     [:form {:autoComplete "off"}
      [:select {:on-change #(dispatch [:user-selected (.. % -target -value)])
                :value selected-user
                :autoComplete "nope"}  
       [:option {:value "all users"} "all users combined"]
       (for [user all-users]
         [:option {:key user :value user} user])]]

     
     [:table
      [:thead
       [:tr [:th ""] [:th ""] [:th ""]]]
      [:tbody
       (doall
        (for [item-i sorted]
          (let [item-i (assoc item-i :key (:id item-i))]
            [item item-i])))]]]))

(defn votelist []

  [:table
   [:thead
    [:tr [:th "left"] [:th "pts"] [:th "right"] [:th "pts"]]]
   [:tbody
    (let [idtoname @(subscribe [:idtoname])
          votes @(subscribe [:votes])]
      
      (doall (map (fn [i]
                    [:tr.vote 
                     {:key (:id i)}
                     [:td (idtoname (:item_a i))]
                     [:td (- 100 (:magnitude i))]
                     [:td (idtoname (:item_b i))]
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
     
     (when (:vote_edit show)
       [c/collapsible-cage
        false
        (str "MY VOTES (" @(subscribe [:votes-count]) ") on attribute "
             (or @(subscribe [:current-attribute])
                 "default"))
        "votinglistpanel"
        [votelist]])]))

