(ns frontdsl.page
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]))

(defn tdsl-app [inp]
  [:table 
   (for [[kw thought] inp]
     [:tr [:td.kw [:pre.swag (str kw)]] [:td [:pre thought]]])])

(defn ^:export run [inp]
  (rdom/render [tdsl-app (js->clj inp)] (js/document.getElementById "app")))
