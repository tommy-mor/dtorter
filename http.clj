(ns dtorter.http
  (:require [io.pedestal.http :as http]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            [com.walmartlabs.lacinia.schema :as schema]
            [dtorter.api :as api]))

(def schema (api/load-schema))

(def service (lp/default-service schema nil))

(defonce runnable-service (http/create-server service))

(http/start runnable-service)

runnable-service




