(ns frontsorter.tagform.create
  (:require ["./../../tagpage/CreateTagPage" :as foo]
            [reagent.dom :as d]
            [reagent.core :as r]))

(defn ^:export init! []
  (d/render [:> foo/App] (.getElementById js/document "app")))
