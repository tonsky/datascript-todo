(ns datascript-todo.react
  (:require
    [sablono.compiler :as s]))

(defmacro defc [name argvec render]
  `(def ~name (component (fn ~argvec ~(s/compile-html render)))))
