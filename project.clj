(defproject datascript-todo "0.1.0"
  :global-vars  {*warn-on-reflection* true}

  :dependencies [
    [org.clojure/clojure "1.7.0-alpha4"]
    [org.clojure/clojurescript "0.0-2411"]
  ]

  :plugins [
    [lein-cljsbuild "1.0.3"]
  ]

  :cljsbuild { 
    :builds [
      { :id "release"
        :source-paths  ["src"]
        :compiler {
          :output-to     "web/todo.min.js"
          :optimizations :advanced
          :pretty-print  false
        }}
  ]}
  
  :profiles {
    :dev {
      :cljsbuild {
        :builds [
          { :id "dev"
            :source-paths  ["src"]
            :compiler {
              :output-to     "web/todo.js"
              :output-dir    "web/out"
              :optimizations :none
              :source-map    true
            }}
      ]}
    }
  }
)
