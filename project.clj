(defproject wundernut11 "1.0.0"
  :description "Wundernut 11 solution"
  :main wundernut.analyzer
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [incanter "1.5.7"]
                 [org.craigandera/dynne "0.4.1"]
                 ]
  :repl-options {:init-ns wundernut.analyzer}
  :jvm-opts ["-Djava.awt.headless=true"]
  :profiles
  {:uberjar {
             :aot :all
             :uberjar-name "wundernut11-morse-decoder.jar"
             }})
