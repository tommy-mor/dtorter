(ns dtorter.views.tag
  (:require [io.pedestal.http.route :refer [url-for]]
            [dtorter.queries :as queries]
            [cheshire.core :as json]
            [dtorter.views.common :refer [layout q]]
            [dtorter.util :refer [strip get-throwing]]
            
            [shared.query-strings :as qs]
            [shared.specs :as sp]
            [clojure.spec.alpha :as s]))

(def show-all {:vote_panel true
               :vote_edit true
               :edit_tag true
               :add_items true})

;; TODO get rid of show map, should be calculated on clientside.
(defn add-show [{db :tag_by_id item :item_by_id} attr]
  (cond-> db
    true (assoc :show show-all)
    (nil? (:pair db)) (assoc-in [:show :vote_panel] false)
    
    item (assoc :item item)
    attr (assoc :current-attribute attr)))

;; PROBLEM: we need to chose default attr before running q.
(defn gather-info [ctx tagid itemid]
  (def ctx ctx)
  (def tagid tagid)
  (def itemid itemid)
  (let [attr (queries/biggest-attribute ctx (:node ctx) {:tagid tagid})]
    (def attr attr)
    (->  (q ctx (if itemid
                  qs/item-app-db
                  qs/app-db)
            (cond-> {:info {:tagid tagid :attribute attr}}
              itemid (assoc :itemid itemid)))
         (get-throwing :data)
         (select-keys [:tag_by_id :item_by_id])
         (add-show attr))))

(defn conform-throwing [spec x]
  (let [conformed (s/conform spec x)]
    (if (= conformed ::s/invalid)
      (throw (ex-info "invalid data being sent" (s/explain-data spec x)))
      conformed)))


(defn jsonstring [ctx tagid itemid]
  (str "var tagid = '" tagid "';\n"
       "var itemid = '" itemid "';\n"
       "var init = " (->> (gather-info ctx tagid itemid)
                          strip
                          json/generate-string) ";"))

(gather-info ctx tagid itemid)
(def tag-page
  {:name ::tag-page
   :enter (fn [{:keys [node request] :as ctx}]
            (let [tagid (-> request :path-params :tagid)] ;; TODO find better default attribute
              (assoc ctx :response {:status 200 :html
                                    (layout request [:div
                                                     ;; [:span (json/generate-string tag)]
                                                     [:div#app.appbody]
                                                     [:script {:type "text/javascript"}
                                                      (jsonstring ctx tagid false)]
                                                     [:script {:type "text/javascript"
                                                               :src "/js/app.js"}]
                                                     [:script {:type "text/javascript"}
                                                      "frontsorter.tag.init_BANG_()"]])})))})
(def item-page
  {:name ::item-page
   :enter (fn [{:keys [node request] :as ctx}]
            (let [tagid (-> request :path-params :tagid)
                  itemid (-> request :path-params :itemid)]
              (assoc ctx :response {:status 200 :html
                                    (layout request
                                            [:div
                                             ;; [:span (json/generate-string tag)]
                                             [:div#app.appbody]
                                             [:script {:type "text/javascript"}
                                              (jsonstring ctx tagid itemid)]
                                             [:script {:type "text/javascript"
                                                       :src "/js/app.js"}]
                                             [:script {:type "text/javascript"
                                                       :src "/js/item.js"}]
                                             [:script {:type "text/javascript"}
                                              "frontsorter.item.init_BANG_()"]])})))})

