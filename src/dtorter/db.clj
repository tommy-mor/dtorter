(ns dtorter.db
  (:require
   [xtdb.api :as xt]
   [clojure.java.io :as io]
   [dtorter.data :as data]
   [babashka.fs :as fs]))

(comment
  (defn lmdb-at [f] {:kv-store {:xtdb/module 'xtdb.lmdb/->kv-store
                                       :db-dir (io/file f)}})
  (def node (xt/start-node
                    {:xtdb/index-store (lmdb-at "/tmp/idx")
                     :xtdb/document-store (lmdb-at "/tmp/ds")
                     :xtdb/tx-log (lmdb-at "/tmp/log")})))



(defn create-tag-from-dir [dir]
  (let [user {:xt/id "OWNER"
              :user/name "owner"
              :user/password-hash
              "1$10$argon2i$v13$742WQ/0oGNX15jYCqhMlmg$ucrTgNssVJ14z2Z2WKpdydAGVFsNZRDBsUOqXaLy2Wk$$$"}
        tag {:xt/id "file-tag"
             :tag/name (str "files in " dir)
             :tag/description "-----"
             :tag/owner "OWNER"}
        files (->> (fs/list-dir dir)
                   (map fs/file)
                   (map #(hash-map :xt/id (str (.toURL %))
                                   :item/name (.getName %)
                                   :item/owner "OWNER"
                                   :item/tags ["file-tag"]
                                   :item/url (str (.toURL %)))))]
    (into [user tag] files)))

(defn start []
  (let [node (xt/start-node {})]
    (xt/await-tx node (xt/submit-tx node (for [tx (create-tag-from-dir ".")]
                                           [::xt/put tx])))
    node))


