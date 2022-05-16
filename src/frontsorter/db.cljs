(ns frontsorter.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as re-frame]
            [shared.specs :as sp]))

(def default-db nil)

(comment (->> @re-frame.db/app-db
              (s/valid? ::sp/db))
         (-> @re-frame.db/app-db
             (select-keys [:attributes :attributecounts :current-attribute]))
         (->> @re-frame.db/app-db
              keys))


















;; TODO
;; (defn initdata []
;;   (handleresponse {:body (js->clj js/init :keywordize-keys true)
;;                    :success true}))

;; TODO add more spec to this 
