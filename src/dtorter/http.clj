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
            
            [ring.middleware.session.cookie :as cookie]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [dtorter.db :as db]
            [dtorter.api :as api]
            [dtorter.swagger :as dagger]
            [dtorter.exceptions]
            [clojure.spec.alpha :as s]
            [shared.specs :as sp]
            [dtorter.views.routes :as views]
            [crypto.random :as random]
            [tdsl.show]
            [dtorter.clean-data :as clean]))

(defonce cookies (middlewares/session {:store (cookie/cookie-store {:key (random/bytes 16)})}))

(defn router [node]
  (pedestal/routing-interceptor
   (http/router
    
    [[""
      {:no-doc true} (views/routes)]
     ["/githubrefresh/:tagid"
      {:no-doc true} clean/refresh]
     ["/tdsl"
      {:no-doc true}
      (tdsl.show/routes)]
     
     ["/api"
      {:interceptors (api/api-interceptors) 
       ;; :parameters
       ;; {:query ::sp/tag-query} ;; don't put these into swagger cause its confusing 
       }
      (conj (api/api-routes)
            ["/swagger.json"
             {:get {:no-doc true
                    :swagger {:info {:title "sorter api"
                                     :description "for sorting things and stuff"}}
                    :handler (dtorter.swagger/create-swagger-handler)}}])]]
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

(defn router-dev [node]
  (io.pedestal.interceptor/interceptor
   {:name :hack-dev
    :enter
    (fn [ctx]
      (def ctx ctx)
      (:enter (router node))
      (let [real-interceptor (router node)]
        ((:enter real-interceptor) (assoc ctx :hack/interceptor real-interceptor))))
    :leave
    (fn [ctx]
      (let [real-fn (-> ctx :hack/interceptor :leave)]
        (real-fn ctx)))}) )

;; TODO use :server/enable-session {}
(defonce server (atom nil))

(defn start [{:keys [prod] :or {prod false}}]
  (def node (dtorter.db/start))
  (-> {:env (if prod :prod :dev)
       ::server/host (if true
                       (if prod "sorter.isnt.online" "localhost")
                       (.. java.net.InetAddress getLocalHost getHostName))
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
      (pedestal/replace-last-interceptor (router-dev node))
      (update ::server/interceptors #(cons cookies %))
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




