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

(defn- mount-timer-static [mount-el]
  (rum/mount (timer-static "Static" @*clock) mount-el)
  ;; Setting up watch manually,
  ;; force top-down re-render via mount
  (add-watch *clock :timer-static
             (fn [_ _ _ new-val]
               (rum/mount (timer-static "Static" new-val) mount-el))))

;;** window
(rum/defc window < rum/static []
  [:.example
   [:.example-title "Timers"]
   [:#timer-static (timer-static "Static" @*clock)]])

(defn mount []
  (rum/mount (window) (dom-el "main")))

;;* Execute
;;** Mount main window
(mount)

;;** Mount components
(mount-timer-static (dom-el "timer-static"))

;;** Start clock
(tick)
