(ns dtorter.views.tag
  (:require [io.pedestal.http.route :refer [url-for]]
            [xtdb.api :as xt]
            [dtorter.queries :as queries]
            [cheshire.core :as json]
            [dtorter.views.common :refer [layout]]
            [dtorter.api :refer [strip]]
            [com.walmartlabs.lacinia :as lacinia]))

(def stest (atom nil))

(defn q [query-string]
  )

(def tagid "09044c15-3d3a-4268-9586-074d8ddf95d9")

(def starting-data-query
  "query starting_data($tagid: ID, $attribute: String)  {
     tag_by_id(id: $tagid) { 
       name description sorted(attribute: $attribute) {name elo}
     }
   }")

(-> (lacinia/execute (:gql-schema @stest) starting-data-query {:tagid tagid
                                                               :attribute "default"} {:db (:db @stest)})
    :data
    :tag_by_id
    
    json/generate-string
    
    )


(def show-all {:vote_panel true
               :vote_edit true
               :edit_tag true})

(re-graph/init {})

(re-graph/query)



(defn initstr [db tag]
  (strip {:tag tag
          :votes []
          :show show-all
          :users []
          :attributes []
          :sorted []
          :left {}
          :right {}
          :unsorted []}))

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
                                                     [:h1 "tag page"]
                                                     [:span (json/generate-string tag)]
                                                     [:div#app.appbody]
                                                     [:script {:type "text/javascript"}
                                                      (jsonstring db tag)]
                                                     [:script {:type "text/javascript"
                                                               :src "/js/app.js"}]
                                                     [:script {:type "text/javascript"}
                                                      "frontsorter.tag.init_BANG_()"]])})))})
