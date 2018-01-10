(defproject org.clojars.akiel/datomic-spec "0.3-SNAPSHOT"
  :description "Specs for Datomic"
  :url "https://github.com/alexanderkiel/datomic-spec"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]]

  :plugins [[jonase/eastwood "0.2.5" :exclusions [org.clojure/clojure]]]

  :profiles {:dev
             {:dependencies [[com.datomic/datomic-free "0.9.5385"]
                             [org.clojars.akiel/iota "0.1"]
                             [org.clojure/test.check "0.9.0"]]}}

  :aliases
  {"lint" ["eastwood" "{:exclude-linters [:constant-test :suspicious-test]}"]})
