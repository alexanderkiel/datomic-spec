(ns datomic-spec.test-util
  (:require [datomic.api :as d]))

(defn db-fixture [f]
  (try
    (d/create-database "datomic:mem://test")
    (f)
    (finally
      (d/delete-database "datomic:mem://test"))))
