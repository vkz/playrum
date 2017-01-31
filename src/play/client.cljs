(ns play.client
  (:require [rum.core :as rum]))

(enable-console-print!)

(print "Go out and play!")

(def *clock (atom 0))
(def *color (atom "#FA8D97"))

(defn format-time [ts]
  (-> ts (js/Date.) (.toISOString) (subs 11 23)))

(rum/defc timer-static < rum/static [label ts]
  [:div label ": "
   [:span {:style {:color @*color}} (format-time ts)]])

(rum/defc window < rum/static []
  [:.example
   [:.example-title "Timers"]
   [:#timer-static (timer-static "Static" @*clock)]])

(defn mount []
  (rum/mount (window) (.getElementById js/document "main")))
