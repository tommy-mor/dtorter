(ns dtorter.main
  (:require
   [xtdb.api :as xt]
   [dtorter.data :as data]))

(defn uuid [st] (java.util.UUID/fromString st))

(def node (xt/start-node {}))

(xt/submit-tx node
              [[::xt/put {:xt/id :kaarlang/clients
                          :clients [:blue-energy :gold-harmony :tombaugh-resources]}]])

(xt/sync node) 

(xt/q (xt/db node)
      '{:find [entity swag]
        :where [[entity :vote/tag swag]]})

(xt/sync node)

(pprint (distinct (map keys (get-transactions))))

(xt/submit-tx node (for [tx (get-transactions)]
                     [::xt/put tx]))



