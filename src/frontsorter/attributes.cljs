(ns frontsorter.attributes
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx
                          reg-sub reg-fx
                          subscribe dispatch dispatch-sync]]
   [reagent.core :refer [atom]]
   [frontsorter.common :as c]
   [frontsorter.events :as events]))

(reg-sub :current-attribute (some-fn :current-attribute
                                     (constantly ::empty)))

(reg-sub :raw-attributes :attributes)
(reg-sub :attributecounts :attributecounts)
(reg-sub :attributes
         :<- [:current-attribute]
         :<- [:raw-attributes]
         :<- [:attributecounts]
         (fn [[ca attrs counts] _]
           (if (or (contains? (set attrs) ca)
                   (#{::empty} ca))
             [attrs counts]
             [(conj attrs ca)
              (conj counts 0)])))

;; eventually switch back to using counts, rn just make it work normally
;; make attribute counts a separate array, same length

(reg-event-fx :attribute-selected
              events/interceptor-chain
              (fn [{:keys [db]} [_ attribute]]
                {:db (assoc db :current-attribute attribute)
                 :dispatch [:refresh-state]}))

@(subscribe [:current-attribute])
@(subscribe [:raw-attributes])
@(subscribe [:attributes])

(defn attributes-panel []
  (let [editing (atom false)
        new-attr-name (atom "")]
    (fn []
      (let [current-attribute @(subscribe [:current-attribute])
            [attributes attributecounts] @(subscribe [:attributes])]
        [:div {:style {:display "flex"}} "you are voting on"
         (if (and (not (empty? attributes))
                  (not @editing))
           [:select
            {:on-change #(let [new-attr (.. % -target -value)]
                           (case new-attr
                             "[add new attribute]" (reset! editing true)
                             (dispatch-sync [:attribute-selected new-attr])))
             :value current-attribute}
            (for [[attribute attributecount] (reverse (map list attributes attributecounts))]
              [:option {:value attribute
                        :key attribute} (str (name attribute) " (" attributecount " votes)")])
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





