(ns frontsorter.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]))


(s/def ::db (s/keys :req
                    [::tag ::item ::sorted ::votes ::show ::users]))

(def default-db nil)




;; TODO
;; (defn initdata []
;;   (handleresponse {:body (js->clj js/init :keywordize-keys true)
;;                    :success true}))

;; TODO add more spec to this 
