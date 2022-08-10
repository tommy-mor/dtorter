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
            [cljs.core.async :refer [<!]]
            [tick.core :as t]))

(defonce todos (r/atom {}))
(defonce match (r/atom nil))

(defonce show-body (r/atom true))
(defonce body-query (r/atom ""))

(defonce editbox-state (r/atom {:name ""
                                :body ""
                                :file ""
                                :position 0}))

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

(defn expandable-kw [{:keys [name body file] :as note}]
  (let [expanded (r/atom false)]
    (fn [{:keys [name body file] :as note}]
      (let [show (or @expanded @show-body)]
        [:div
         [:div.align-top {:on-click #(swap! expanded not)
                          :class (find-color name)}
          [:pre (str name) (when show
                             [:div {:href "#"
                                    :style {:color "blue"
                                            :float "right"
                                            :cursor "pointer"}
                                    :on-click #(do
                                                 (.stopPropagation %)
                                                 (reset! editbox-state note)
                                                 false)} "e"])]]
         (when show
           [:div [:pre (str/trim body)]])]))))

(defonce last-send (r/atom (t/now)))
(defonce pending (r/atom false))
(defonce updated (r/atom true))
;; IDK what to do with this... its not great.
;; maybe have server return what it got, so state knows if it is synced? or returns hash?
;; keeps bouncing back and forth until hash matches. but bounces can only happen max once every 3 seconds..

(def editbox
  (r/create-class
   (let [httpfn (fn []
                  (http/post (str "/tdsl/b/" js/dir "/update")
                             {:edn-params [(update @editbox-state :name keyword)]}))
         sendfn (fn sendfn []
                  (reset! updated false)
                  (if (and (not @pending)
                           (t/> (t/now) (t/>> @last-send (t/new-duration 1 :seconds))))
                    (go 
                      (let [response (httpfn)]
                        (reset! last-send (t/now))
                        (reset! updated true)
                        (js/console.log "response")
                        (js/console.log response)))
                    (when (not @pending)
                      (reset! pending true)
                      (js/setTimeout #(do
                                        (go (httpfn)
                                            (reset! last-send (t/now))
                                            (reset! updated true)
                                            (reset! pending false)))
                                     3000))))
         submitfn (fn []
                    (httpfn)
                    (reset! editbox-state {:name ""
                                           :body ""
                                           :position 0
                                           :file ""}))]
     {:reagent-render
      (fn [e]
        [:div.w-full
         [:input.border-4.w-full {:value (-> @editbox-state :name)
                                  :class (find-color (-> @editbox-state :name))}]
         [:textarea#editbox.border-4.w-full {:value (-> @editbox-state :body)
                                             :on-change
                                             (fn [e]
                                               (swap! editbox-state
                                                      assoc :body (.. e -target -value))
                                               (sendfn))
                                             :on-key-down
                                             (fn [e]
                                               (def e e)
                                               (when (and (.. e -ctrlKey)
                                                          (= (.. e -code)
                                                             "Enter"))
                                                 (sendfn)
                                                 (submitfn)))}]
         [:pre {:style {:color (if @updated "green" "red")}}
          @last-send]
         [:pre
          (if @pending "PENDING" "not pending")
          "\n"
          (if @updated "UPDATED" "not updated") 
          ]])
      :component-did-update
      (fn []
        (let [box (js/document.getElementById "editbox")]
          (set! (.. box -style -height)
                (str (+ (.. box -scrollHeight) 8) "px"))))})))

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
       [:div.flex.gap-2.flex-wrap
        [editbox]
        (doall (for [{:keys [name body file] :as thought} 
                     (->> @todos
                          (filter #(if (not (empty? q))
                                     (str/includes? (:name %) q)
                                     true))
                          (filter #(if (not (empty? @body-query))
                                     (str/includes? (:body %) @body-query)
                                     true))
                          (filter #(not= (select-keys % [:file :position])
                                         (select-keys @editbox-state [:file :position]))))]
                 [expandable-kw thought]))]
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
