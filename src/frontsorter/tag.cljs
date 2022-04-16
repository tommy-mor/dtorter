(ns frontsorter.tag
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.dom :as d]
   [re-frame.core :as rf :refer [dispatch dispatch-sync]]
   [day8.re-frame.http-fx]
   [frontsorter.views :as views]
   [frontsorter.events]
   [frontsorter.subs]
   [frontsorter.attributes]))

;; println now does console.log
(enable-console-print!)

(dispatch-sync [:init-db])

;; initialize app
(defn mount-root []
  (d/render [views/tag-page] (.getElementById js/document "app")))


(defn ^:export init! []
  (mount-root))

