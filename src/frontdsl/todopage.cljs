(ns frontdsl.todopage
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            
            [frontdsl.page :as page]
            
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]))

(def todos (r/atom {}))

(def count-tree)
(defn count-tree [todos]
  (cond
    (seq? todos) (count todos)

    (map? todos) (reduce + (for [[k v] todos]
                             (count-tree v)))))


(count-tree @todos)
(defn todo [m]
  [:pre (pr-str m)])

(defn todo-nest [k m]
  (let [collapsed (r/atom true)]
    (fn [k m]
      [:div
       [:div {:style {:display "flex"}}
        [:div {:style {:padding-right "10px"
                       :cursor "pointer"}
               :on-click #(swap! collapsed not)}
         (if @collapsed
           "[+]"
           "[-]")]
        
        [:pre (str k " (" (count-tree m) ")")]]
       (when (not @collapsed)
         [:<>
          [:div {:style {:margin-left "30px"}}
           (cond
             (map? m)
             (doall (for [[k v] m]
                      [todo-nest k v]))
             
             (seq? m)
             (map todo m))]])])))

(defn tdsl-app [_]
  [:div {:style {:padding-top "30px"}}
   [todo-nest "todos" @todos]
   [page/editbox]])

(defn ^:export run [inp]
  (def todo-url (str "/tdsl/b/" js/dir "/todo"))
  (go (reset! todos (:body (<! (http/get todo-url {:accept "application/edn"})))))
  (reset! page/editbox-state (js->clj inp :keywordize-keys true))
  (rdom/render [tdsl-app] (js/document.getElementById "app")))

