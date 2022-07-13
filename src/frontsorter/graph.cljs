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
  []

  (let [context (.getContext (.getElementById js/document "rev-chartjs") "2d")
        chart-data {:type "scatter"
                    :data {:datasets [{:data []
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
  []
  (create-class
   {:component-did-mount show-revenue-chart
    :display-name        "chartjs-component"
    :reagent-render      (fn []
                           
                           [:canvas {:id "rev-chartjs" :width "700" :height "380"}])
    :key 123}))

(defn render-graph []
  [graph-data])

(defonce x-data (atom false))
(defonce y-data (atom false))

(defn gather-data []
  (def yid->item (into {} (map (juxt :xt/id identity) (-> @y-data))))
  (def xid->item (into {} (map (juxt :xt/id identity) (-> @x-data))))
  (seq (doall (for [id (set/intersection (set (map :xt/id @x-data)) (set (map :xt/id @y-data)))]
                (do
                  {:x (:elo (xid->item id))
                   :y (:elo (yid->item id))
                   :name (:item/name (yid->item id))})))))
(defn attribute-selector [selected-atom data-atom attributes]
  [:select
   {:on-change #(let [new-attr (.. % -target -value)]
                  (reset! selected-atom new-attr)
                  (go (reset! data-atom (-> (martian/response-for m :tag/sorted {:id js/tagid :attribute new-attr})
                                            <!
                                            :body
                                            :tag.filtered/sorted))
                      (js/console.log (clj->js @data-atom))
                      (when (and @chart
                                 @x-data
                                 @y-data)
                        (js/console.log @chart)
                        (set! (.-data @chart)
                              
                              (clj->js {:datasets [{:data (gather-data)
                                                    :label "matchup2"
                                                    :backgroundColor "#90EE90"
                                                    }]}))
                        (. @chart update))))
    :value @selected-atom}

   [:option {:disabled true :value false} "select an option"]
   (for [[attribute number] attributes]
     [:option {:value attribute
               :key attribute}
      (str (name attribute) " (" number " votes)")])])

(defn graph [attrs]
  (let [x-attr (atom false)
        y-attr (atom false)]    
    (fn [attrs]
      #_ (GET (str "/t/" js/tagid "/graph/" @x-attr "/" @y-attr)
              {:handler #(do
                           (reset! data %)
                           (set! (.. @chart -data) (clj->js
                                                    {:datasets [{:data %
                                                                 :label "matchup2"
                                                                 :backgroundColor "#90EE90"}]}))
                           (. @chart update))})
      [:div
       [:a {:style {:background "green"
                    :float "left"}
            :href (str "/t/" js/tagid)}
        "<<back to tag"]
       "x attribute"
       [attribute-selector x-attr x-data attrs]
       "y attribute"
       [attribute-selector y-attr y-data attrs]

       (when (and @x-attr
                  @y-attr
                  (not (= @x-attr @y-attr)))
         (render-graph))])))

(defn mount-root [attrs]
  (d/render [graph attrs] (.getElementById js/document "app")))


(defn ^:export init! [attrs]
  (mount-root (read-string attrs)))

