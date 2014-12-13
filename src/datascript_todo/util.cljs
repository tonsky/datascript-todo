(ns datascript-todo.util)

(defn remove-vals [f m]
  (reduce-kv (fn [m k v] (if (f v) m (assoc m k v))) (empty m) m))
