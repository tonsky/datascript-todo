(ns datascript-todo
  (:require
    [sablono.core :as s :include-macros true]
    [datascript-todo.react :as r :include-macros true]))

(enable-console-print!)

(r/defc canvas [greeting]
  (s/html
    [:#canvas
      "Hello, " greeting]))

(defn ^:export start! []
  (r/render (canvas "stranger") (.-body js/document)))
