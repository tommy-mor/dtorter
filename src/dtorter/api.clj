(ns dtorter.api
  (:require [clojure.java.io :as io]
            [clojure.edn  :as edn]
            [clojure.walk :refer [postwalk]]

            [dtorter.queries :as queries]
            [dtorter.mutations :as mutations]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]
            [xtdb.api :as xt]))


(def api-interceptors
  [])

(def api-routes
  [
   ["/tag/:id"
    {:get {:parameters {:path {:id string?}}
           :handler (fn [r]
                      (def r r)
                      {:status 200 :body {:wow (prn-str r)}})}}]])
