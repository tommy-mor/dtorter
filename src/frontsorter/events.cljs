(ns frontsorter.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path after
                          reg-fx dispatch-sync]]
   [cljs.spec.alpha :as s]
   [shared.specs :as sp]
   [cljs.reader :refer [read-string]]
   [martian.re-frame :as martian]
   [lambdaisland.uri :as uri]))
;; spec checking from
;; https://github.com/day8/re-frame/blob/master/examples/todomvc/src/todomvc/events.cljs#L49
;; TODO check spec differently for tag page
(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw ::sp/db)))

;; maybe add (path [:tagpage]) to this?
(def interceptor-chain [check-spec-interceptor {:id :def
                                                :before (fn [db]
                                                          (println "loading things")
                                                          (def ctx db)
                                                          (def db (-> db
                                                                      :coeffects :db))
                                                          db)
                                                :after nil}])

;; re-graph stuff
;; fill db with default db
(reg-event-fx
 :init-db
 interceptor-chain
 ;; TODO add spec checking here
 (fn [{:keys [db]} [_ mergee]]
   {:db (let [db (-> js/init
                     read-string)]
          (merge db mergee))}))

(defn appdb-args [db]
  " things that are part of ...appDB fragment, but not every mutation.
   these should be filled in always so state knows how to refresh itself.
  "
  (def tdb db)
  (-> tdb
      :interface.filter/user)
  (let [user (-> db :interface.filter/user)]
    (cond-> {:attribute (-> db :interface.filter/attribute)
             :id js/tagid}
      js/itemid (assoc :itemid js/itemid)
      (not (= user :interface.filter/all-users)) (assoc :user user))))

;; TODO make this only clear the correct error
(reg-event-db :clear-errors interceptor-chain #(assoc % :errors []))
;; TODO add current-attribute to special part of spec
(reg-event-db
 ::refresh-db
 interceptor-chain
 (fn [db [_ {:keys [body errors] :as payload}]]
   ;; TODO add errors to error element
   (def payload payload)
   (merge db body {:percent 50})
   ;; PROBLEM: t has pair
   ))


(defn cancel-vote [db]
  (-> db
      (assoc :item (-> db :pair :left))
      (dissoc :pair :percent)))

(comment (reg-event-fx
          :vote
          (fn [{:keys [db]} _]
            {:db (if js/itemid
                   (cancel-vote db)
                   db)
             :dispatch [:mutate
                        :vote
                        (if js/itemid qs/vote-item qs/vote)
                        (merge (appdb-args db)
                               {:vote_info {:tagid js/tagid
                                            :left_item (-> db :pair :left :id)
                                            :right_item (-> db :pair :right :id)
                                            :attribute (-> db :current-attribute)
                                            :magnitude (-> db :percent)}})
                        [::refresh-db]]}))

         (reg-event-fx
          :delete-vote
          (fn [{:keys [db]} [_ vote]]
            {:dispatch [:mutate
                        :delete-vote
                        qs/del-vote
                        (merge (appdb-args db)
                               {:voteid (:id vote)})
                        [::refresh-db]]}))

         (reg-event-fx
          :add-item
          (fn [{:keys [db]} [_ item]]
            {:dispatch [:mutate
                        :add-item
                        qs/add-item
                        (merge (appdb-args db) {:item_info (merge {:tagid js/tagid} item)})
                        [::refresh-db]]})))

(martian/init (str js/window.location.origin "/api/swagger.json"))
;; TODO implement the piggyback as martian/re-frame middleware AND pedestal middleware :smiling_imp:

(reg-event-fx
 :refresh-state interceptor-chain
 (fn [{:keys [db]} _]
   {:dispatch [::martian/request
               :tag/sorted          
               (appdb-args db)
               [::refresh-db]
               [::http-failure]]}))
;; ui events

(reg-event-db
 :slide
 interceptor-chain
 (fn [db [_ new-perc]]
   (assoc db :percent new-perc)))

(reg-event-fx
 :user-selected
 interceptor-chain
 (fn [{:keys [db]}
      [_ new-user]]
   {:db (assoc db :interface.filter/user new-user)
    :dispatch [:refresh-state [:left :right]]}))


(reg-event-db
 :delete-item-success
 interceptor-chain
 (fn [db _]
   (set! js/window.location (str "/t/" js/tagid))
   (js/console.log "should never get here")
   db))

#_(defn dispatch [query-kw-str rest]
  (re-frame.core/dispatch
   (into [(keyword query-kw-str)]
         (js->clj rest :keywordize-keys true))))

(defn ^:export add_item [item callback]
  (re-frame.core/dispatch [:add-item (js->clj item :keywordize-keys true) callback]))

(defn ^:export edit_item [item callback]
  (re-frame.core/dispatch [:edit-item (js->clj item :keywordize-keys true) callback]))

(defn ^:export delete_item [item callback]
  (re-frame.core/dispatch [:delete-item (js->clj item :keywordize-keys true) callback]))



;; for item page
(comment
  (reg-event-db
   :voteonpair
   interceptor-chain
   (fn [db [_ vote leftitem rightitem]]
     (js/console.log "strst")
     (-> db
         (assoc :pair {:left leftitem
                       :right rightitem}
                :percent (second
                          (frontsorter.common/calcmag vote (:id leftitem))))
         (dissoc :item)))))

(reg-event-db
 :cancelvote
 interceptor-chain
 (fn [db _]
   (cancel-vote db)))





;; attribute system





;; jail

