(ns tdsl.show
  (:require [tdsl.parse :as parse]
            [garden.core :refer [css]]
            [cheshire.core :as json]))

(defn display []
  (def files (parse/parse-files))
  (def thoughts (sort-by first (apply concat (for [f files]
                                               (for [thought f]
                                                 (first thought))))))
  thoughts)

(def styles
  (css [:.test {:background-color "pink"}]
       [:.swag {:background-color "lightblue"}]
       [:.kw {:vertical-align "top"}]))

(def page
  {:name ::page
   :enter (fn [ctx]
            (let [data 3]
              (assoc ctx :response {:status 200
                                    :html [:html
                                           [:head
                                            [:style styles]
                                            [:script {:src "/js/shared.js"
                                                      :type "text/javascript"}]
                                            [:script {:src "/js/tdsl.js"
                                                      :type "text/javascript"}]]
                                           [:div#app]
                                           [:script "frontdsl.page.run(" (json/generate-string (display)) ")"]]})))})
