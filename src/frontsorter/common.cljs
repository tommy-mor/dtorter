(ns frontsorter.common
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [reagent.core :refer [atom]]))

(defn collapsible-cage [open title cls & children]
  (let [collapsed (atom (not open))]
    (fn [open title cls & children]
      [:div.cageparent
       {:class cls}
       
       [:div.cagetitle
        {:on-click (fn [e] (swap! collapsed not))}
        (if @collapsed
          (str "[+]  " title)
          (str "[  ]  " title))]
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

(defn url-displayer [side]
  (let [format @(subscribe [:url-format])
        [url embedurl] @(subscribe [:urls side])]
    (cond
      ((keyword "any website") format) [:a {:href url
                                            :target "_blank"} url]
      ((keyword "image link") format)  [:img {:src url
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

(defn itemview [side]
  (let [format @(subscribe [:format])
        item @(subscribe [:item side])]
    [:div.item 
     {:class (case side :right "rightitem" :left "leftitem" "")
      :style (when (not (= side :item))
               {:transform (str "translateY(-" @(subscribe [:side-height side]) "px)")})}
     (when (:name format)
       [:h1 {:style {:margin-bottom "4px"}} (:name item)])
     (when (:url format)
       [url-displayer side])
     (when (:paragraph format)
       [:<> 
        [:br]
        [:pre {:style {:color "red"
                       :white-space "pre-line"}} (:paragraph (:content item))]])] ))

(defn smallbutton [text fn & [style]]
  [:a {:on-click fn :style style :class "sideeffect" :href "#"} text])

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
    (let [mag (if (= (:item_a vote) leftid)
                (- 100 (:magnitude vote))
                (:magnitude vote))
          mag2 (- 100 mag)]
      [mag mag2])))


(defn button [text event & {:keys [class] :or {class "button"}}]
  [:div {:class (str "button " class) :on-click #(dispatch [event])} text])

(defn pairvoter [& {:keys [cancelevent]}]
  (let []
    
    [collapsible-cage true "VOTE" "votingaddpanel"
     [:div.votearena
      {:style @(subscribe [:pair-arena-style])}
      
      [itemview :left]
      [itemview :right]
      
      [slider]
      
      [button "submit" :vote]
      
      (when cancelevent
        [button "cancel" :cancelvote :class "cancelbutton"])]] ))


;; not a view, a function for converting attributes dict to a list
(defn attributes-not-db [smaller-db]
  (sort-by val
           (let [{:keys [chosen none current]} smaller-db]
             (merge (when current {(keyword current) 0}) 
                    (if (empty? chosen)
                      {:default none}
                      (if (zero? none)
                        chosen
                        (merge  {:default none}  chosen)))))))
