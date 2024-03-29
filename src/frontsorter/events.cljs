(ns frontsorter.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path after
                          reg-fx dispatch-sync]]
   [cljs.spec.alpha :as s]
   [shared.specs :as sp]
   [cljs.reader :refer [read-string]]
   [martian.re-frame :as martian]
   [lambdaisland.uri :as uri]
   [cognitect.transit :as transit]
   [frontsorter.router :as router]))


;; spec checking from
;; https://github.com/day8/re-frame/blob/master/examples/todomvc/src/todomvc/events.cljs#L49
(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw ::sp/db)))


(def router-sync-interceptor
  {:id :router-sync
   :before identity
   :after
   (fn [ctx]
     "if the current database suggests a different route than we have, update the route (skipping the controller)"

     (def ctx ctx)
     (def current-query (-> ctx
                            :effects :db
                            :current-route
                            :query-params))
     (def desired-query
       (let [attribute-filter (-> ctx :effects :db :page/tag :interface.filter/attribute)
             user-filter (-> ctx :effects :db :page/tag :interface.filter/user)]
         (cond-> {}
           (and attribute-filter (not= attribute-filter :interface.filter/no-attribute))
           (assoc :attribute attribute-filter)
           
           (and user-filter (not= user-filter :interface.filter/all-users))
           (assoc :user user-filter))))
     
     
     (if (and current-query desired-query
              (not= current-query desired-query)
              (not (empty? desired-query)))
       (-> ctx
           (assoc-in [:effects ::router/navigate-secret!]
                     [:nav
                      (-> ctx :effects :db :current-route :data :name)
                      (-> ctx :effects :db :current-route :path-params)
                      desired-query])
           (update-in [:effects :db :page/tag] dissoc :interface.filter/attribute)
           (update-in [:effects :db :page/tag] dissoc :interface.filter/user))
       ctx))})

;; maybe add (path [:tagpage]) to this?
(def interceptor-chain [router-sync-interceptor
                        check-spec-interceptor
                        {:id :def
                         :before
                         (fn [ctx]
                           (def db (-> ctx :coeffects :db))
                           ctx)
                         :after nil}])


(reg-event-fx
 :init-db-str
 interceptor-chain
 (fn [{:keys [db]} [_ initstr]]
   {:db (merge (transit/read (transit/reader :json) initstr)
               {:current-route nil})}))

(defn refresh-args-match
  [match]
  (apply merge (vals (:parameters match))))

(defn appdb-args [db]
  " things that are part of ...appDB fragment, but not every mutation.
   these should be filled in always so state knows how to refresh itself. "
  (refresh-args-match (:current-route db)))

;; TODO make this only clear the correct error
(reg-event-db :clear-errors interceptor-chain #(assoc % :errors []))

;; TODO add current-attribute to special part of spec
(reg-event-fx
 ::refresh-db
 interceptor-chain
 (fn [{:keys [db]} [_ {:keys [body errors] :as payload}]]
   ;; TODO add errors to error element
   (def payload payload)
   
   {:db (merge db {:page/tag body
                   :percent 50})}))

(reg-event-db
 :error
 interceptor-chain
 (fn [db [_ error]]
   ;; TODO add errors to error element
   (update db :errors conj error)))

(reg-event-fx
 ::http-failure
 interceptor-chain
 
 (fn [_ [_ error]]
   (js/console.log error)
   ;; TODO add errors to error element
   {:dispatch [:error (ex-message error)]}
   
   
   ;; PROBLEM: t has pair
   ))


(martian/init (str js/window.location.origin "/api/swagger.json"))
;; TODO implement the piggyback as martian/re-frame middleware AND pedestal middleware :smiling_imp:

(reg-event-fx
 :refresh-state
 interceptor-chain
 (fn [{:keys [db]} [_ {::router/keys [match]}]]
   (let [db (cond-> db match (assoc :current-route match))]
     {:dispatch [::martian/request
                 :tag/sorted          
                 (appdb-args db)
                 [::refresh-db]
                 [::http-failure]]})))

(reg-event-fx
 :vote
 (fn [{:keys [db]} _]
   (if  (= (:interface.filter/attribute db)
           :interface.filter/no-attribute)
     {:dispatch [:error "must specify attribute before voting"]}
     {:db db
      :dispatch [::martian/request
                 :vote/new
                 {:vote/left-item (-> db :page/tag :pair :left :xt/id)
                  :vote/right-item (-> db :page/tag :pair :right :xt/id)
                  :vote/attribute (-> db :current-route :query-params :attribute)
                  :vote/tag (-> db :page/tag :xt/id)
                  :vote/magnitude (:percent db)
                  :owner (-> db :session/user-id)}
                 
                 [:refresh-state]
                 [::http-failure]]})))

(reg-event-fx
 :tag/edit
 (fn [{:keys [db]} [_ state]]
   (def stae state)
   {:dispatch [::martian/request
               :tag/put  (assoc state :id (:xt/id state))
               [:refresh-state]
               [::http-failure]]}))
(reg-event-fx
 :tag/delete
 (fn [{:keys [db]} [_ state]]
   {:dispatch [::martian/request
               :tag/delete {:id (:xt/id state)}
               [::martian/request
                :tag/list-all
                {}
                [:tag.after/list-all]]
               [::http-failure]]}))
(reg-event-fx
 :tag.after/list-all
 (fn [{:keys [db]} [_ {:keys [body]}]]
   {:db (-> db
            (assoc :page/tags body)
            (dissoc :page/tag))}))

(reg-event-fx
 :vote/delete
 (fn [{:keys [db]} [_ vote]]
   {:dispatch [::martian/request
               :vote/delete
               {:id (:xt/id vote)}
               [:refresh-state]
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
   {:dispatch [::router/navigate
               (-> db :current-route :data :name)
               (-> db :current-route :path-params)
               (let [query (-> db :current-route :query-params)]
                 (cond
                   (= new-user :interface.filter/all-users)
                   (dissoc query :user)

                   true
                   (assoc query :user new-user)))]}))

