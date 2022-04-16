(ns frontsorter.attributes
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx
                          reg-sub reg-fx
                          subscribe dispatch dispatch-sync]]
   [reagent.core :refer [atom]]
   [frontsorter.common :as c]))

;; inviariants : when no attribute is selected, :current-attribute is null
;; 

(defn attributes [db] (c/attributes-not-db (:attributes db)))

(reg-sub :attributes attributes) 

(defn current-attribute [db]
  (-> db :attributes :current))

(reg-sub :current-attribute current-attribute)

(reg-event-fx :attribute-selected
              (fn [{:keys [db]} [_ attribute]]
                (let [path [:attributes :chosen (keyword attribute)]]
                  {:db (cond-> db
                         true (assoc-in [:attributes :current] attribute)
                         ;; because of lua empty table
                         true (update-in [:attributes :chosen] #(into {} %))
                         
                         (and (not (= "default" attribute))
                              (nil? (get-in db path)))
                         (assoc-in path 0))
                   :dispatch [:refresh-state [:attributes]]})))

(defn attributes-panel []
  (let [editing (atom false)
        new-attr-name (atom "")]
    (fn []
      (let [current-attribute @(subscribe [:current-attribute])
            attributes @(subscribe [:attributes])]
        [:div {:style {:display "flex"}} "you are voting on"
         (if (and (not @editing) (not (= [:default] (keys attributes))))
           [:select
            {:on-change #(let [new-attr (.. % -target -value)]
                           (case new-attr
                             "[add new attribute]" (reset! editing true)
                             (dispatch-sync [:attribute-selected new-attr])))
             :value (or current-attribute "default")}
            (for [[attribute number] attributes]
              [:option {:value attribute
                        :key attribute} (str (name attribute) " (" number " votes)")])
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
