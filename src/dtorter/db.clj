(ns dtorter.db
  (:require
   [xtdb.api :as xt]
   [clojure.java.io :as io]
   [dtorter.data :as data]))

(comment
  (defn lmdb-at [f] {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                                       :db-dir (io/file f)}})
  (def node (xt/start-node
                    {:xtdb/index-store (lmdb-at "/tmp/idx")
                     :xtdb/document-store (lmdb-at "/tmp/ds")
                     :xtdb/tx-log (lmdb-at "/tmp/log")})))


(defn start []
  (let [node (xt/start-node {})]
    
    (xt/await-tx node (xt/submit-tx node (for [tx (data/get-transactions)]
                                           [::xt/put tx])))
    node))





