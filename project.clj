(defproject org.clojars.akiel/datomic-spec "0.5.1"
  :description "Specs for Datomic"
  :url "https://github.com/alexanderkiel/datomic-spec"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies
  [[org.clojure/spec.alpha "0.2.176"]]

  :plugins
  [[jonase/eastwood "0.2.5" :exclusions [org.clojure/clojure]]]

  :profiles
  {:dev
   {:dependencies
    [[com.datomic/datomic-free "0.9.5697"]
     [org.clojars.akiel/iota "0.1"]
     [org.clojure/clojure "1.10.0"]
     [org.clojure/test.check "0.9.0"]]}}

  :aliases
  {"lint" ["eastwood" "{:exclude-linters [:constant-test :suspicious-test]}"]})
