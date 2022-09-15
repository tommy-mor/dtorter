(ns frontsorter.item
  (:require
   [reagent.dom :as d]
   [re-frame.core :as rf :refer [dispatch dispatch-sync subscribe]]
   [day8.re-frame.http-fx]
   [frontsorter.events]
   [frontsorter.common :as c]
   [frontsorter.subs]
   [frontsorter.attributes :as attrs]
   [martian.re-frame :as martian]))

(js/console.log "loaded?")
(rf/reg-sub ::item (comp :item :page/tag))
(rf/reg-sub ::loaded?
            :<- [::item]
            (comp not nil?))

(rf/reg-event-db
 ::vote-on-pair
 frontsorter.events/interceptor-chain
 (fn [db [_ vote leftitem rightitem]]
   (-> db
       (assoc-in [:page/tag :pair] {:left leftitem
                                    :right rightitem})
       (assoc-in [:percent] (first
                             (frontsorter.common/calcmag vote (:id leftitem)))))))

;; only called from js
;; TODO move these
;; views --

(defn votepanel [rowitem]
  (let [itemid (:xt/id @(subscribe [::item]))
        vote @(subscribe [:vote-on rowitem])
        [mag mag2] (c/calcmag vote (:xt/id rowitem))
        ignoreitem @(subscribe [::item])
        editfn #(dispatch [::vote-on-pair vote ignoreitem rowitem])
        delfn #(dispatch [:delete-vote vote])]
    (if (= (:xt/id rowitem) itemid)
      [:td "--"]
      (if vote
        [:tr.vote
         [c/mini-slider mag]
         (comment [:td [:<> "" [:b mag] " vs " [:b mag2] "  " (:item/name ignoreitem)]])
         [:td 
          [c/smallbutton "edit " editfn]]
         [:td]
         [:td 
          [c/smallbutton " delete" delfn]]]
        [:td [c/smallbutton "vote" editfn]]))))


(defn rowitem [rowitem]
  (let [itemid (:xt/id @(subscribe [::item]))]
    [:tr 
     [:td (.toFixed (:elo rowitem) 2)]
     ;; customize by type (display url for links?)
     
     [:td (:item/votecount rowitem)]
     [:td [:b (if (= itemid (:xt/id rowitem))
                (:item/name rowitem)
                " ")]]
     [votepanel rowitem]
     [:td (let [url (str "/t/" "/i/" (:xt/id rowitem))
                name [:div
                      {:onClick (fn [] (set! js/window.location.href url))}
                      (:item/name rowitem)]
                id (:xt/id rowitem)
                right @(subscribe [:right])]
            (def right right)
            (cond
              (and right
                   (= id (:xt/id right))) [:b name]
              (= id itemid) "--"
              true name))]]))

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
                            (assoc :key (:xt/id n)))]))]]))





