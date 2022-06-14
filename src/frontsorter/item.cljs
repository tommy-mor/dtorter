(ns frontsorter.item
  (:require
   [reagent.dom :as d]
   [re-frame.core :as rf :refer [dispatch dispatch-sync subscribe]]
   [day8.re-frame.http-fx]
   [frontsorter.views :as views]
   [frontsorter.events]
   [frontsorter.common :as c]
   [frontsorter.subs]
   [frontsorter.attributes :as attrs]))

(dispatch-sync [:init-db {}])





;; only called from js
;; TODO move these
;; views --

(defn back []
  (let [name @(subscribe [:name])]
    [:a {:href (str "/t/" js/tagid)} " << " name]))

#_(defn item-edit [show]
  (let [callback #(reset! show false)]
    [:> foo/ItemCreator {:inputList (c/fields-from-format @(subscribe [:format]))
                         :editItem @(subscribe [:item :item])
                         :editCallback callback}]))

(defn itemv []
  #_[c/editable
   nil
   (:edit_item @show) ;; TODO
   item-edit
     [c/itemview (:format (:settings @tag)) @item 10 false (:type (:settings @tag))]]
  [c/editable
   nil ;; title
   (:edit_item @(subscribe [:show]))
   [:h1 "edit item here"]
   [c/itemview :item]])

(defn votepanel [rowitem]
  (let [vote @(subscribe [:vote-on rowitem])
        [mag mag2] (c/calcmag vote (:id rowitem))
        ignoreitem (or @(subscribe [:item :item])
                       @(subscribe [:item :left]))
        editfn #(dispatch [:voteonpair vote ignoreitem rowitem])
        delfn #(dispatch [:delete-vote vote])]
    (if vote
      [:div.vote
       [:td [:<> "" [:b mag] " vs " [:b mag2] "  " (:name ignoreitem)]]
       [:td 
        [c/smallbutton "edit " editfn]]
       [:td]
       [:td 
        [c/smallbutton " delete" delfn]]]
      (if (= (:id rowitem) js/itemid)
        [:td "--"]
        [:td [c/smallbutton "vote" editfn]]))))


(defn rowitem [rowitem]
  (let [show @(subscribe [:show])]
    (def rowitem rowitem)
    rowitem
    
    (:id rowitem)
    
    [:tr 
     [:td (.toFixed (:elo rowitem) 2)]
     ;; customize by type (display url for links?)
     
     [:td (:votecount rowitem)]
     [:td [:b (if (= js/itemid (:id rowitem))
                "x"
                " ")]]
     [:td (let [url (str "/t/" js/tagid "/" (:id rowitem))
                name [:div
                      {:onClick (fn [] (set! js/window.location.href url))}
                      (:name rowitem)]
                id (:id rowitem)
                right @(subscribe [:item :right])]
            (def right right)
            right
            (cond
              (and right
                   (= id (:id right))) [:b name]
              (= id js/itemid) [:b name]
              true name))]
     
     [:td [:b (if (= (:id @(subscribe [:item :right])) (:id rowitem))
                "x"
                " ")]]
     
     (if (:vote_edit show)
       [votepanel rowitem])]))

(defn ranklist []
  ;; (js/console.log "rank")
  ;; (js/console.log (clj->js  @rank))
  
  (let [sorted @(subscribe [:sorted])]
    [:table
     [:thead
      [:tr [:th "elo"] [:th "#votes"] [:th ""]]]
     [:tbody
      (doall (for [n sorted]
               [rowitem (-> n
                            (assoc :key (:id n)))]))]]))

(defn home-page []
  #_ (fn []
       [:div
        [back @tag]
        (if @score
          [c/pairvoter score (:format (:settings @tag)) sendvote :startopen true :cancelfn #(reset! score nil)]
          [itemv])
        [c/collapsible-cage true
         "MATCHUPS"
         [ranklist]]])
  [:div
   [back]
   (case @(subscribe [:item-stage])
     :itemview [itemv]
     :voting [c/pairvoter :cancelevent [:cancelvote]])
   [c/collapsible-cage true "ATTRIBUTES" ""
    [attrs/attributes-panel]]
   [c/collapsible-cage true "MATCHUPS" "itemranking"
    [ranklist]] 
   ])


(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export init! []
  (mount-root))
