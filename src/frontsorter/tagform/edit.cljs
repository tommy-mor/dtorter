(ns frontsorter.tagform.edit
  (:require [reagent.dom :as d]
            [reagent.core :as r]))

(defn ^:export init! []
  (d/render [:h1 "edit tag form"] (.getElementById js/document "app")))
