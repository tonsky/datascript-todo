(defproject datascript-todo "0.1.0"
  :dependencies [
    [org.clojure/clojure "1.7.0"]
    [org.clojure/clojurescript "1.7.122"]
    [datascript "0.12.1"]
    [datascript-transit "0.1.0"]
    [rum "0.3.0"]
  ]

  :plugins [
    [lein-cljsbuild "1.1.0"]
  ]

  :cljsbuild { 
    :builds [
      { :id "advanced"
        :source-paths  ["src"]
        :compiler {
          :main          datascript-todo
          :output-to     "target/todo.js"
          :optimizations :advanced
          :pretty-print  false
          :warnings      {:single-segment-namespace false}
        }}
  ]}
  
  :profiles {
    :dev {
      :cljsbuild {
        :builds [
          { :id "none"
            :source-paths  ["src"]
            :compiler {
              :main          datascript-todo
              :output-to     "target/todo.js"
              :output-dir    "target/none"
              :optimizations :none
              :source-map    true
              :warnings      {:single-segment-namespace false}
            }}
      ]}
    }
  }
)
