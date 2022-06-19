(ns dtorter.http
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [dtorter.views.front-page :as views]
            [hiccup.core :refer [html]]
            
            [ring.middleware.session.cookie :as cookie]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [dtorter.db :as db]
            [dtorter.api]))


(def cookies (middlewares/session {:store (cookie/cookie-store)}))

(defn common-interceptors [schema node]
  [cookies
   ;; https://souenzzo.com.br/clojure-ssr-app-with-pedestal-and-hiccup.html
   {:name  ::html-response
    :leave (fn [{:keys [response]
                 :as   ctx}]
             (if (contains? response :html)
               (let [html-body (->> response
                                    :html
                                    html
                                    (str "\n"))]
                 (assoc ctx :response (-> response
                                          (assoc :body html-body)
                                          (assoc-in [:headers "Content-Type"] "text/html"))))
               ctx))}])

;; TODO use :server/enable-session {}
(defonce server (atom nil))

(defn start [{:keys [prod] :or {prod false}}]
  (def node (dtorter.db/start))
  (-> {:env (if prod :prod :dev)
       ::server/host (if prod "sorter.isnt.online" "localhost")
       ::server/type :jetty
       ::server/port 8080
       ::server/resource-path "/public"
       #_::server/routes #_(views/routes (common-interceptors resolver
                                                          node))}
      
      #_(update ::server/routes route/expand-routes)
      ;; TODO make prod make this evaluated, not a function
      #_(assoc ::server/routes #(let [resolver (dtorter.api/load-schema node)]
                                (-> {::server/routes
                                     (views/routes (common-interceptors
                                                    resolver node))}
                                    (enable-graphql resolver)
                                    (enable-ide resolver)
                                    ::server/routes
                                    route/expand-routes)))
      
      (merge {::server/join? prod
              ::server/allowed-origin {:creds true :allowed-origins (constantly true)}
              ;; TODO use a nonce on the script tag which does frontsorter.initbang
              ::server/secure-headers
              {:content-security-policy-settings
               "object-src 'none'; default-src * ws: wss: 'self' 'unsafe-eval' 'unsafe-inline'"}})

      

      server/default-interceptors
      server/dev-interceptors
      server/create-server
      server/start
      (as-> $ (reset! server $))))

(defn stop []
  (server/stop @server))

(defn reset []
  (when @server
    (stop))
  (start {}))

(comment (reset))




(comment (defn sorter
           ([kw] 
            `(sorter ~kw [1 2 3 4]))
           ([kw body]
            `(sorter ~kw)))

         (dtorter.http/sorter :books))










