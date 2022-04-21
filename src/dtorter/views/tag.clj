(ns dtorter.views.tag
  (:require [io.pedestal.http.route :refer [url-for]]
            [xtdb.api :as xt]
            [dtorter.queries :as queries]
            [cheshire.core :as json]
            [dtorter.views.common :refer [layout]]
            [dtorter.api :refer [strip]]
            [com.walmartlabs.lacinia :as lacinia]
            
            [shared.query-strings :as qs]
            [shared.specs :as sp]
            [clojure.spec.alpha :as s]
            [clojure.walk :refer [postwalk]]))

(defn q [ctx query-string args]
  (lacinia/execute (:gql-schema ctx)
                   query-string
                   args
                   {:db (:db ctx)}))

(comment (def tagid "09044c15-3d3a-4268-9586-074d8ddf95d9")
         (def attribute "default"))

(def show-all {:show {:vote_panel true
                      :vote_edit true
                      :edit_tag true}})

(defn get-throwing [map val]
  (let [got (get map val)]
    (if (= got nil)
      (throw (ex-info "couldnt find key in map" {:map map :key val}))
      got)))

(defn gather-info [ctx tid attribute]
  (->  (q ctx qs/starting-data-query {:tagid tagid :attribute attribute})
        (get-throwing :data) :tag_by_id (merge show-all)))

(defn conform-throwing [spec x]
  (let [conformed (s/conform spec x)]
    (if (= conformed ::s/invalid)
      (throw (ex-info "invalid data being sent" (s/explain-data spec x)))
      conformed)))


(defn show [x]
  (def shown x)
  shown
  x)
(defn jsonstring [ctx tag attribute]
  (def ctx ctx)
  (str "var tagid = '" (:xt/id tag) "';\n"
       "var itemid = false;\n"
       "var init = " (->> (gather-info ctx (:xt/id tag) attribute)
                          show
                          (conform-throwing ::sp/db)
                          strip
                          json/generate-string) ";"))

(def tag-page
  {:name ::tag-page
   :enter (fn [{:keys [db request] :as ctx}]
            (let [tid (-> request :path-params :tagid)
                  tag (queries/tag-by-id db tid)
                  attribute (or (-> request :path-params :attribute)
                                "default")] ;; TODO find better default attribute
              (assoc ctx :response {:status 200 :html
                                    (layout request [:div
                                                     ;; [:span (json/generate-string tag)]
                                                     [:div#app.appbody]
                                                     [:script {:type "text/javascript"}
                                                      (jsonstring ctx tag attribute)]
                                                     [:script {:type "text/javascript"
                                                               :src "/js/app.js"}]
                                                     [:script {:type "text/javascript"}
                                                      "frontsorter.tag.init_BANG_()"]])})))})

