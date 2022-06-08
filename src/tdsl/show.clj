(ns tdsl.show
  (:require [tdsl.parse :as parse]
            [garden.core :refer [css]]
            [cheshire.core :as json]
            [io.pedestal.interceptor.chain :refer [terminate]]
            [io.pedestal.http.route :refer [url-for]]
            [ring.util.response :as ring-resp]))

(defn display []
  (def files (parse/parse-files))
  (def thoughts (sort-by first (apply concat (for [f files]
                                               (for [thought f]
                                                 (conj (first thought) (-> f
                                                                           meta
                                                                           :source-file
                                                                           str)))))))
  thoughts)


(def styles
  (css [:.test {:background-color "pink"}]
       [:.swag {:background-color "lightblue"
                :max-width "120"}]
       [:.kw {:vertical-align "top"}]))

(defn only-users [users]
  {:name :filter :enter
   #(if (-> % :request :session :user-name users)
      %
      (terminate
       (assoc % :response {:status 404})))})

(def page
  {:name ::page
   :enter (fn [ctx]
            (assoc ctx :response
                   {:status 200
                    :html [:html
                           [:head
                            [:style styles]
                            [:script {:src "/js/shared.js"
                                      :type "text/javascript"}]
                            [:script {:src "/js/tdsl.js"
                                      :type "text/javascript"}]]
                           [:div#app]
                           [:script "frontdsl.page.run(" (json/generate-string (display)) ")"]]}))})

(def refresh
  {:name ::refresh
   :enter (fn [ctx]
            
            (parse/update-files)
            
            (let [qs (-> ctx
                         :request
                         :cookies
                         (get "query")
                         :value)]
              
              (assoc ctx :response (ring-resp/redirect (str (url-for :tdsl-page) "#" qs)))))})


(def users #{"tommy" "owner"})
(defn routes [common-interceptors]
  #{["/tdsl" :get
     (into common-interceptors [page (only-users users)])
     :route-name :tdsl-page]
    ["/tdsl/refresh" :get
     (into common-interceptors [refresh (only-users users)])
     :route-name :tdsl-refresh]})
