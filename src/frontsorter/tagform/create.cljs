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

(defn keybinds [finish]
  #(when (and (= (.-keyCode %) 13)
              (.-ctrlKey %))
     (finish)))

(defn new-tag-form [initial-state finish-function delete-function]
  (let [state (r/atom initial-state)]
    (fn []
      (let [title (r/cursor state [:tag/name])
            description (r/cursor state [:tag/description])]
        [:div.tagform
         [:input {:type :text :value @title
                  :on-change (sync-state title)
                  :on-key-down (keybinds #(finish-function @state))
                  :placeholder "title"}]
         [:br]
         [:input {:type :text :value @description
                  :on-change (sync-state description)
                  :on-key-down (keybinds #(finish-function @state))
                  :placeholder "description"} ]
         [:br]
         
         [:button {:on-click #(finish-function @state)}
          "submit" ]
         [:button {:style {:color "red"
                           :float "right"}
                   :on-click #(when (js/confirm "are you sure?")
                                  (delete-function @state))}
          "delete" ]]))))




