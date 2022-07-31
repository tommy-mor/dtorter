(ns frontdsl.page
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [reitit.frontend :as rf]
            [reitit.core]
            [reitit.frontend.easy :as rfe]

            [frontsorter.common :refer [collapsible-cage]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defonce todos (r/atom {}))

(defonce match (r/atom nil))

(defonce show-body (r/atom true))
(defonce show-edit (r/atom false))

(defonce body-query (r/atom ""))

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

(defn filter-input-body
  []
  [:input.bg-black.text-white.max-width
   {:type "type"
    :value @body-query
    :on-change #(reset! body-query (-> % .-target .-value))
    }])

(defn find-color [kw]
  (let [c (str/join " " (cond-> [] 
                          (str/includes? kw "sorter") (conj "bg-pink-200")
                          (str/includes? kw "tdsl") (conj "bg-blue-300")
                          (str/includes? kw "todo") (conj "font-bold")
                          (str/includes? kw "tf2") (conj "bg-amber-200")))]
    (if (or (empty? c) (#{"font-bold"} c))
      (str c " bg-green-100")
      c)))

(defn expandable-kw [kw body]
  (let [expanded (r/atom false)]
    (fn [kw body]
      [:div
       [:div.align-top {:on-click #(swap! expanded not)}
        [:pre {:class (find-color kw)} (str kw)]]
       (when @expanded
         [:div [:pre (str/trim body)]])])))

(defn tdsl-app []
  (fn []
    (let [q (query-from-match)]
      [:div.max-w-full.max-h-full.overscroll-contain
       [filter-input]
       [:br]
       [filter-input-body]
       [:input#show_box {:type "checkbox"
                         :value @show-body
                         :on-change #(swap! show-body not)}]
       [:label {:for "show_box"} "show text"]
       [:input#edit_box{:type "checkbox"
                         :value @show-edit
                         :on-change #(swap! show-edit not)}]
       
       [:label {:for "edit_box"} "show edit box"]
       [:div.flex.gap-2.flex-wrap
        (doall (for [{:keys [name body] :as thought} 
                     (->> @todos
                          (filter #(if (not (empty? q))
                                     (str/includes? (:name %) q)
                                     true))
                          (filter #(if (not (empty? @body-query))
                                     (str/includes? (:body %) @body-query)
                                     true)))]
                 (if @show-body
                   [:div {:key body}
                    [:div.align-top [:pre {:class (find-color name)} (str name)]]
                    [:div [:pre (str/trim body)]]]
                   [expandable-kw name body])))]
       [:textarea "raartsrast"]
       [:a.bg-blue-100.text-black.py-1.px-1
        {:href "/tdsl/refresh"
         :on-click #(set! js/document.cookie (str"query=" (encoded-string-from-match) "; path=/"))}
        "refresh from git"]
       [:div.bg-blue-100.text-black.py-1.px-1
        {:on-click
         (fn [] (http/post (str "/tdsl/b/" js/dir "/update")
                           {:edn-params @todos}))}
        
        "send things"]])))

(def routes
  [["/"
    {:name ::tdsl
     :view tdsl-app
     :parameters {:query {:q string?}}}]])

(defn ^:export run [inp]
  ;; TODO make this edn so that the keyword values stay keywords. have to escape it like in dtortr
  (reset! todos (js->clj inp :keywordize-keys true))
  (rfe/start!
   (rf/router routes)
   (fn [m] (reset! match m))
   {:use-fragment true})
  (rdom/render [tdsl-app] (js/document.getElementById "app")))
