(ns dtorter.views.tag
  (:require [io.pedestal.http.route :refer [url-for]]
            [xtdb.api :as xt]
            [dtorter.queries :as queries]
            [cheshire.core :as json]
            [dtorter.views.common :refer [layout]]
            [dtorter.api :refer [strip]]))

(def stest (atom nil))

(def show-all {:vote_panel true
               :vote_edit true
               :edit_tag true})


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
