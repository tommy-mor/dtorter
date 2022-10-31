(ns frontsorter.common
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [reagent.core :refer [atom]]
   [clojure.string :as str]
   [frontsorter.router :as router]))

(defn render-tag [tag]
  (def tag tag)
  (def size (Math/sqrt (* 10 (+ (:item/_tags tag) (:vote/_tag tag)))))
  [:div.tag-small
   {:style {:font-size (max size 12)
            :height "fit-content"}}
   [:a {:on-click #(dispatch [::router/navigate ::router/tag-view {:id (:xt/id tag)}])}
    (:tag/name tag)]])

(defn collapsible-cage [open title cls & children]
  (let [collapsed (atom (not open))]
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

(defn spotify-player [url]
  [:iframe {:src url
            :width 300 :height 80
            :allowtransparency "true" :allow "encrypted-media"}])

(defn youtube-embedurl [fullurl]
  (str/replace fullurl "https://youtube.com/watch?v=" "https://youtube.com/embed/"))

(defn url-displayer [url]
  (let [format true
        embedurl true])
  (cond
    (str/starts-with? url "https://youtube.com")
    [:div {:style {:padding-bottom "56.25%"
                   :position "relative"
                   :width "100%"
                   :height 0}}
     [:iframe {:src (youtube-embedurl url) :style {:height "100%"
                                                   :width "100%"
                                                   :position "absolute"
                                                   :top 0
                                                   :left 0
                                                   }
               :allow-full-screen true}]]))

(defn itempanel [item]
  (def item item)
  [:div {:on-click #(dispatch [::router/navigate
                                    ::router/item-view
                                    {:itemid (:xt/id item)}])}

   [:h1 {:style {:margin-bottom "4px"}} (:item/name item)]
   (when-let [url (:item/url item)]
     [url-displayer url])])

(defn item-in-vote [side]
  (let [item @(subscribe [side])]
    [:div.item 
     {:class (case side :right "rightitem" :left "leftitem" "")
      :style (when (not (= side :item))
               {:transform (str "translateY(-" @(subscribe [:side-height side]) "px)")})}
     
     [itempanel item]]))

(defn smallbutton [text fn & [style]]
  [:a {:on-click #(do (.preventDefault %)
                      (fn))
       :style {:cursor "pointer"} :class "sideeffect"} text])

(defn hoveritem [keys & children]
  (let [hovered (atom false)]
    (fn [keys & children]
      [:tr
       (merge keys 
              {
               :on-mouse-over (fn [] (reset! hovered true))
               :on-mouse-out (fn [] (reset! hovered false))
               ;; :key TODO
               :class (if @hovered "item hovered" "item")
               })
       children])))


;; slider stuff
(defn slider []
  (let [percent @(subscribe [:percent])]
    [:input.slider {:type "range" :value percent :min 0 :max 100
                    :on-change (fn [e]
                                 (let [new-value (js/parseInt (.. e -target -value))]
                                   (dispatch-sync [:slide new-value])
                                   ;; if this doesn't work again, make it 
                                   ))}]))
(defn calcmag [vote leftid]
  (if (not vote)
    [50 50]
    (let [mag (if (= (-> vote :vote/left-item :xt/id) leftid)
                (- 100 (:vote/magnitude vote))
                (:vote/magnitude vote))
          mag2 (- 100 mag)]
      [mag mag2])))

(defn mini-slider [perc]
  [:div.mini-slider
   [:div.mini-slider-box {:style {:margin-left (str (- perc 10) "%")}}]])


(defn button [text event & {:keys [class] :or {class "button"}}]
  [:div {:class (str "button " class) :on-click #(dispatch [event])} text])

(defn pairvoter [& {:keys [cancelevent]}]
  (let []
    
    [:div.votearena.row
     [:div.col
      [item-in-vote :left]]
     [:div.col
      [item-in-vote :right]]
     
     [:div.row
      [:div.col [slider]]]
     
     [:div.row
      [:div.col [:div.btn.btn-primary {:on-click #(dispatch [:vote])}
                 "submit"]]]
     
     ;; TODO
     (when cancelevent
       [button "cancel" :cancelvote :class "cancelbutton"])] ))


;; not a view, a function for converting attributes dict to a list

