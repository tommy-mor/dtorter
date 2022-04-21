(ns frontsorter.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub :all (fn [x] x))

;; show 
; second arg is a function that takes db
(reg-sub :show :show)

;; tag info subs
;; pair subs

(reg-sub :left (comp :left :pair))

(reg-sub :right (comp :left :pair))

(reg-sub :percent :percent)

(reg-sub :pair
 (fn [query-v _]
   [(subscribe [:left]) (subscribe [:right]) (subscribe [:percent])])

 
 (fn [[left right percent] _]
   {:left left
    :right right
    :percent percent}))

(reg-sub :name :name)
(reg-sub :description :description)
(reg-sub :owner :owner)
(reg-sub :usercount :usercount)
(reg-sub :votecount :votecount)
(reg-sub :itemcount :itemcount)
(reg-sub :tag
         :<- [:name]
         :<- [:description]
         :<- [:owner]
         :<- [:votecount]
         :<- [:usercount]
         :<- [:itemcount]
         (fn [[name desc owner votecount usercount itemcount] _]
           {:name name :description desc :creator owner
            :numvotes votecount :numusers usercount
            :numitems itemcount}))

; this is slightly wrong, because css is a little closer to view
; than computed subscriptions like to be (closer to data)
(reg-sub :pair-arena-style :<- [:format]
         (fn [format _]
           (if (or (-> format :url :youtube)
                   (-> format :url ((keyword "youtube with timestamp"))))
             {:width "150%"
              :transform "translate(-16.6%, 0)"}
             {})))

(reg-sub :side-height :<- [:percent]
         (fn [percent [_ side]]
           (case side
             :right (/ (max 0 (- percent 50)) 2) 
             :left (/ (max 0 (- 50 percent)) 2)
             0)))

(reg-sub :item (fn [db [_ side]] ((comp side :pair) db)))
(reg-sub :urls
         (fn [[_ side]] (subscribe [:item side]))
         (fn [item [_ side]]
           ((juxt :url :embedurl) (item :content))))

(reg-sub :url-format :<- [:format] #(:url %))

;; add panel subs

;; (reg-sub :format #(-> % :tag :settings :format))
(reg-sub :format (constantly {:name true}))

;; ranklist subs

(reg-sub :sorted :sorted)

(reg-sub :sorted-count :<- [:sorted] count)
(reg-sub :sorted-not-empty :<- [:sorted-count] (complement zero?)) 

(reg-sub :users :users)
(reg-sub :selected-user :<- [:users] #(or (:user %) "all users"))
(reg-sub :all-users :<- [:users] :users)

(reg-sub :unsorted :unsorted)
(reg-sub :unsorted-count :<- [:unsorted] count)
(reg-sub :unsorted-not-empty :<- [:unsorted-count] (complement zero?))

(reg-sub :votes :votes)
(reg-sub :votes-count :<- [:votes] count)

(reg-sub :idtoname :<- [:sorted] #(into {} (map (juxt :id :name) %)))

(reg-sub :errors #(or (:errors %) []))

;; only for item page
(reg-sub :item-id #(:id (:item %)))
(reg-sub :votes :votes)
(reg-sub :vote-on
         :<- [:votes]
         (fn [votes [_ item]]
           (get votes (keyword (:id item)))))

(reg-sub :item-stage
         (fn [db _]
           (cond
             (and (:left db)
                  (:right db)
                  (not (:item db))) :voting
             (and (:item db)
                  (not (:left db))
                  (not (:right db))) :itemview
             true (js/console.log "bad state"))))

