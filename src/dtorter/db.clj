(ns dtorter.db
  (:require
   [xtdb.api :as xt]
   [com.stuartsierra.component :as component]
   [clojure.java.io :as io]
   [dtorter.data :as data]))

(comment
  (defn lmdb-at [f] {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                                       :db-dir (io/file f)}})
  (def node (xt/start-node
                    {:xtdb/index-store (lmdb-at "/tmp/idx")
                     :xtdb/document-store (lmdb-at "/tmp/ds")
                     :xtdb/tx-log (lmdb-at "/tmp/log")})))

(defrecord Db [data]
  component/Lifecycle
  (start [this]
    (println "starting db")
    (let [node (xt/start-node {})]
      
      (xt/await-tx node (xt/submit-tx node (for [tx (data/get-transactions)]
                                             [::xt/put tx])))
      (assoc this :node node)))
  (stop [this]
    (assoc this :node nil)))

(defn new-db
  []
  (map->Db {}))




