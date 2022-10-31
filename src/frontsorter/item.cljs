(ns frontsorter.item
  (:require
   [reagent.dom :as d]
   [re-frame.core :as rf :refer [dispatch dispatch-sync subscribe]]
   [reagent.core :as r]
   [day8.re-frame.http-fx]
   [frontsorter.events]
   [frontsorter.common :as c]
   [frontsorter.subs]
   [frontsorter.attributes :as attrs]
   [martian.re-frame :as martian]))

(rf/reg-sub ::item (comp :item :page/tag))
(rf/reg-sub ::loaded?
            :<- [::item]
            (comp not nil?))

(rf/reg-sub ::toplevel-item :page/item)
(rf/reg-sub ::toplevel-item-loaded?
            :<- [::toplevel-item]
            map?)

(rf/reg-event-db
 ::vote-on-pair
 frontsorter.events/interceptor-chain
 (fn [db [_ vote leftitem rightitem]]
   (-> db
       (assoc-in [:page/tag :pair] {:left leftitem
                                    :right rightitem})
       (assoc-in [:percent] (first
                             (frontsorter.common/calcmag vote (:id leftitem)))))))
(rf/reg-event-db
 ::unload-item
 frontsorter.events/interceptor-chain
 (fn [db _]
   (js/console.log "unloading item :)")
   (dissoc db :page/item)))

(rf/reg-event-fx
 ::load-item
 frontsorter.events/interceptor-chain
 (fn [{:keys [db]} [_ match]]
   (tap> db)
   (let [itemid (-> match
                    :frontsorter.router/match
                    :path-params
                    :itemid)]
     (js/console.log itemid)
     {:db (dissoc db :page/tag)
      :dispatch [::martian/request
                 :item/get
                 {:id itemid}
                 [::item-loaded]
                 [:frontsorter.events/http-failure]]})))
(rf/reg-event-fx
 ::item-loaded
 frontsorter.events/interceptor-chain
 (fn [{:keys [db]} [_ {body :body}]]
   {:db (assoc db :page/item body)
    :dispatch
    [::martian/request
     :item/memberships
     {:id (:xt/id body)}
     [::membership-loaded]
     [:frontsorter.events/http-failure]]}))


;; only called from js ;; TODO move these
;; views --

(defn votepanel [rowitem]
  (let [itemid (:xt/id @(subscribe [::item]))
        vote @(subscribe [:vote-on rowitem])
        [mag mag2] (c/calcmag vote (:xt/id rowitem))
        ignoreitem @(subscribe [::item])
        editfn #(dispatch [::vote-on-pair vote ignoreitem rowitem])
        delfn #(dispatch [:vote/delete vote])]
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


(defn neighbor [{:tag/keys [name] :as tag}]
  (def tag tag)
  [:div.tag-small
   {:on-click #(dispatch [:frontsorter.router/navigate
                          :frontsorter.router/membership-view
                          {:itemid (:xt/id @(subscribe [::toplevel-item]))
                           :tagid (:xt/id tag)}])}
   [:a name]])


(rf/reg-sub ::memberships ::memberships)

(rf/reg-event-fx
 ::membership-loaded
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (assoc db ::memberships body)})
                 )
(defn add-to-tag []
  (let [collapsed (r/atom false)
        tags-dont-show (set (map :xt/id @(subscribe [::memberships])))
        item @(subscribe [::toplevel-item])]
    (fn []
      (if @collapsed
        [:div (for [tag @(subscribe [:page/tags])]
                (do
                  (js/console.log tag)
                  (when-not (tags-dont-show (:xt/id tag))
                    [:div {:style {:display "flex"}}
                     [:div.tag-small (:tag/name tag)]
                     [:button {:on-click
                                     #(dispatch [::martian/request
                                                 :item/join-tag
                                                 {:id (:xt/id item)
                                                  :tagid (:xt/id tag)
                                                  :owner @(subscribe [:session/user-id])}
                                                 ["AOIESRNAOIRETSN TODO!!!!!!!!!!!!!!"]])}
                      "add"]])))]
        [:div {:on-click #(swap! collapsed not)}
         "click to add this item to another tag"]))))

(defn neighborlist []
  [:div
   [:p "this item is in tags: "]
   [:table.tag-table
    [:tbody
     (doall (for [membership @(subscribe [::memberships])]
              [:tr [neighbor membership]]))]]
   
   [add-to-tag]])


(defn bare-itempanel []
  (let [item (or @(subscribe [::item])
                 @(subscribe [::toplevel-item]))]
    [c/itempanel item]))

(defn itempanel []
  (let [item (or @(subscribe [::item])
                 @(subscribe [::toplevel-item]))]
    [:div.col
     [:div.card
      [:div.card-header "item"]
      [:div.card-body
       [:div.cageparent.item [c/itempanel item]]]]
     [:div.card.mt-2
      [:div.card-header "memberships"]
      [:div.card-body
       [neighborlist]]]]))

(defn rowitem [rowitem]
  (let [item @(subscribe [::item])
        itemid (:xt/id item)]
    [:tr 
     [:td (.toFixed (:elo rowitem) 2)]
     ;; customize by type (display url for links?)
     
     [:td (:item/votecount rowitem)]
     [:td (if (= itemid (:xt/id rowitem))
            [:b (:item/name rowitem)]
            (:item/name item))]
     [votepanel rowitem]
     [:td (let [name [:div
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
    [:table.tag-table
     [:tbody
      (doall (for [n sorted]
               [rowitem (-> n
                            (assoc :key (:xt/id n)))]))]]))





