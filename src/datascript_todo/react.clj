(ns datascript-todo.react)

(defmacro defc [name argvec render]
  `(def ~name (component (fn ~argvec ~render))))
