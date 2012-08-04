(defproject test-lily/test-lily "0.1.0-SNAPSHOT"
  :license {:name "Eclipse Public License",
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.lilyproject.lily-client "1.2.1"]]
  :profiles {:dev
             {:dependencies
              [[com.intelie/lazytest
                "1.0.0-SNAPSHOT"
                :exclusions
                [swank-clojure]]
               [midje "1.4.0"]]}}
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :description "Testing the lily stack from the repl.")
