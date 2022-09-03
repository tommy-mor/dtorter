(ns frontdsl.todopage
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            
            [frontdsl.page :as page]
            
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [alandipert.storage-atom :refer [local-storage]]))

(def todos (r/atom {}))
(def collapsed (local-storage (r/atom {}) :collapsed))

(def count-tree)
(defn count-tree [todos]
  (cond
    (seq? todos) (count todos)

    (map? todos) (reduce + (for [[k v] todos]
                             (count-tree v)))))


(count-tree @todos)
(defn todo [m]
  [:pre {:key (str (:name m)
                   (:file m))}
   (:task m)])

(defn todo-nest [title path m]
  (def path path)
  (def title title)
  
  (if (nil? (get-in @collapsed path))
    (swap! collapsed assoc-in path {}))
  
  (let [collapsed (r/cursor collapsed path)]
    (fn [title path m]
      [:div {:key title}
       [:div {:style {:display "flex"}}
        [:div {:style {:padding-right "10px"
                       :cursor "pointer"}
               :on-click #(reset! collapsed (if (not (empty? @collapsed))
                                              {}
                                              (into {}
                                                    (cond
                                                      (map? m)
                                                      (for [[k v] m]
                                                        [k {}])
                                                      
                                                      (seq? m)
                                                      [[:not-empty :not-empty]]))) )}
         (if (empty? @collapsed)
           "[+]"
           "[-]")]
        
        [:pre (str title " (" (count-tree m) ")")]]
       (when (not (empty? @collapsed))
         [:<>
          [:div {:style {:margin-left "30px"}}
           (cond
             (map? m)
             (doall (for [[k v] m]
                      [todo-nest k (conj path k) v]))
             
             (seq? m)
             (map todo m))]])])))

(defn tdsl-app [_]
  [:div {:style {:padding-top "30px"}}
   [todo-nest "todos" ["todos"] @todos]
   [page/editbox]])

(defn ^:export run [inp]
  (def todo-url (str "/tdsl/b/" js/dir "/todo"))
  (go (reset! todos (:body (<! (http/get todo-url {:accept "application/edn"})))))
  (reset! page/editbox-state (js->clj inp :keywordize-keys true))
  (rdom/render [tdsl-app] (js/document.getElementById "app")))

