(ns frontdsl.todopage
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            
            [frontdsl.page :as page]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [alandipert.storage-atom :refer [local-storage]]
            [tick.core :as t]
            [clojure.contrib.humanize :as humanize]))


(defn collapsible-cage [open title cls & children]
  (let [collapsed (r/atom (not open))]
    (fn [open title cls & children]
      [:div.cageparent
       {:class cls}
       
       [:div.cagetitle
        {:on-click (fn [e] (swap! collapsed not))}
        (if @collapsed
          (str "[+]  " title)
          (str "[–]  " title))]
       (if @collapsed
         nil
         children)])))

(def linear-key "lin_api_LG69pvw2e961XWIg89XhTmlix2GCGd1BAU3o3a8C")
(def todos (r/atom nil))

(defn todo-row [todo]
  (def todo todo)
  [:a {:style {:background-color (:color (:project todo))
               :display "flex"
               :justify-content "space-between"
               :border "1px"
               :border-style "solid"
               :border-color "lightgrey"}
       :href "https://linear.app"}
   [:p.title (:title todo)]
   [:p.title {:style {:margin-left "30px"}}
    (when (:dueDate todo)
      (humanize/duration (* 1000 60 (t/minutes
                                     (t/between (t/instant) (t/at (t/date (:dueDate todo)) "23:59"))))
                         {:number-format str}))]])

(defn tdsl-app [_]
  [:div {:style {:padding-top "30px"}}
   (comment [todo-nest "todos" ["todos"] @todos])
   [:div {:style {:font-size "xx-large"}} [collapsible-cage  true "started" "win"
                                           (doall
                                            (for [todo @todos
                                                  :when (= (:type (:state todo)) "started" )]
                                              [todo-row todo]))]]
   [collapsible-cage  true "unstarted" "win"
    (doall
     (for [todo @todos
           :when (= (:type (:state todo)) "unstarted" )]
       [todo-row todo]))]
   [collapsible-cage  false "backlog/else" "win"
    (doall
     (for [todo @todos
           :when (not (#{"unstarted" "sarted" "completed"} (:type (:state todo))))]
       [todo-row todo]))]
   
   [page/editbox]])

(defn ^:export run [inp]
  (go (reset! todos (-> (<! (http/post "https://api.linear.app/graphql"
                                       {:json-params {:query "{ issues { nodes { id title dueDate project {name color}  state {type name} } }}"}
                                        :oauth-token linear-key
                                        :with-credentials? false}))
                        :body
                        :data
                        :issues
                        :nodes)))
  (reset! page/editbox-state (js->clj inp :keywordize-keys true))
  (rdom/render [tdsl-app] (js/document.getElementById "app")))

