(ns datomic-spec.test
  (:require
    [clojure.spec.test.alpha :as st]
    [datomic-spec.core :as core]))

(defn instrument
  "Instruments the datomic.api namespace.

  Opts are the same as in clojure.spec.test/instrument."
  ([]
   (instrument nil))
  ([opts]
   (st/instrument (st/enumerate-namespace 'datomic.api)
                  (merge {:spec core/spec-overrides} opts))))
