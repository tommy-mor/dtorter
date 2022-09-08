(ns frontdsl.todopage
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            
            [frontdsl.page :as page]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [alandipert.storage-atom :refer [local-storage]]))

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
  [:div {:style {:background-color "aliceblue"
                 :margin "10px"}}
   [:pre (:task m)]])

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

(def linear-key "lin_api_LG69pvw2e961XWIg89XhTmlix2GCGd1BAU3o3a8C")
(def todos (r/atom nil))

(go (reset! todos (-> (<! (http/post "https://api.linear.app/graphql"
                                  {:json-params {:query "{ issues { nodes { id title dueDate project {name color}  state {type name} } }}"}
                                   :oauth-token linear-key
                                   :with-credentials? false}))
                      :body
                      :data
                      :issues
                      :nodes)))

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
   [:p.title (:dueDate todo)]
   ])

(distinct (map :state @todos))
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
           :when (not (#{"unstarted" "sarted"} (:type (:state todo))))]
       [todo-row todo]))]
   
   [page/editbox]])

(defn ^:export run [inp]
  (def todo-url (str "/tdsl/b/" js/dir "/todo"))
  (go (reset! todos (:body (<! (http/get todo-url {:accept "application/edn"})))))
  (reset! page/editbox-state (js->clj inp :keywordize-keys true))
  (rdom/render [tdsl-app] (js/document.getElementById "app")))

