(ns stickerbomber.app
  (:require [reagent.dom :as rd]))

(defn app []
  [:div "Hello World!"])

(defn ^:after-load reload []
  (rd/render [app]
             (.getElementById js/document "app")))

(defn init! []
  (reload))

(init!)
