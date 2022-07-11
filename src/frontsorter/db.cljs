(ns frontsorter.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]
            [shared.specs :as sp]))

(def default-db {})

(comment (tap> @re-frame.db/app-db))



















;; TODO
;; (defn initdata []
;;   (handleresponse {:body (js->clj js/init :keywordize-keys true)
;;                    :success true}))

;; TODO add more spec to this 
