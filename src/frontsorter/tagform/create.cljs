(ns frontsorter.tagform.create
  (:require [reagent.dom :as d]
            [reagent.core :as r]))

(defn ^:export init! []
  (d/render [:h1 "create tag form"] (.getElementById js/document "app")))
