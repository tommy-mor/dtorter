(ns dtorter.http
  (:require [io.pedestal.http :as server]

            [reitit.ring :as ring]
            [reitit.http :as http]
            [reitit.coercion.spec]
            [reitit.http.coercion :as coercion]
            [reitit.dev.pretty :as pretty]
            [reitit.http.interceptors.parameters :as parameters]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.pedestal :as pedestal]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.http.interceptors.dev :as dev]

            [reitit.ring.spec :as rrs]

            
            [clojure.core.async :as a]
            [clojure.java.io :as io]

            [muuntaja.core :as m]
            
            [dtorter.views.front-page :as views]
            [hiccup.core :refer [html]]
            
            [ring.middleware.session.cookie :as cookie]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [dtorter.db :as db]
            [dtorter.api :as api]
            [dtorter.swagger :as dagger]
            [dtorter.exceptions]))


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

(defn interceptor [number]
  {:enter (fn [ctx] (update-in ctx [:request :number] (fnil + 0) number))})

(defn router [node]
  (pedestal/routing-interceptor
   (http/router
    ["/api" {:interceptors api/api-interceptors}
     (conj api/api-routes
           ["/swagger.json"
            {:get {:no-doc true
                   :swagger {:info {:title "my api"
                                    :description "of swag"}}
                   :handler (dtorter.swagger/create-swagger-handler)}}])]
    ;; https://github.com/metosin/reitit/blob/master/examples/pedestal-swagger/src/example/server.clj
    ;; TODO add more things here from example
    
    {#_:reitit.interceptor/transform #_dev/print-context-diffs
     :exception pretty/exception
     :validate rrs/validate
     :data {:coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :interceptors  [
                            {:enter #(assoc-in % [:request :node] node)}
                            dtorter.exceptions/middleware
                            swagger/swagger-feature
                            (parameters/parameters-interceptor)
                            (muuntaja/format-negotiate-interceptor)
                            (muuntaja/format-response-interceptor)
                            (muuntaja/format-request-interceptor)
                            (coercion/coerce-response-interceptor)
                            (coercion/coerce-request-interceptor)]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/api/docs"
      :url "/api/swagger.json"
      :config {:validatorUrl nil
               :operationsSorter "alpha"}})
    (ring/create-resource-handler)
    (ring/create-default-handler)))) 

;; TODO use :server/enable-session {}
(defonce server (atom nil))

(defn start [{:keys [prod] :or {prod false}}]
  (def node (dtorter.db/start))
  (-> {:env (if prod :prod :dev)
       ::server/host (if prod "sorter.isnt.online" "localhost")
       ::server/type :jetty
       ::server/port 8080
       ::server/resource-path "/public"
       ::server/routes []}
      
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
      (pedestal/replace-last-interceptor (router node))
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



(comment (reset)
         (stop))




(comment (defn sorter
           ([kw] 
            `(sorter ~kw [1 2 3 4]))
           ([kw body]
            `(sorter ~kw)))

         (dtorter.http/sorter :books))
