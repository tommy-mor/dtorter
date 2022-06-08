(ns frontdsl.page
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [reitit.frontend :as rf]
            [reitit.core]
            [reitit.frontend.easy :as rfe]))

(defonce todos (r/atom {}))

(defonce match (r/atom nil))

(defn query-from-match []
  (-> @match
      :query-params
      :q))

(defn encoded-string-from-match []
  (reitit.core/match->path @match {:q (query-from-match)}))

(defn filter-input-inner []
  [:input {:type "text"
           :value (query-from-match)
           :on-change #(rfe/push-state ::tdsl {} {:q (-> % .-target .-value)})}])

(def filter-input 
  (with-meta filter-input-inner
    {:component-did-mount #(.focus (rdom/dom-node %))}))

(defn tdsl-app []
  (let [q (query-from-match)]
    [:div
     [filter-input]
     [:table 
      [:tbody
       (doall (for [[kw thought url] (->> @todos
                                      (filter #(if (not (empty? q))
                                                 (str/includes? (str (first %)) q)
                                                 true)))]
                [:tr {:key (str kw thought)} [:td.kw [:pre.swag (str kw)] [:a {:href url} (last (str/split url #"/"))]] [:td [:pre thought]]]))]]
     [:a {:href "/tdsl/refresh"
          :on-click #(set! js/document.cookie (str"query=" (encoded-string-from-match)))}
      "refresh from git"]]))

(def routes
  [["/"
    {:name ::tdsl
     :view tdsl-app
     :parameters {:query {:q string?}}}]])

(defn ^:export run [inp]
  (reset! todos (js->clj inp))
  (rfe/start!
   (rf/router routes)
   (fn [m] (reset! match m))
   {:use-fragment true})
  (rdom/render [tdsl-app] (js/document.getElementById "app")))
