(ns play.main
  (:require [hiccup.page :refer [doctype html5 include-js]]))

(defn- page []
  (html5 nil
   [:body [:div#main]]
   (include-js "target/app.js")))

(defn -main [& args]
  (spit "index.html" (page)))
