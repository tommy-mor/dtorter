(ns frontsorter.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]
            [shared.specs :as sp]))

(s/def ::db ::sp/db)


(def default-db nil)





;; TODO
;; (defn initdata []
;;   (handleresponse {:body (js->clj js/init :keywordize-keys true)
;;                    :success true}))

;; TODO add more spec to this 
