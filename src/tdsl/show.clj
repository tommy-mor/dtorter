(ns tdsl.show)

(def page
  {:name ::page
   :enter (fn [ctx]
            (def ctx ctx)
            (println "Swag")
            (assoc ctx :response {:status 200
                                  :html [:div [:pre (prn-str ctx)]
                                         [:h1 "stst"]]}))})
