(ns frontsorter.graph
  (:require
   ["chart.js"]
   [reagent.core :refer [atom create-class]]
   [reagent.dom :as d]
   [frontsorter.common :as c]
   [cljs.reader :refer [read-string]]
   [martian.core :as martian]
   [martian.cljs-http :as martian-http]
   [cljs.core.async :refer [go <!]]
   [clojure.set :as set]))

(go (def m (<! (martian-http/bootstrap-openapi (str js/window.location.origin "/api/swagger.json")))))
(def height 850)
(def width 850)

(def chart (atom nil))

(defn show-revenue-chart
  [data]
  (def data data)
  data
  (let [context (.getContext (.getElementById js/document "rev-chartjs") "2d")
        chart-data {:type "scatter"
                    :data {:datasets [{:data (vec data)
                                       :label "matchup"
                                       :backgroundColor "#90EE90"}]}
                    :options {:interaction {:mode "nearest"}
                              :plugins {:tooltip
                                        {:callbacks
                                         {:label
                                          #(.. % -raw -name)}}}
                              :scales {:x {:type "linear"
                                           :position "bottom"
                                           :min 0
                                           :max 100}
                                       :y {:type "linear"
                                           :min 0
                                           :max 100}}}}
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
  [graph-data data])

(defn attribute-selector [selected-atom data-atom attributes]
  [:select
   {:on-change #(let [new-attr (.. % -target -value)]
                  (reset! selected-atom new-attr)
                  (go (reset! data-atom (-> (martian/response-for m :tag/sorted {:id js/tagid :attribute new-attr})
                                            <!
                                            :body
                                            :tag.filtered/sorted))
                      (when @chart
                        (js/console.log @chart)
                        (set! (.-data @chart)
                              
                              (clj->js {:datasets [{:data [{:x 3 :y 4}]
                                                    :label "matchup2"
                                                    :backgroundColor "#90EE90"
                                                    }]})))))
    :value @selected-atom}

   [:option {:disabled true :value false} "select an option"]
   (for [[attribute number] attributes]
     [:option {:value attribute
               :key attribute}
      (str (name attribute) " (" number " votes)")])])


(def x-data (atom []))
(def y-data (atom []))

(defn gather-data []
  (def yid->item (into {} (map (juxt :xt/id identity) (-> @y-data))))
  (def xid->item (into {} (map (juxt :xt/id identity) (-> @x-data))))
  (doall (for [id (set/intersection (set (map :xt/id @x-data)) (set (map :xt/id @y-data)))]
           (do
             {:x (:elo (xid->item id))
              :y (:elo (yid->item id))
              #_:name #_(:item/name (yid->item id))}))))

(defn graph [attrs]
  (let [x-attr (atom false)
        y-attr (atom false)]    
    (fn [attrs]
      (js/console.log (clj->js data))
      
      #_ (GET (str "/t/" js/tagid "/graph/" @x-attr "/" @y-attr)
              {:handler #(do
                           (reset! data %)
                           (set! (.. @chart -data) (clj->js
                                                    {:datasets [{:data %
                                                                 :label "matchup2"
                                                                 :backgroundColor "#90EE90"}]}))
                           (. @chart update))})
      [:div
       "x attribute"
       [attribute-selector x-attr x-data attrs]
       "y attribute"
       [attribute-selector y-attr y-data attrs]

       (when (and @x-attr
                  @y-attr
                  (not (= @x-attr @y-attr)))
         [render-graph (gather-data)])])
    ))

(defn mount-root [attrs]
  (d/render [graph attrs] (.getElementById js/document "app")))


(defn ^:export init! [attrs]
  (mount-root (read-string attrs)))

