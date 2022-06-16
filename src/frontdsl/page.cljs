(ns frontdsl.page
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [reitit.frontend :as rf]
            [reitit.core]
            [reitit.frontend.easy :as rfe]

            [frontsorter.common :refer [collapsible-cage]]))

(defonce todos (r/atom {}))

(defonce match (r/atom nil))

(defonce show-body (r/atom true))

(defn query-from-match []
  (-> @match
      :query-params
      :q))

(defn encoded-string-from-match []
  (reitit.core/match->path @match {:q (query-from-match)}))

(defn filter-input-inner []
  [:input.bg-black.text-white.max-width
   {:type "text"
    :value (query-from-match)
    :on-change #(rfe/push-state ::tdsl {} {:q (-> % .-target .-value)})}])

(def filter-input 
  (with-meta filter-input-inner
    {:component-did-mount #(.focus (rdom/dom-node %))}))

(defn tdsl-app []
  (fn []
    (println @show-body)
    (let [q (query-from-match)]
      [:div.max-w-full.max-h-full.overscroll-contain
       [filter-input]
       [:input#show_box {:type "checkbox"
                         :value @show-body
                         :on-change #(swap! show-body not)}]
       [:label {:for "show_box"} "show text"]
       [:div.flex.gap-2.flex-wrap
        (doall (for [[kw thought] (->> @todos
                                       (filter #(if (not (empty? q))
                                                  (str/includes? (str (first %)) q)
                                                  true)))]
                 [:div {:key thought}
                  [:div.align-top [:pre.bg-green-300 (str kw)]]
                  [:div [:pre (if @show-body (str/trim thought))]]]))]
       [:a.bg-blue-100.text-black.py-1.px-1
        {:href "/tdsl/refresh"
         :on-click #(set! js/document.cookie (str"query=" (encoded-string-from-match)))}
        "refresh from git"]])))

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
