(ns frontsorter.router
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reitit.core :as r]
   [reitit.coercion :as rc]
   [reitit.coercion.spec :as rss]
   [reitit.frontend :as rf]
   [reitit.frontend.controllers :as rfc]
   [reitit.frontend.easy :as rfe]))

;; https://gist.github.com/vharmain/a8bbfa5bc601feba0f421959228139a1
(def routes
  ["/"
   [""
    {:name ::home}]
   ["t/:id"
    {:name ::tag-view
     :path-params {:id string?}
     :controllers [{:identity identity
                    :start (fn [match]
                             (re-frame/dispatch [:refresh-state {::match match}]))}]}]
   ["t/:id/i/:itemid"
    {:name ::tag-item-view
     :path-params {:id string? :itemid string?}
     :controllers [{:identity identity
                    :start (fn [match]
                             (def r match)
                             (re-frame/dispatch [:refresh-state {::match match}]))}]}]])
(def router (rf/router routes))
;; Events

(re-frame/reg-event-fx
 ::navigate
 (fn [db route]
   {::navigate! route}))

(re-frame/reg-event-db
 ::navigated
 (fn [db [_ new-match]]
   (assoc db :current-route new-match)))


;; Subs

(re-frame/reg-sub ::current-route :current-route)

;; Effects

(re-frame/reg-fx
 ::navigate!
 (fn [[_ k params query]]
   (rfe/push-state k params query)))

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))


(re-frame/reg-fx
 ::navigate-secret!
 (fn [[_ k params query]]
   (let [url (href k params query)]
     (js/console.log "secretly navigating to" url)
     (.replaceState js/window.history nil "" url)
     (re-frame/dispatch [::navigated (rf/match-by-path router url)]))))

(defn front-page [] [:div "home page"])
(defn tag-page [] [:div "tag-page"])

(defn on-navigate [new-match]
  (let [old-match (re-frame/subscribe [::current-route])]
    (when new-match
      (let [cs (rfc/apply-controllers (:controllers @old-match) new-match)
            m (assoc new-match :controllers cs)]
        (re-frame/dispatch [::navigated new-match])))))

(defn init-routes! []
  (rfe/start!
   router
   on-navigate
   {:use-fragment false}))

(comment (defn router-component [{:keys [router]}]
           (let [current-route @(re-frame/subscribe [::current-route])]
             [:div
              [:pre (pr-str current-route)]])))

