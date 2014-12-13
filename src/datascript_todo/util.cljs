(ns datascript-todo.util
  (:require
    [datascript :as d]))

(defn remove-vals [f m]
  (reduce-kv (fn [m k v] (if (f v) m (assoc m k v))) (empty m) m))

(defn e-by-av [db a v]
  (-> (d/datoms db :avet a v) first :e))

(defn v-by-ea [db e a]
  (-> (d/datoms db :eavt e a) first :v))
