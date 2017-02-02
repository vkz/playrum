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

;;** bmi-calculator

(def *bmi-data (atom {:height 180
                      :weight 80}))

(defn calc-bmi [{:keys [height weight bmi] :as data}]
  (let [h (/ height 100)]
    (if (nil? bmi)
      (assoc data :bmi (/ weight (* h h)))
      (assoc data :weight (* bmi h h)))))

(defn slider [param value min max]
  (let [reset (case param
                :bmi :weight
                :bmi)]
    [:input {:type "range"
             :value (int value)
             :min min
             :max max
             :style {:width "100%"}
             :on-change #(swap! *bmi-data assoc
                                param (-> % .-target .-value)
                                reset nil)}]))

(rum/defc bmi-calculator < rum/reactive []
  (let [{:keys [weight height bmi] :as data} (calc-bmi (rum/react *bmi-data))
        [color diagnose] (cond
                           (< bmi 18.5) ["orange" "underweight"]
                           (< bmi 25) ["inherit" "normal"]
                           (< bmi 30) ["orange" "overweight"]
                           :else ["red" "obese"])]
    (reset! *bmi-data data)
    [:div.bmi
     [:div
      "Height: " (int height) "cm"
      (slider :height height 100 220)]
     [:div
      "Weight: " (int weight) "kg"
      (slider :weight weight 30 150)]
     [:div
      "BMI: " (int bmi) " "
      [:span {:style {:color color}} diagnose]
      (slider :bmi bmi 10 50)]]))

(defn- mount-bmi-calculator [el]
  (rum/mount (bmi-calculator) el))

;;** form-validation

(rum/defc validated-input < rum/reactive [ref f]
  [:input {:type "text"
           :style {:width 170
                   :background-color (when-not (f (rum/react ref))
                                       (rum/react *color))}
           :value (rum/react ref)
           :on-change #(reset! ref (.. % -target -value))}])

(rum/defcc restricted-input < rum/reactive [comp ref f]
  [:input {:type "text"
           :style {:width 170}
           :value (rum/react ref)
           :on-change #(let [new-val (.. % -target -value)]
                         (if (f new-val)
                           (reset! ref new-val)
                           ;; request-render is mandatory because sablono :input
                           ;; keeps current value in input’s state and always
                           ;; applies changes to it
                           (rum/request-render comp)))}])

(rum/defcs restricted-input-native < rum/reactive [state ref f]
  (let [comp (:rum/react-component state)]
    (js/React.createElement
     "input"
     #js {:type "text"
          :style #js {:width 170}
          :value (rum/react ref)
          :onChange #(let [new-val (.. % -target -value)]
                       (when (f new-val)
                         (reset! ref new-val)
                         ;; need forceUpdate here because otherwise rendering
                         ;; will be delayed until requestAnimationFrame and that
                         ;; breaks cursor position inside input
                         (.forceUpdate comp)))})))

(rum/defc form-validation []
  (let [state (atom {:email "joe@example.com"
                     :phone "+7916 810 0356"
                     :age "35"})]
    [:dl
     [:dt "Email: "] [:dd (validated-input
                           (rum/cursor state :email)
                           #(re-matches #"[^@]+@[^@.]+\..+" %))]
     [:dt "Phone: "] [:dd (restricted-input-native
                           (rum/cursor state :phone)
                           #(re-matches #"[0-9\- +()]*" %))]
     [:dt "Age: "] [:dd (restricted-input
                         (rum/cursor state :age)
                         #(re-matches #"([1-9][0-9]*)?" %))]]))

(defn- mount-form-validation [el]
  (rum/mount (form-validation) el))

;;** inputs

(def values (range 1 5 1))

(rum/defc input-text < rum/reactive [*ref]
  [:input {:type "text"
           :style {:width 170}
           :value (rum/react *ref)
           :on-change (fn [e]
                        (reset! *ref (long (.. e -currentTarget -value))))}])

(defn- next-value [v]
  (let [vv (rand-nth values)]
    (if (= v vv)
      (recur v)
      vv)))

(rum/defc shuffle-button [*ref]
  [:button
   {:on-click #(swap! *ref next-value)}
   "Next value"])

(rum/defc input-value < rum/reactive [*ref]
  [:code (pr-str (rum/react *ref))])

(rum/defc input-checkbox < rum/reactive [*ref]
  (let [value (rum/react *ref)]
    [:dive
     (for [v values]
       [:input {:type "checkbox"
                :name "inputs_checkbox"
                :checked (= v value)
                :value v
                :key v
                :on-click (fn [_] (reset! *ref v))}])]))

(rum/defc input-radio < rum/reactive [*ref]
  (let [value (rum/react *ref)]
    [:div
     (for [v values]
       [:input {:type "radio"
                :name "inputs_radio"
                :checked (= v value)
                :key v
                :value v
                :on-click (fn [_] (reset! *ref v))}])]))

(rum/defc input-select < rum/reactive [*ref]
  (let [value (rum/react *ref)]
    [:select {:value value
              :on-change (fn [e]
                           (reset! *ref (long (.. e -currentTarget -value))))}
     (for [v values]
       [:option {:value v :key v} v])]))

(rum/defc inputs []
  (let [*ref (atom 1)]
    [:dl
     [:dt "Input"] [:dd (input-text *ref)]
     [:dt "Checks"] [:dd (input-checkbox *ref)]
     [:dt "Radio"] [:dd (input-radio *ref)]
     [:dt "Select"] [:dd (input-select *ref)]
     [:dt (input-value *ref)] [:dd (shuffle-button *ref)]]))

(defn- mount-inputs [el]
  (rum/mount (inputs) el))

;;** refs

(rum/defcc ta < {:after-render
                 (fn [state]
                   (let [ta (rum/ref-node state "ta")]
                     (set! (.-height (.-style ta)) "0")
                     (set! (.-height (.-style ta))
                           (str (+ 2 (.-scrollHeight ta)) "px")))
                   ;; mixins must return state, else you get weird errors
                   state)}
  [comp]
  [:textarea
   {:ref :ta
    :style {:width "100%"
            :padding "10px"
            :font "inherit"
            :outline "none"
            :resize "none"}
    :default-value "Auto-resizing\ntextarea"
    :placeholder "Auto-resizing textarea"
    :on-change (fn [_] (rum/request-render comp))}])

(rum/defc refs []
  [:div
   (ta)])

(defn- mount-refs [el]
  (rum/mount (refs) el))

;;** local-state

(comment

  ;; Alternative solution:
  ;; Close over state and pass it over to the subcomponent. We pretty much
  ;; fake "local state" this way without actually using React's local state.

  (rum/defc clicker < rum/reactive [*ref title]
    [:div {:style {"-webkit-user-select" "none"
                   "cursor" "pointer"}
           :on-click (fn [_] (swap! *ref inc))}
     title ": " (rum/react *ref)])

  (rum/defc local-state [title]
    (let [*count (atom 0)]
      [:div (clicker *count title)]))
  ;; end
  )

(rum/defcs local-state < (rum/local 0 ::key)
  [state title]
  (let [*count (::key state)]
    [:div
     {:style {"-webkit-user-select" "none"
              "cursor" "pointer"}
      :on-click (fn [_] (swap! *count inc))}
     title ": " @*count]))

(defn- mount-local-state [el]
  (rum/mount (local-state "Clicks count") el))

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
    [:.example-title "Artboard"]
    [:#artboard]]
   [:.example
    [:.example-title "BMI Calculator"]
    [:#bmi]]
   [:.example
    [:.example-title "Form Validation"]
    [:#form-validation]]
   [:.example
    [:.example-title "Inputs"]
    [:#inputs]]
   [:.example
    [:.example-title "Refs"]
    [:#refs]]
   [:.example
    [:.example-title "Local state"]
    [:#local-state]]])

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
(mount-bmi-calculator (dom-el "bmi"))
(mount-form-validation (dom-el "form-validation"))
(mount-inputs (dom-el "inputs"))
(mount-refs (dom-el "refs"))
(mount-local-state (dom-el "local-state"))

;;** Start clock
(tick)
