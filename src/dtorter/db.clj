(ns dtorter.db
  (:require
   [xtdb.api :as xt]
   [clojure.java.io :as io]
   [dtorter.data :as data]))

(defn lmdb-at [f] {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                              :db-dir (io/file f)}})
(defn node [] (if false
                (xt/start-node
                 {:xtdb/index-store (lmdb-at "/tmp/xtdb/idx")
                  :xtdb/document-store (lmdb-at "/tmp/xtdb/ds")
                  :xtdb/tx-log (lmdb-at "/tmp/xtdb/log")})
                (xt/start-node {})))


(defn start []
  (let [node (node)]
    #_(xt/await-tx node (xt/submit-tx node (for [tx (data/get-transactions)]
                                           [::xt/put tx])))
    node))





