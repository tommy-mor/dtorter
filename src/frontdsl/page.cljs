(ns frontdsl.page
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]))

(defonce todos (r/atom {}))
(defonce filterbox (r/atom ""))

(defn filter-input []
  [:input {:type "text"
           :value @filterbox
           :on-change #(reset! filterbox (-> % .-target .-value))}])

(def filter-input 
  (with-meta filter-input
    {:component-did-mount #(.focus (rdom/dom-node %))}))

(defn tdsl-app []
  [:div
   [filter-input]
   [:table 
    [:tbody
     (doall (for [[kw thought] (->> @todos
                                    (filter #(if (not (empty? @filterbox))
                                         (str/includes? (str (first %)) @filterbox)
                                         (constantly true))))]
        [:tr [:td.kw [:pre.swag (str kw)]] [:td [:pre thought]]]))]]])

(defn ^:export run [inp]
  (reset! todos (js->clj inp))
  (rdom/render [tdsl-app] (js/document.getElementById "app")))
