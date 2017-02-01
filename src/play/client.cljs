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

;;** window
(rum/defc window []
  [:.example
   [:.example-title "Timers"]
   [:#timer-static]
   [:#timer-reactive]
   [:#controls]])

(defn mount [el]
  (rum/mount (window) el))

;;* Execute
;;** Mount main window
(mount (dom-el "main"))

;;** Mount components
(mount-timer-static (dom-el "timer-static"))
(mount-timer-reactive (dom-el "timer-reactive"))
(mount-controls (dom-el "controls"))

;;** Start clock
(tick)
