(ns tdsl.show
  (:require [tdsl.parse :as parse]
            [cheshire.core :as json]
            [io.pedestal.interceptor.chain :refer [terminate]]
            [io.pedestal.http.route :refer [url-for]]
            [ring.util.response :as ring-resp]
            [clojure.string :as str]))

(defn display [dir]
  (def files (parse/parse-files dir))
  (def thoughts (sort-by first (apply concat (for [f files]
                                               (for [thought f]
                                                 (first thought))))))

  (->> thoughts
       (filter (comp (complement nil?) second))
       (filter (comp not #{(keyword "(ns")} first))))

(defn only-users [users]
  {:name :filter :enter
   #(if (-> % :request :session :user-name users)
      %
      (terminate
       (assoc % :response {:status 404})))})

(def page
  {:name ::page
   :enter (fn [ctx]
            (let [dir (-> ctx
                          :request
                          :path-params
                          :base)
                  username (-> ctx
                               :request
                               :session
                               :user-name)
                  access (or (and (= dir "tdsl")
                                  (#{"tommy"} username))
                             (and (= dir "egregore")
                                  (#{"tommy" "blobbed"} username)))]
              
              (if-not access (throw (ex-info "access denied" ctx)))
              
              (assoc ctx
                     :response
                     {:status 200
                      :html [:html
                             [:head
                              [:link {:href "/css/site.css"
                                      :rel "stylesheet"
                                      :type "text/css"}]
                              [:script {:src "/js/shared.js"
                                        :type "text/javascript"}]
                              [:script {:src "/js/tdsl.js"
                                        :type "text/javascript"}]]
                             [:div#app]
                             [:script "frontdsl.page.run(" (json/generate-string (display dir)) ")"]]})))})

(def refresh
  {:name ::refresh
   :enter (fn [ctx]

            (let [page  (-> ctx
                            :request
                            :headers
                            (get "referer")
                            (str/split #"/")
                            last)
                  qs (-> ctx
                         :request
                         :cookies
                         (get "query")
                         :value)]
              
              (parse/update-files page)
              
              (assoc ctx :response (ring-resp/redirect (str (url-for :tdsl-page :params {:base page}) "#" qs)))))})


(defn routes [common-interceptors]
  #{["/tdsl/b/:base" :get
     (into common-interceptors [page (only-users #{"tommy"})])
     :route-name :tdsl-page]
    ["/tdsl/refresh" :get
     (into common-interceptors [refresh (only-users #{"tommy"})])
     :route-name :tdsl-refresh]})
