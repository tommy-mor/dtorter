(ns frontsorter.views
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as str]
            [frontsorter.common :as c]
            [frontsorter.attributes :as attrs]
            
            [inside-out.forms :as forms]
            [frontsorter.tagform.create :as tagform]
            [frontsorter.router :as router]
            [frontsorter.item :as item])
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



(defn edit-tag-form [close state]
  (dispatch-sync [:edit-tag state])
  (reset! close false))

(defn tag-edit [close]
  [:div (comment {:on-click #(reset! close false)})
   [tagform/new-tag-form
    @(subscribe [:tag-edit-info])
    (partial edit-tag-form close)]])


(defn tag-info []
  (let [{:tag/keys [name description
                    itemcount usercount votecount]
         :interface/keys [owner]
         :as tag} @(subscribe [:tag])
        user-id @(subscribe [:session/user-id])]
    (def tag tag)
    [c/editable
     
     "TAG"
     (= user-id (:xt/id owner))
     tag-edit
     [:div {:style {:padding-left "10px"}}
      
      (if (= name "gh issues")
        [:a {:style {:background "red"
                     :float "right"}
             :href (str "/githubrefresh/") }
         " REFRESH "])
      (comment [:a {:style {:background "green"
                            :float "right"}
                    :href (str "/t/" "/graph") }
                " graph "])
      [:h1 name]
      [:i description]
      [:br]
      "created by user " [:a (:user/name owner)]
      [:br]
      [:b itemcount] " items "
      [:b votecount] " votes by " [:b usercount] " users"
      
      
      ;; TODO make this use correct plurality/inflection
      ]]))

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
     [:td [c/smallbutton "x" #(dispatch [:delete-vote i])]])])

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

     (when @(subscribe [:pair-loaded?])
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

