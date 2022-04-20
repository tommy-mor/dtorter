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

(def stest (atom nil))

(defn q [ctx query-string args]
  (lacinia/execute (:gql-schema ctx)
                   query-string
                   args
                   {:db (:db ctx)}))

(def tagid "09044c15-3d3a-4268-9586-074d8ddf95d9")




(def show-all {:show {:vote_panel true
                      :vote_edit true
                      :edit_tag true}})
(comment (->>  (q @stest qs/starting-data-query {:tagid tagid :attribute "default"})
               :data :tag_by_id (merge show-all) (s/explain ::sp/db)))

(defn initstr [db tag]
  (strip {:tag tag
          :votes []
          :show show-all
          :users []
          :attributes []
          :sorted []
          :pair {}
          :unsorted []}))

(comment 
  (def tag {:name "math explainer videos" ,
            :description "think numberphile/mathologer/3b1b" ,
            :owner "092d58c9-d64b-40ab-a8a2-d683c92aa319" ,
            :id "0afbe57c-fd32-4779-9a72-e375c6159325"})


  (s/explain ::sp/db (initstr nil tag)))

(defn jsonstring [db tag]
  (str "var tagid = '" (:xt/id tag) "';\n"
       "var itemid = false;\n"
       "var init = " (json/generate-string (initstr db tag)) ";"))

(def tag-page
  {:name ::tag-page
   :enter (fn [{:keys [db request] :as ctx}]
            (let [tid (-> request :path-params :tagid)
                  tag (queries/tag-by-id db tid)]
              (reset! stest ctx)
              (assoc ctx :response {:status 200 :html
                                    (layout request [:div
                                                     ;; [:span (json/generate-string tag)]
                                                     [:div#app.appbody]
                                                     [:script {:type "text/javascript"}
                                                      (jsonstring db tag)]
                                                     [:script {:type "text/javascript"
                                                               :src "/js/app.js"}]
                                                     [:script {:type "text/javascript"}
                                                      "frontsorter.tag.init_BANG_()"]])})))})
