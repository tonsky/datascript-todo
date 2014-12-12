(ns datascript-todo.react)

(defn component [render-fn]
  (let [react-component
        (.createClass js/React
           #js {:render
                (fn []
                  (this-as this
                    (apply render-fn (aget (.-props this) "args"))))
                })]
    (fn [& args]
      (react-component #js {:args args}))))

(defn render [component node]
  (.renderComponent js/React component node))
