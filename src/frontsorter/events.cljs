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
               [::refresh-db-vote]]}))

(reg-event-db
 ::refresh-db-vote
 interceptor-chain
 (fn [db [_ {:keys [data errors] :as payload}]]
   (merge db (:vote data))))




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
   {:db (assoc-in db [:users :user] new-user)
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

(comment (reg-event-db
          :cancelvote
          interceptor-chain
          (fn [db _]
            (voting->item db))))

;; attribute system





;; jail

