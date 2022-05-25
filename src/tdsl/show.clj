(ns tdsl.show
  (:require [tdsl.parse :as parse]
            [garden.core :refer [css]]))

(-> files
    second
    
    )

(defn display []
  (def files (parse/parse-files))
  (def thoughts (sort-by first (apply concat (for [f files]
                                               (for [thought f]
                                                 (first thought))))))

  (for [[k w] thoughts]
    [:tr
     [:td.kw
      [:pre.swag (str k)]]
     [:td [:pre w]]]))

(def styles
  (css [:.test {:background-color "pink"}]
       [:.swag {:background-color "lightblue"}]
       [:.kw {:vertical-align "top"}]))

(def page
  {:name ::page
   :enter (fn [ctx]
            (assoc ctx :response {:status 200
                                  :html [:html
                                         [:head
                                          [:style styles]]
                                         [:table  
                                          (display)]]}))})
