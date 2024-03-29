(ns frontsorter.attributes
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx
                          reg-sub reg-fx
                          subscribe dispatch dispatch-sync]]
   [reagent.core :refer [atom]]
   [frontsorter.common :as c]
   [frontsorter.events :as events]
   [frontsorter.router :as router]))

(reg-sub :current-attribute
         :<- [:current-route]
         (some-fn (comp :attribute :query-params)
                  (constantly ::empty)))

(reg-sub :raw-attributes
         :<- [:tag]
         :interface/attributes)

(reg-sub :attributes
         :<- [:current-attribute]
         :<- [:raw-attributes]
         (fn [[ca attrs] _]
           (if (contains? attrs ca)
             attrs
             (merge attrs {ca 0}))))



;; eventually switch back to using counts, rn just make it work normally
;; make attribute counts a separate array, same length

(reg-event-fx :attribute-selected
              events/interceptor-chain
              (fn [{:keys [db]} [_ attribute]]
                {:dispatch [::router/navigate
                            (-> db :current-route :data :name)
                            (-> db :current-route :path-params)
                            (merge
                             (-> db :current-route :query-params)
                             {:attribute attribute})]}))

(defn attributes-panel []
  (let [editing (atom false)
        new-attr-name (atom "")]
    (fn []
      (let [current-attribute @(subscribe [:current-attribute])
            attributes @(subscribe [:attributes])]
        [:div {:style {:display "flex"}} "you are voting on"
         (if (and (not (empty? attributes))
                  (not @editing)
                  (not (#{:interface.filter/no-attribute} current-attribute)))
           [:select
            {:on-change #(let [new-attr (.. % -target -value)]
                           (case new-attr
                             "[add new attribute]" (reset! editing true)
                             (dispatch-sync [:attribute-selected new-attr])))
             :value current-attribute}
            (for [[attribute attributecount] (sort-by second attributes)]
              [:option {:value attribute
                        :key attribute} (str (name attribute) " (" attributecount " votes)")])
            [:option {:key "add new"} "[add new attribute]"]]
           
           [:<> [:input {:type "text"

                         :value @new-attr-name
                         :on-change #(reset! new-attr-name (.. % -target -value))
                         :placeholder "type attribute here"}]
            [:button.btn
             {:on-click #(do
                           (dispatch-sync [:attribute-selected @new-attr-name])
                           (reset! editing false)
                           (reset! new-attr-name ""))}
             "chose"]])
         "attribute"]))))





