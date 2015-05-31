(defproject datascript-todo "0.1.0"
  :dependencies [
    [org.clojure/clojure "1.7.0-RC1"]
    [org.clojure/clojurescript "0.0-3297"]
    [datascript "0.11.2"]
    [rum "0.2.6"]
    [com.cognitect/transit-cljs "0.8.215"]
  ]

  :plugins [
    [lein-cljsbuild "1.0.6"]
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
