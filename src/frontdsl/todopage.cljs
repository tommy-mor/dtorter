(ns frontdsl.todopage
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            
            [frontdsl.page :as page]))

(defn tdsl-app [_]
  [page/editbox])

(defn ^:export run [inp]
  (js/console.log)
  (reset! page/editbox-state (js->clj inp :keywordize-keys true))
  (rdom/render [tdsl-app] (js/document.getElementById "app")))

