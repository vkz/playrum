(ns play.client
  (:require [rum.core :as rum]))

(enable-console-print!)

(print "Go out and play!")

;;* State
(def *clock (atom 0))
(def *color (atom "#FA8D97"))
(def *speed (atom 500))

;;* Utils
(defn- dom-el [^String id]
  (.getElementById js/document id))

(defn format-time [ts]
  (-> ts (js/Date.) (.toISOString) (subs 11 23)))

(defn tick []
  (reset! *clock (.getTime (js/Date.)))
  (js/setTimeout tick @*speed))

;;* Components
;;** Shared
(defn periodic-refresh [period]
  ;; Custom mixin for updating components on timer
  ;; for cases where you have nothing to subscribe to
  {:did-mount
   (fn [state]
     (let [react-comp (:rum/react-component state)
           interval (js/setInterval #(rum/request-render react-comp) period)]
       (assoc state ::interval interval)))
   :will-unmount
   (fn [state]
     (js/clearInterval (::interval state)))})

(rum/defc watches-count < (periodic-refresh 1000) [ref]
  ;; uses custom mixin to tap into React lifecycle
  [:span (count (.-watches ref))])

;;** timer-static
(rum/defc timer-static < rum/static [label time]
  [:div label ": "
   [:span {:style {:color @*color}} (format-time time)]])

(defn- mount-timer-static [el]
  (rum/mount (timer-static "Static" @*clock) el)
  ;; Setting up watch manually,
  ;; force top-down re-render via mount
  (add-watch *clock :timer-static
             (fn [_ _ _ new-val]
               (rum/mount (timer-static "Static" new-val) el))))

;;** timer-reactive
(rum/defc timer-reactive < rum/reactive []
  [:div "Reactive: "
   [:span {:style {:color (rum/react *color)}}
    (format-time (rum/react *clock))]])

(defn- mount-timer-reactive [el]
  (rum/mount (timer-reactive) el))

;;** controls
(rum/defc input < rum/reactive [ref]
  [:input {:type "text"
           :value (rum/react ref)
           :style {:width 100}
           :on-change #(reset! ref (.. % -target -value))}])

(rum/defc controls []
  [:dl
   [:dt "Color: "] [:dd (input *color)]
   [:dt "Clone: "] [:dd (input *color)]
   [:dt "Color: "] [:dd (watches-count *color) " watches"]
   [:dt "Tick: "] [:dd (input *speed) " ms"]
   [:dt "Time:"] [:dd (watches-count *clock) " watches"]])

(defn- mount-controls [el]
  (rum/mount (controls) el))

;;** binary clock

(def *bclock-renders (atom 0))

(rum/defc render-count < rum/reactive [ref]
  [:div.stats "Renders: " (rum/react ref)])

(rum/defc bit < rum/static [n bit]
  (swap! *bclock-renders inc)
  [:td.bclock-bit {:style (when (bit-test n bit) {:backgroundColor @*color})}])

(rum/defc binary-clock < rum/reactive []
  (let [ts   (rum/react *clock)
        msec (mod ts 1000)
        sec  (mod (quot ts 1000) 60)
        min  (mod (quot ts 60000) 60)
        hour (mod (quot ts 3600000) 24)
        hh   (quot hour 10)
        hl   (mod  hour 10)
        mh   (quot min 10)
        ml   (mod  min 10)
        sh   (quot sec 10)
        sl   (mod  sec 10)
        msh  (quot msec 100)
        msm  (->   msec (quot 10) (mod 10))
        msl  (mod  msec 10)]
    [:table.bclock
     [:tbody
      [:tr [:td]      (bit hl 3) [:th] [:td]      (bit ml 3) [:th] [:td]      (bit sl 3) [:th] (bit msh 3) (bit msm 3) (bit msl 3)]
      [:tr [:td]      (bit hl 2) [:th] (bit mh 2) (bit ml 2) [:th] (bit sh 2) (bit sl 2) [:th] (bit msh 2) (bit msm 2) (bit msl 2)]
      [:tr (bit hh 1) (bit hl 1) [:th] (bit mh 1) (bit ml 1) [:th] (bit sh 1) (bit sl 1) [:th] (bit msh 1) (bit msm 1) (bit msl 1)]
      [:tr (bit hh 0) (bit hl 0) [:th] (bit mh 0) (bit ml 0) [:th] (bit sh 0) (bit sl 0) [:th] (bit msh 0) (bit msm 0) (bit msl 0)]
      [:tr [:th hh]   [:th hl]   [:th] [:th mh]   [:th ml]   [:th] [:th sh]   [:th sl]   [:th] [:th msh]   [:th msm]   [:th msl]]
      [:tr [:th {:col-span 8} (render-count *bclock-renders)]]]]))

(defn- mount-binary-clock [el]
  (rum/mount (binary-clock) el))

;;** artboard

(def ^:const board-width 19)
(def ^:const board-height 10)

(defn- prime? [i]
  (and (>= i 2)
       (empty? (filter #(= 0 (mod i %)) (range 2 i)))))

(defn- initial-board []
  (->> (map prime? (range 0 (* board-width board-height)))
       (partition board-width)
       (mapv vec)))

(def *board (atom (initial-board)))
(def *board-renders (atom 0))

(rum/defc board-stats < rum/reactive [*board *renders]
  [:div.stats
   "Renders: " (rum/react *renders)
   [:br]
   "Board watches: " (watches-count *board)
   [:br]
   "Color watches: " (watches-count *color)])

(rum/defc cell < rum/reactive [x y]
  (swap! *board-renders inc)
  (let [*cursor (rum/cursor-in *board [y x])]
    ;; each cell subscribes to its own cursor inside a board
    ;; note that subscription to color is conditional:
    ;; only if cell is on (@cursor == true),
    ;; this component will be notified on color changes
    [:td.art-cell {:style {:background-color (when (rum/react *cursor) (rum/react *color))}
                   :on-mouse-over (fn [_] (swap! *cursor not) nil)}]))

(rum/defc board-reactive []
  ;; faking board with a table till we add CSS for .artboard .art-cell .stats
  [:div
   [:table
    [:tbody.artboard
     (for [y (range 0 board-height)]
       [:tr.art-row {:key y}
        (for [x (range 0 board-width)]
          ;; this is how one can specify React key for component. React requires
          ;; creating "stable identities" for children in arrays and iterators -
          ;; no idea why.
          (-> (cell x y)
              (rum/with-key [x y])))])]]
   (board-stats *board *board-renders)])

(defn- mount-artboard [el]
  (rum/mount (board-reactive) el))

;;** window
(rum/defc window []
  [:div
   [:.example
    [:.example-title "Timers"]
    [:#timer-static]
    [:#timer-reactive]]
   [:.example
    [:.example-title "Controls"]
    [:#controls]]
   [:.example
    [:.example-title "Binary clock"]
    [:#binary-clock]]
   [:.example
    [:.example-title]
    [:#artboard]]])

(defn mount [el]
  (rum/mount (window) el))

;;* Execute
;;** Mount main window
(mount (dom-el "main"))

;;** Mount components
(mount-timer-static (dom-el "timer-static"))
(mount-timer-reactive (dom-el "timer-reactive"))
(mount-controls (dom-el "controls"))
(mount-binary-clock (dom-el "binary-clock"))
(mount-artboard (dom-el "artboard"))

;;** Start clock
(tick)
