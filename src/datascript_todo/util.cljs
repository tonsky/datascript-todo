(ns datascript-todo.util
  (:require
    [datascript :as d]))

(defn remove-vals [f m]
  (reduce-kv (fn [m k v] (if (f v) m (assoc m k v))) (empty m) m))

(defn e-by-av [db a v]
  (-> (d/datoms db :avet a v) first :e))

(defn v-by-ea [db e a]
  (-> (d/datoms db :eavt e a) first :v))

(defn date->month [date]
  [(.getFullYear date)
   (inc (.getMonth date))])

(defn format-month [month year]
  (str (get ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"] (dec month))
       " " (rem year 100)))

(defn month-start [month year]
  (js/Date. (str year "-" month "-01")))

(defn month-end [month year]
  (let [[month year] (if (< month 12)
                       [(inc month) year]
                       [01 (inc year)])]
    (-> (str year "-" month "-01 00:00:00")
        js/Date.
        dec
        js/Date.
        )))
