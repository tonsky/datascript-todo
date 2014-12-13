(ns datascript-todo.dom
  (:require
    [clojure.string :as str]))

(defn q [selector]
  (js/document.querySelector selector))

(defn value [el]
  (let [val (.-value el)]
    (when-not (str/blank? val)
      (str/trim val))))

(defn date-value [el]
  (when-let [val (value el)]
    (let [val (js/Date.parse val)]
      (when-not (js/isNaN val)
        val))))

(defn array-value [el]
  (when-let [val (value el)]
    (str/split val #"\s+")))
