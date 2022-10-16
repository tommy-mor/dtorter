(ns frontsorter.common
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [reagent.core :refer [atom]]
   [clojure.string :as str]
   [frontsorter.router :as router]))

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

(defn fields-from-format [format]
  (vec (filter identity
               (for [k ["name" "url" "paragraph"]]
                 (if ((keyword k) format)
                   k)))))

(defn youtube-embedurl [fullurl]
  (str/replace fullurl "https://youtube.com/watch?v=" "https://youtube.com/embed/"))

(defn url-displayer [url]
  (let [format true
        embedurl true]
    (comment (cond
               true [:a {:href url
                         :target "_blank"} url]
               ((keyword "image link") format)
               [:img {:src url
                      :style {:max-width "100%"}}]
               
               (or ((keyword "youtube") format)
                   ((keyword "youtube with timestamp") format))
               [:div {:style {:padding-bottom "56.25%"
                              :position "relative"
                              :width "100%"
                              :height 0}} [:iframe {:src embedurl :style {:height "100%"
                                                                          :width "100%"
                                                                          :position "absolute"
                                                                          :top 0
                                                                          :left 0
                                                                          }
                                                    :allow-full-screen true}]]
               ((keyword "spotify") format) [spotify-player embedurl]
               
               true [:span "unknown format"])))
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
  item
  [:div.item {:on-click #(dispatch [::router/navigate
                                    ::router/item-view
                                    {:itemid (:xt/id item)}])}

   [:h1 {:style {:margin-bottom "4px"}} (:item/name item)]
   (when-let [url (:item/url item)]
     [url-displayer url])])

(comment (when (:item/name item))
         (comment (when (:url item)
                    [url-displayer side]))
         (when (:paragraph name)
           [:<> 
            [:br]
            [:pre {:style {:color "red"
                           :white-space "pre-line"}} (:paragraph (:content item))]]))

(defn itemview [side]
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


(defn editable [title is-editable edit-body body]
  (let [edit (atom false)]
    (fn [title is-editable edit-body body]
      [:div.cageparent
       [:div.cagetitle title
        (if is-editable
          [:div.rightcorner {:on-click #(reset! edit true)} "edit"])]
       
       (if @edit [edit-body edit] body)])))

(defn editable-link [title is-editable url body]
  [:div.cageparent.tag
   [:div.cagetitle title
    (if is-editable
      [:div.rightcorner {:on-click #(set! js/window.location.href url)} "edit"])]
   body] )

(defn editpage [stateatom showatom submitfn deletefn]
  
  [:div.votearena
   (into [:<>]
         (for [[attr v] @stateatom]
           ^{:key attr}
           [:input.editinput {:type "text" :value v
                              :on-change #(let [v (-> % .-target .-value)]
                                            (swap! stateatom assoc attr v))
                              :on-key-down #(condp = (.-which %)
                                              13 (submitfn)
                                              nil)}]))
   [smallbutton "submit" submitfn]
   [smallbutton "cancel" #(reset! showatom false)]
   [smallbutton "delete" deletefn {:color "red"}]])


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
    
    [collapsible-cage true "VOTE" "votingaddpanel"
     [:div.votearena
      [itemview :left]
      [itemview :right]
      
      [slider]
      
      [button "submit" :vote]
      
      (when cancelevent
        [button "cancel" :cancelvote :class "cancelbutton"])]] ))


;; not a view, a function for converting attributes dict to a list

