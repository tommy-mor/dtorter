(ns frontsorter.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path after
                          reg-fx dispatch-sync]]
   [re-graph.core :as re-graph]
   [cljs.spec.alpha :as s]
   [shared.specs :as sp]
   [shared.query-strings :as qs]))


;; spec checking from
;; https://github.com/day8/re-frame/blob/master/examples/todomvc/src/todomvc/events.cljs#L49
(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw ::sp/db)))

;; maybe add (path [:tagpage]) to this?
(def interceptor-chain [check-spec-interceptor])

;; re-graph stuff
;; fill db with default db
(reg-event-fx
 :init-db
 interceptor-chain
 ;; TODO add spec checking here
 (fn [{:keys [db]} _]
   {:db (let [db (js->clj js/init :keywordize-keys true)]
          (assoc db
                 :percent 50
                 :current-attribute "default")) ;; TODO
    :dispatch [::re-graph/init {:ws nil :http {:url "/api"}}]}))

(reg-fx :delayed
        (fn [event]
          (js/setTimeout
           #(re-frame.core/dispatch event)
           3000)))

;; TODO make this only clear the correct error
(reg-event-db :clear-errors interceptor-chain #(assoc % :errors []))

;; TODO add current-attribute to special part of spec
(comment (clojure.string/replace :strst #"^mutation\s?" "" ))
(reg-event-fx
 :vote
 (fn [{:keys [db]} _]
   {:dispatch [::re-graph/mutate
               :vote
               qs/vote
               {:tagid js/tagid
                :left_item (-> db :pair :left :id)
                :right_item (-> db :pair :right :id)
                :attribute (-> db :current-attribute)
                :magnitude 50}
               [::refresh-db]]}))


(reg-event-db
 ::refresh-db
 interceptor-chain
 (fn [db [_ {:keys [data errors] :as payload}]]
   (merge db data)))




(reg-event-db :handle-refresh
              interceptor-chain
              (fn [db [_ result]] (merge db result {:errors []})))
(reg-event-db :handle-refresh-keeping
              interceptor-chain
              (fn [db [_ keep-keys result]] (merge db
                                                   result
                                                   (select-keys db keep-keys)
                                                   {:errors []})))
(reg-event-db :handle-refresh-callback
              interceptor-chain
              (fn [db [_ callback result]]
                (callback)
                (merge db result {:errors []})))


;; ui events

(reg-event-db
 :slide
 interceptor-chain
 (fn [db [_ new-perc]]
   (assoc db :percent new-perc)))

(defn voting->item [db]
  (-> db
      (assoc :item (:left db))
      (dissoc :left :right :percent)))

(comment
  (reg-event-fx
  :vote
  interceptor-chain
  (fn [{:keys [db]} _]
    (http-effect (if js/itemid
                   (voting->item db)
                   (assoc db :percent 50))
                
                 {:method :post
                  :uri (str "/api/votes")
                  :params (let [current-attribute (:current-attribute db)]
                            (cond-> {:tagid (-> db :tag :id)
                                     :left (-> db :left :id)
                                     :right (-> db :right :id)
                                     :mag (-> db :percent)}

                              true
                              (assoc :attribute current-attribute)))
                  :on-success [:handle-refresh]})))) 

(comment
  (reg-event-fx
  :delete-vote
  interceptor-chain
  (fn [{:keys [db]} [_ vote]]
    (http-effect db {:method :delete
                     :uri (str "/api/votes/" (:id vote))
                     :params (if js/itemid {:itemid js/itemid} {})
                     :on-success [:handle-refresh]}))))
(reg-event-fx
 :user-selected
 interceptor-chain
 (fn [{:keys [db]}
      [_ new-user]]
   {:db (assoc-in db [:users :user] new-user)
    :dispatch [:refresh-state [:left :right]]}))

(comment
  (reg-event-fx
   :add-item
   interceptor-chain
   (fn [{:keys [db]} [_ item callback]]
     (http-effect db {:method :post
                      :uri "/api/items"
                      :params (assoc item :tagid js/tagid)
                      :on-success [:handle-refresh-callback callback]}))))
(comment (reg-event-fx
          :edit-item
          interceptor-chain
          (fn [{:keys [db]} [_ item callback]]
            (http-effect db {:method :put
                             :uri (str "/api/items/" js/itemid)
                             :params (assoc item :tagid js/tagid)
                             :on-success [:handle-refresh-callback callback]}))))
(comment (reg-event-fx
          :delete-item
          interceptor-chain
          (fn [{:keys [db]} [_ voteid]]
            (http-effect db {:method :delete
                             :uri (str "/api/items/" js/itemid)
                             :params {}
                             :on-success [:delete-item-success]
                             :dont-rehydrate true}))))
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
(reg-event-db
 :voteonpair
 interceptor-chain
 (fn [db [_ vote leftitem rightitem]]
   (-> db
       (assoc :left leftitem
              :right rightitem
              :percent (second
                        (frontsorter.common/calcmag vote (:id leftitem))))
       (dissoc :item))))

(reg-event-db
 :cancelvote
 interceptor-chain
 (fn [db _]
   (voting->item db)))

;; attribute system





;; jail
(comment
  (defn http-effect [db m]
    (if (not (:params m))
      (js/console.error "request must have params, or api_respond_to will be confused"))

    (let [current-attribute (:current-attribute db)]
      {:http-xhrio (cond-> m
                     (or
                      (= :post (:method m))
                      (= :delete (:method m))
                      (= :put (:method m)))
                     (assoc :format (ajax/json-request-format))
                     
                     (not (:dont-rehydrate m))
                     (cond-> 
                         true
                       (assoc-in [:params :rehydrate :tagid] js/tagid)
                       
                       js/itemid
                       (assoc-in [:params :rehydrate :itemid] js/itemid)
                       
                       true
                       (assoc-in [:params :rehydrate :attribute] current-attribute) ;; can be null...
                       
                       (not (= "all users" (-> db :users :user)))
                       (assoc-in [:params :rehydrate :username] (-> db :users :user)))
                     
                     true
                     (assoc :response-format (ajax/json-response-format {:keywords? true})
                            :on-failure [:failed-http-req]))
       :db db}))
  (reg-event-fx :failed-http-req
                interceptor-chain
                (fn [{:keys [db]} [_ result]]
                  {:db (case (:status result)
                         500 (assoc db :errors ["internal server error"])
                         (assoc db :errors (:errors (:response result))))
                   :delayed [:clear-errors]})))
