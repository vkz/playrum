(ns play.main
  (:require [hiccup.page :refer [doctype html5 include-js]]))

(def css
  (clojure.string/join
   "\n"
   ["* { box-sizing: border-box; vertical-align: top; text-rendering: optimizelegibility; }"
    "tr, td, table, tbody { padding: 0; margin: 0; border-collapse: collapse; }"
    "body, input, button { font: 16px/20px 'Input Sans Narr', 'Input Sans Narrow', sans-serif;}"
    ".example { border: 2px solid #ccc; border-radius: 2px; padding: 20px; width: 300px; display: inline-block; height: 260px; margin: 10px 5px; line-height: 28px; }"
    ".example-title { border-bottom: 1px solid #ccc; margin: -4px 0 20px; line-height: 30px; }"
    "dt { width: 82px; float: left; }"
    "dt, dd { vertical-align: bottom; line-height: 36px }"
    "input { padding: 6px 6px 2px; border: 1px solid #CCC; }"
    "input:focus { outline: 2px solid #a3ccf7; }"
    ".bclock { margin: -6px 0 0 -4px; }"
    ".bclock td, .bclock th { height: 25px; font-size: 12px; font-weight: normal; }"
    ".bclock th { width: 10px; }"
    ".bclock td { width: 25px; border: 4px solid white;  }"
    ".bclock-bit { background-color: #EEE; }"
    ".bclock .stats { text-align: left; padding-left: 8px; }"
    ".artboard { -webkit-user-select: none; line-height: 10px; }"
    ".art-cell { width: 12px; height: 12px; margin: 0 1px 1px 0; display: inline-block; background-color: #EEE; }"
    ".artboard .stats { font-size: 12px; line-height: 14px; margin-top: 8px; }"
    ]))

(defn- page []
  (html5 nil
         [:link {:href "http://cloud.webtype.com/css/34a9dbc8-2766-4967-a61f-35675306f239.css"
                 :rel "stylesheet"
                 :type "text/css"}]
         [:style css]
         [:body [:div#main]]
         ;; All components as well as their respective mount functions are
         ;; defined in client.cljs which compiles to app.js. Once included it
         ;; mounts every component one by one and we are in business.
         (include-js "target/app.js")))

(defn -main [& args]
  (spit "index.html" (page)))
