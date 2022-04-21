(ns frontsorter.attributes
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx
                          reg-sub reg-fx
                          subscribe dispatch dispatch-sync]]
   [reagent.core :refer [atom]]
   [frontsorter.common :as c]))

;; inviariants : when no attribute is selected, :current-attribute is null
;; 

(reg-sub :attributes :attributes)
(reg-sub :current-attribute :current-attribute)

;; eventually switch back to using counts, rn just make it work normally

(reg-event-fx :attribute-selected
              (fn [{:keys [db]} [_ attribute]]
                (let [path [:attributes :chosen (keyword attribute)]]
                  {:db (assoc db :current-attribute attribute)
                   :dispatch [:refresh-state [:attributes]]}))) 

(defn attributes-panel []
  (let [editing (atom false)
        new-attr-name (atom "")]
    (fn []
      (let [current-attribute @(subscribe [:current-attribute])
            attributes @(subscribe [:attributes])]
        [:div {:style {:display "flex"}} "you are voting on"
         (if (not @editing)
           [:select
            {:on-change #(let [new-attr (.. % -target -value)]
                           (case new-attr
                             "[add new attribute]" (reset! editing true)
                             (dispatch-sync [:attribute-selected new-attr])))
             :value current-attribute}
            (for [attribute attributes]
              [:option {:value attribute
                        :key attribute} (str (name attribute) " (TODO"  " votes)")])
            [:option {:key "add new"} "[add new attribute]"]]
           
           [:<> [:input {:type "text"

                         :value @new-attr-name
                         :on-change #(reset! new-attr-name (.. % -target -value))
                         :placeholder "default"}]
            [:button
             {:on-click #(do
                           (dispatch-sync [:attribute-selected @new-attr-name])
                           (reset! editing false)
                           (reset! new-attr-name ""))}
             "chose"]])
         "attribute"]))))
