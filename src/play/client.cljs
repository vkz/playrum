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

;;** window
(rum/defc window []
  [:div.example
   [:div.example-title "Timers"]
   [:div#timer-static]
   [:div#timer-reactive]])

(defn mount [el]
  (rum/mount (window) el))

;;* Execute
;;** Mount main window
(mount (dom-el "main"))

;;** Mount components
(mount-timer-static (dom-el "timer-static"))
(mount-timer-reactive (dom-el "timer-reactive"))

;;** Start clock
(tick)
