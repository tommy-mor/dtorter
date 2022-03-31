(ns dtorter.http
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            
            [dtorter.api :as api]
            [dtorter.queries :as queries]
            [dtorter.data :as data]
            [xtdb.api :as xt]

            [hiccup.core :refer [html]]

            [dtorter.views.front-page :as views]))

(def schema (api/load-schema))

;; database stuff

(def node (xt/start-node {}))
(def db (do
          (xt/submit-tx node (for [tx (data/get-transactions)]
                               [::xt/put tx]))
          (xt/sync node)
          (xt/db node)))

(defn enable-graphql [service-map schema]
  (let [interceptors (lp/default-interceptors schema {:db db})]
    (-> service-map
        (update ::server/routes conj
                ["/api" :post interceptors :route-name ::graphql-api]))))

(defn enable-ide [service-map schema]
  (-> service-map
      (update ::server/routes conj
              ["/ide" :get (lp/graphiql-ide-handler {}) :route-name ::graphql-ide])
      (update ::server/routes into
              (lp/graphiql-asset-routes "/assets/graphiql"))
      (assoc ::server/secure-headers nil)))

(def html-response
  "If the response contains a key :html,
     it take the value of these key,
     turns into HTML via hiccup,
     assoc this HTML in the body
     and set the Content-Type of the response to text/html"
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
              ctx))})

(def routes #{["/test" :get
               (fn [r] {:status 200 :body (str "hssellos worl")})
               :route-name :bingus]
              ["/" :get
               [html-response views/front-page] :route-name :front-page]})

(def service
  (-> {:env :prod
       ::server/type :jetty
       ::server/port 8080
       ::server/resource-path "/public"}
      (enable-graphql schema)))
;; https://souenzzo.com.br/clojure-ssr-app-with-pedestal-and-hiccup.html
(defn refresh-routes []
  "regather routes for dev thing"
  (let [schema (api/load-schema)]
    
    (route/expand-routes
     (::server/routes (-> {::server/routes routes}
                          (enable-graphql schema)
                          (enable-ide schema))))))
    


;; taken from https://github.com/pedestal/pedestal/blob/50fe5ea89108998ac5a79a02a44432fd111ea6f8/samples/json-api/src/json_api/server.clj#L11
(defn run-dev
  "runs a service for the terminal"
  []
  (-> service
      (enable-ide schema)
      (merge {:env :dev
              ::server/join? false
              ::server/routes refresh-routes
              ::server/allowed-origin {:creds true :allowed-origins (constantly true)}})
      server/default-interceptors
      server/dev-interceptors
      server/create-server
      server/start))

(defonce server (atom nil))

(defn stop-server [server] (server/stop server))

(defn start-server [old]
  (when old (stop-server old))
  (run-dev))

(defn start [] (swap! server start-server))
(defn stop [] (swap! server stop-server))
(comment
  (start)

  (:io.pedestal.http/interceptors @server)

  (stop)
  (start))
