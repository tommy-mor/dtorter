(ns frontsorter.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub :page/tags :page/tags)
(reg-sub :all (fn [x] x))
(reg-sub :session/user-id :session/user-id)
(reg-sub :current-route :current-route)

(reg-sub :tag :page/tag)
(reg-sub :tag-loaded? (fn [db] (not (nil? (:page/tag db)))))

(reg-sub :pair-loaded?
         :<- [:tag]
         (fn [tag]
           (not (nil? (:pair tag)))))

(comment (-> @(subscribe [:pair-loaded?])))


(reg-sub :left :<- [:tag] (comp :left :pair))

(reg-sub :right :<- [:tag] (comp :right :pair))

(reg-sub :percent :percent)

(reg-sub :side-height :<- [:percent]
         (fn [percent [_ side]]
           (case side
             :right (/ (max 0 (- percent 50)) 2) 
             :left (/ (max 0 (- 50 percent)) 2)
             0)))

;; add panel subs

;; (reg-sub :format #(-> % :tag :settings :format))
;; ranklist subs

(reg-sub :sorted :<- [:tag] :tag.filtered/sorted)
(reg-sub :sorted-count :<- [:sorted] count)
(reg-sub :sorted-not-empty :<- [:sorted-count] (complement zero?)) 

(reg-sub :users :<- [:tag] :interface/users)
(reg-sub :current-user :<- [:tag] :interface.filter/user)

(reg-sub :unsorted :<- [:tag] :tag.filtered/unvoted-items)
(reg-sub :unsorted-count :<- [:unsorted] count)
(reg-sub :unsorted-not-empty :<- [:unsorted-count] (complement zero?))

(reg-sub :votes :<- [:tag] :tag.session/votes)
(reg-sub :votes-count :<- [:votes] count)
(reg-sub :votes-not-empty :<- [:votes-count] (complement zero?))

(reg-sub :idtoname :<- [:sorted] #(into {} (map (juxt :id :name) %)))

(reg-sub :errors #(or (:errors %) []))

;; only for item page
(reg-sub :item-id #(:id (:item %)))
;; (reg-sub :votes :votes)
(reg-sub :vote-on
         :<- [:votes]
         (fn [votes [_ item]]
           (def votes votes)
           (let [vs (filter (comp (fn [x] (x (:xt/id item)))
                                  set
                                  (juxt (comp :xt/id :vote/left-item)
                                        (comp :xt/id :vote/right-item)))
                            votes)]
             (first vs))))

(comment (def pom (first (filter #(= "Pomegranate" (:item/name %)) @(subscribe [:sorted]))))
         (def ban (first (filter #(= "Banana" (:item/name %)) @(subscribe [:sorted]))))
         (def plum (first (filter #(= "Plum" (:item/name %)) @(subscribe [:sorted]))))

         (def plum-vote @(subscribe [:vote-on plum]))
         (def pom-vote @(subscribe [:vote-on pom]))

         (tap> [plum-vote pom-vote]))



(reg-sub :tag-edit-info
         #(select-keys % [:tag/name :tag/description :owner :xt/id]))



