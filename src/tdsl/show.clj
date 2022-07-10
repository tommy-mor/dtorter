(ns tdsl.show
  (:require [tdsl.parse :as parse]
            [cheshire.core :as json]
            [io.pedestal.interceptor.chain :refer [terminate]]
            [io.pedestal.http.route :refer [url-for]]
            [ring.util.response :as ring-resp]
            [clojure.string :as str]
            [dtorter.views.common :refer [html-interceptor]]))

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

(defn page [req]
  (def req req)
  (let [dir (-> req
                :path-params
                :base)
        username (-> req
                     :session
                     :user-name)
        access (or (and (= dir "tdsl")
                        (#{"tommy"} username))
                   (and (= dir "egregore")
                        (#{"tommy" "blobbed"} username)))]

    (def dir dir)
    (def username username)
    (def access access)
    
    
    (if-not access (throw (ex-info "access denied" req)))
    
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
            [:script "frontdsl.page.run(" (json/generate-string (display dir)) ")"]]}))

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


(defn routes []
  [["/b/:base" {:get {:handler page}
                :parameters {:path {:base string?}}
                :interceptors [html-interceptor (only-users #{"tommy"})]}]
   #_["/tdsl/refresh" :get
    [refresh (only-users #{"tommy"})]
    :route-name :tdsl-refresh]])
