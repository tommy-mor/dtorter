(ns frontsorter.tagform.create
  (:require [reagent.dom :as d]
            [reagent.core :as r]
            [frontsorter.common :as c]
            [martian.core :as martian]
            [martian.cljs-http :as martian-http]
            [cljs.core.async :refer [<!]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))


(defn sync-state [atom]
  #(reset! atom (-> % .-target .-value)))

(def m)

(defn new-tag-form [initial-state finish-function]
  (let [state (r/atom initial-state)]
    (fn []
      (let [title (r/cursor state [:tag/name])
            description (r/cursor state [:tag/description])]
        [:div.tagform
         [:input {:type :text :value @title
                  :on-change (sync-state title)
                  :placeholder "title"}]
         [:input {:type :text :value @description
                  :on-change (sync-state description)
                  :placeholder "description"} ]
         [:pre (pr-str @state)]
         [:button {:on-click #(finish-function @state)}
          "submit" ]]))))

(defn submit-tag-form [state]
  (go
    (let [url (<! (martian/response-for m :tag/new
                                        (assoc state :owner js/userid)))]
      (when (:success url)
        (set! js/window.location
              (str "/t/" (-> url :body :xt/id)))))))

(defn new-item-form []
  [:h1 "new item uwu"])

(defn load-martian []
  (go
    (def m (<! (martian-http/bootstrap-openapi (str js/window.location.origin "/api/swagger.json"))))))

(defn ^:export init! []
  (load-martian)
  
  (d/render [:div
             [c/collapsible-cage
              false
              "new item"
              ""
              [new-item-form]]
             [c/collapsible-cage
              false
              "new tag"
              ""
              [new-tag-form {} submit-tag-form ]]] (.getElementById js/document "app")))
