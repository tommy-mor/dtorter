(ns frontsorter.graph
  (:require
   ["chart.js"]
   [reagent.core :refer [atom create-class]]
   [reagent.dom :as d]
   [frontsorter.common :as c]
   [ajax.core :as ajax :refer [GET]]))

(def height 850)
(def width 850)

(def chart (atom nil))

(defn show-revenue-chart
  [data]
  (let [context (.getContext (.getElementById js/document "rev-chartjs") "2d")
        chart-data {:type "scatter"
                    :data {:datasets [{:data data
                                       :label "matchup"
                                       :backgroundColor "#90EE90"}]}
                    :options {:interaction {:mode "nearest"}
                              :plugins {:tooltip
                                        {:callbacks
                                         {:label
                                          #(.. % -raw -name)}}}}}
        chart-data (clj->js chart-data)]
    (reset! chart (js/Chart. context chart-data))))

(defn graph-data
  [data]
  (create-class
   {:component-did-mount #(show-revenue-chart data)
    :display-name        "chartjs-component"
    :reagent-render      (fn []
                           
                           [:canvas {:id "rev-chartjs" :width "700" :height "380"}])
    :key data}))

(defn render-graph [data]
  [graph-data @data])

(defn attribute-selector [selected-atom attributes]
  [:select
   {:on-change #(let [new-attr (.. % -target -value)]
                  (reset! selected-atom new-attr))
    :value @selected-atom}

   [:option {:disabled true :value false} "select an option"]
   (for [[attribute number] attributes]
     [:option {:value attribute
               :key attribute}
      (str (name attribute) " (" number " votes)")])])


(defn graph []
  (let [x-attr (atom false)
        y-attr (atom false)
        attrs (c/attributes-not-db (js->clj js/attributes :keywordize-keys true))
        data (atom nil)]    
    (fn []
      (GET (str "/t/" js/tagid "/graph/" @x-attr "/" @y-attr)
           {:handler #(do
                        (reset! data %)
                        (set! (.. @chart -data) (clj->js
                                                 {:datasets [{:data %
                                                              :label "matchup2"
                                                              :backgroundColor "#90EE90"}]}))
                        (. @chart update))})
      [:div
       "x attribute"
       [attribute-selector x-attr attrs]
       "y attribute"
       [attribute-selector y-attr attrs]

       (when (and @x-attr
                  @y-attr
                  (not (= @x-attr @y-attr)))
         [render-graph data])])))

(defn mount-root []
  (d/render [graph] (.getElementById js/document "app")))


(defn ^:export init! []
  (mount-root))
