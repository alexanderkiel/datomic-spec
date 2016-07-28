(ns datomic-spec.core
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as st]
            [clojure.string :as str]
            [datomic.api :as d])
  (:import [datomic Connection Database Datom Entity Log]
           [datomic.db Datum DbId]))

;; ---- Common --------------------------------------------------------------

(s/def ::conn
  #(instance? Connection %))

(s/def ::db
  #(instance? Database %))

(s/def ::log
  #(instance? Log %))

(s/def ::datom
  (s/spec #(instance? Datom %)
          :gen #(gen/fmap (fn [[e a v t]] (Datum. e a v t))
                          (gen/tuple (s/gen nat-int?)
                                     (s/gen nat-int?)
                                     (s/gen string?)
                                     (s/gen nat-int?)))))

(s/def ::entity
  #(instance? Entity %))

(s/def ::tempids
  (s/map-of int? nat-int?))

(s/def ::tempid
  #(instance? DbId %))

(s/def ::index-type
  #{:eavt :aevt :avet :vaet})

;; ---- TX Data ---------------------------------------------------------------

(s/def ::addition
  (s/cat :op #{:db/add}
         :eid :db/id
         :attr keyword?
         :val any?))

(s/def ::retraction
  (s/cat :op #{:db/retract}
         :eid :db/id
         :attr keyword?
         :val any?))

(s/def ::map-form map?)

(s/def ::transact-fn-call
  (s/cat :fn keyword? :args (s/* any?)))

(s/def ::tx-stmt
  (s/or :assertion ::addition
        :retraction ::retraction
        :map-form ::map-form
        :transact-fn-call ::transact-fn-call))

(s/def ::tx-data
  (s/coll-of ::tx-stmt))

;; ---- Pull Pattern ----------------------------------------------------------

(s/def ::pattern
  (s/spec (s/+ ::attr-spec)))

(s/def ::attr-spec
  (s/or :attr ::attr-name
        :wildcard ::wildcard
        :map-spec ::map-spec
        :attr-expr ::attr-expr))

(s/def ::attr-name
  keyword?)

(s/def ::wildcard
  #{"*" '*})

(s/def ::map-spec
  (s/map-of (s/or :attr ::attr-name
                  :limit-expr ::limit-expr)
            (s/or :pattern ::pattern
                  :recursion-limit ::recursion-limit)
            :min-count 1))

(s/def ::attr-expr
  (s/or :limit-expr ::limit-expr
        :default-expr ::default-expr))

(s/def ::limit-expr
  (s/cat :key #{"limit" 'limit}
         :attr ::attr-name
         :limit (s/alt :pos-int pos-int?
                       :nil nil?)))

(s/def ::default-expr
  (s/cat :key #{"default" 'default}
         :attr ::attr-name
         :val any?))


(s/def ::recursion-limit
  (s/or :pos-int pos-int? :ellipsis #{'...}))

;; ---- Entity Identifier -----------------------------------------------------

(s/def ::entity-id nat-int?)

; An entity identifier is any one of the three ways that Datomic can uniquely
; identity an entity: an entity id, ident, or lookup ref. Most Datomic APIs
; that refer to entities take entity identifiers as arguments.

(s/def ::entity-identifier
  (s/or :entity-id ::entity-id
        :ident ::attr-name
        :lookup-ref ::lookup-ref))

;; ---- Lookup Ref ------------------------------------------------------------

; A lookup ref is a list containing an attribute and a value. It identifies
; the entity with the given unique attribute value.

(s/def ::lookup-ref
  (s/cat :attr-name ::attr-name :val any?))

;; ---- Query -----------------------------------------------------------------

(defmulti query-form (fn [query] (if (map? query) :map :list)))

(defmethod query-form :map [_]
  (s/keys :req-un [::find] :opt-un [::with ::in ::where]))

(defmethod query-form :list [_]
  (s/cat :find (s/cat :find-kw #{:find} :spec ::find-spec)
         :with (s/? (s/cat :with-kw #{:with} :vars (s/+ ::variable)))
         :in (s/? (s/cat :in-kw #{:in} :inputs (s/+ ::input)))
         :where (s/? (s/cat :where-kw #{:where} :clauses (s/+ ::clause)))))

(s/def ::query
  (s/multi-spec query-form (fn [g _] g)))

(s/def ::find
  (s/cat :spec ::find-spec))

(s/def ::with
  (s/cat :vars (s/+ ::variable)))

(s/def ::in
  (s/cat :inputs (s/+ ::input)))

(s/def ::where
  (s/cat :clauses (s/+ ::clause)))

(s/def ::find-spec
  (s/alt :rel ::find-rel
         :coll ::find-coll
         :tuple ::find-tuple
         :scalar ::find-scalar))

(s/def ::find-rel
  (s/+ ::find-elem))

(s/def ::find-coll
  (s/spec (s/cat :elem ::find-elem :ellipsis #{'...})))

(s/def ::find-scalar
  (s/cat :elem ::find-elem :period #{'.}))

(s/def ::find-tuple
  (s/+ ::find-elem))

(s/def ::find-elem
  (s/or :var ::variable
        :pull-expr ::pull-expr
        :agg ::aggregate))

(s/def ::pull-expr
  (s/spec (s/cat :op #{'pull}
                 :var ::variable
                 :pattern ::pattern)))

(s/def ::aggregate
  (s/spec (s/cat :name symbol? :args (s/+ ::fn-arg))))

(s/def ::fn-arg
  (s/or :var ::variable :cst ::constant :src-var ::src-var))

(s/def ::where-clauses
  (s/cat :op #{:where} :clauses (s/+ ::clause)))

(s/def ::input
  (s/or :src-var ::src-var
        :binding ::binding
        :pattern-var ::pattern-var
        :rules-var ::rules-var))

(s/def ::src-var
  (s/and simple-symbol? #(str/starts-with? (name %) "$")))

(s/def ::variable
  (s/and simple-symbol? #(str/starts-with? (name %) "?")))

(s/def ::rules-var
  #{'%})

(s/def ::plain-symbol
  (s/and simple-symbol?
         #(not (str/starts-with? (name %) "$"))
         #(not (str/starts-with? (name %) "?"))))

(s/def ::pattern-var
  ::plain-symbol)

(s/def ::and-clause
  (s/cat :op #{'and} :clauses (s/+ ::clause)))

(s/def ::expression-clause
  (s/or :data-pattern ::data-pattern
        :pred-expr ::pred-expr
        :fn-expr ::fn-expr
        :rule-expr ::rule-expr))

(s/def ::rule-expr
  (s/cat :src-var (s/? ::src-var)
         :rule-name ::rule-name
         :vars (s/+ (s/alt :var ::variable
                           :cst ::constant
                           :unused #{'_}))))

(s/def ::not-clause
  (s/cat :src-var (s/? ::src-var)
         :op #{'not}
         :clauses (s/+ ::clause)))

(s/def ::not-join-clause
  (s/cat :src-var (s/? ::src-var)
         :op #{'not-join}
         :vars (s/spec (s/+ ::variable))
         :clauses (s/+ ::clause)))

(s/def ::or-clause
  (s/cat :src-var (s/? ::src-var)
         :op #{'or}
         :clauses (s/+ (s/alt :clause ::clause :and-clause ::and-clause))))

(s/def ::or-join-clause
  (s/cat :src-var (s/? ::src-var)
         :op #{'or-join}
         :rule-vars ::rule-vars
         :clauses (s/+ (s/alt :clause ::clause :and-clause ::and-clause))))

(s/def ::rule-vars
  (s/alt :vars (s/+ ::variable)
         :vars* (s/cat :in (s/spec (s/+ ::variable)) :out (s/* ::variable))))

(s/def ::clause
  (s/or :not-clause ::not-clause
        :not-join-clause ::not-join-clause
        :or-clause ::or-clause
        :or-join-clause ::or-join-clause
        :expression-clause ::expression-clause))

(s/def ::data-pattern
  (s/spec (s/cat :src-var (s/? ::src-var)
                 :elems (s/+ (s/alt :var ::variable
                                    :cst ::constant
                                    :blank #{'_})))))

(s/def ::constant
  (s/or :str string? :num number? :kw keyword?))

(s/def ::pred-expr
  (s/spec (s/cat :expr (s/spec (s/cat :pred (s/alt :sym symbol?
                                                   :set set?)
                                      :args (s/+ ::fn-arg))))))

(s/def ::fn-expr
  (s/spec (s/cat :expr (s/spec (s/cat :fn (s/alt :sym symbol?
                                                 :set set?)
                                      :args (s/+ ::fn-arg)))
                 :binding ::binding)))

(s/def ::binding
  (s/or :bind-scalar ::bind-scalar
        :bind-tuple ::bind-tuple
        :bind-coll ::bind-coll
        :bind-rel ::bind-rel))

(s/def ::bind-scalar
  ::variable)

(s/def ::bind-tuple
  (s/spec (s/+ (s/alt :var ::variable :unused #{'_}))))

(s/def ::bind-coll
  (s/spec (s/cat :var ::variable :ellipsis #{'...})))

(s/def ::bind-rel
  (s/coll-of (s/spec (s/+ (s/alt :var ::variable :unused #{'_})))))

(s/def ::rules
  (s/spec (s/+ ::rule)))

(s/def ::rule
  (s/spec (s/cat :head ::rule-head
                 :clauses (s/+ ::clause))))

(s/def ::rule-head
  (s/spec (s/cat :name ::rule-name
                 :vars ::rule-vars)))

(s/def ::rule-name
  ::plain-symbol)

;; ---- Special ---------------------------------------------------------------

(s/def :datomic-spec.query/args
  (s/spec (s/+ any?)))

(s/def :datomic-spec.query/timeout
  nat-int?)

(s/def ::tx-num-tx-id-date
  (s/alt :tx-num nat-int? :tx-id nat-int? :date inst?))

;; ---- Spec Overrides --------------------------------------------------------

(def spec-overrides
  {`d/as-of
   (s/fspec :args (s/cat :db ::db :t ::tx-num-tx-id-date))

   `d/as-of-t
   (s/fspec :args (s/cat :db ::db))

   `d/attribute
   (s/fspec :args (s/cat :db ::db :attrid (s/alt :entity-id nat-int?
                                                 :ident ::attr-name)))
   `d/basis-t
   (s/fspec :args (s/cat :db ::db))

   `d/connect
   (s/fspec :args (s/cat :uri string?))

   `d/create-database
   (s/fspec :args (s/cat :uri string?))

   `d/datoms
   (s/fspec :args (s/cat :db ::db
                         :index ::index-type
                         :components (s/* ::entity-identifier)))

   `d/db
   (s/fspec :args (s/cat :conn any?))

   `d/delete-database
   (s/fspec :args (s/cat :uri string?))

   `d/entid
   (s/fspec :args (s/cat :db ::db :ident ::entity-identifier))

   `d/entid-at
   (s/fspec :args (s/cat :db ::db
                         :part ::entity-identifier
                         :t-or-date (s/alt :t nat-int? :date inst?)))

   `d/entity
   (s/fspec :args (s/cat :db ::db :eid ::entity-identifier))

   `d/entity-db
   (s/fspec :args (s/cat :entity ::entity))

   `d/filter
   (s/fspec :args (s/cat :db ::db :pred fn?))

   `d/get-database-names
   (s/fspec :args (s/cat :uri string?))

   `d/history
   (s/fspec :args (s/cat :db ::db))

   `d/ident
   (s/fspec :args (s/cat :db ::db :eid (s/alt :entity-id nat-int?
                                              :attr ::attr-name)))

   `d/index-range
   (s/fspec :args (s/cat :db ::db :attrid ::entity-identifier
                         :start (s/nilable nat-int?) :end (s/nilable nat-int?)))

   `d/is-filtered
   (s/fspec :args (s/cat :db ::db))

   `d/log
   (s/fspec :args (s/cat :conn ::conn) :ret (s/nilable ::log))

   `d/next-t
   (s/fspec :args (s/cat :db ::db))

   `d/part
   (s/fspec :args (s/cat :eid nat-int?))

   `d/pull
   (s/fspec :args (s/cat :db ::db
                         :pattern (s/spec ::pattern)
                         :eid ::entity-identifier))
   `d/pull-many
   (s/fspec :args (s/cat :db ::db
                         :pattern (s/spec ::pattern)
                         :eid (s/every ::entity-identifier)))

   `d/q
   (s/fspec :args (s/cat :query ::query :inputs (s/+ any?)))

   `d/query
   (s/fspec :args (s/cat :query-map (s/keys :req-un [::query :datomic-spec.query/args]
                                            :opt-un [:datomic-spec.query/timeout])))

   `d/release
   (s/fspec :args (s/cat :conn ::conn))

   `d/remove-tx-report-queue
   (s/fspec :args (s/cat :conn ::conn))

   `d/rename-database
   (s/fspec :args (s/cat :uri string? :new-name string?))

   `d/request-index
   (s/fspec :args (s/cat :conn ::conn))

   `d/resolve-tempid
   (s/fspec :args (s/cat :db ::db :tempids ::tempids :tempid ::tempid))

   `d/seek-datoms
   (s/fspec :args (s/cat :db ::db
                         :index ::index-type
                         :components (s/* ::entity-identifier)))

   `d/shutdown
   (s/fspec :args (s/cat :shutdown-clojure boolean?))

   `d/since
   (s/fspec :args (s/cat :db ::db :t ::tx-num-tx-id-date))

   `d/since-t
   (s/fspec :args (s/cat :db ::db))

   `d/squuid
   (s/fspec :args (s/cat) :ret uuid?)

   `d/squuid-time-millis
   (s/fspec :args (s/cat :squuid uuid?) :ret nat-int?)

   `d/sync
   (s/fspec :args (s/cat :conn ::conn :t (s/? nat-int?)))

   `d/sync-excise
   (s/fspec :args (s/cat :conn ::conn :t nat-int?))

   `d/sync-index
   (s/fspec :args (s/cat :conn ::conn :t nat-int?))

   `d/sync-schema
   (s/fspec :args (s/cat :conn ::conn :t nat-int?))

   `d/t->tx
   (s/fspec :args (s/cat :t nat-int?) :ret nat-int?)

   `d/tempid
   (s/fspec :args (s/cat :partition keyword? :n (s/? int?)) :ret ::tempid)

   `d/touch
   (s/fspec :args (s/cat :entity ::entity))

   `d/transact
   (s/fspec :args (s/cat :connection ::conn :tx-data ::tx-data))

   `d/transact-async
   (s/fspec :args (s/cat :connection ::conn :tx-data ::tx-data))

   `d/tx->t
   (s/fspec :args (s/cat :tx nat-int?) :ret nat-int?)

   `d/tx-range
   (s/fspec :args (s/cat :log ::log
                         :start (s/nilable ::tx-num-tx-id-date)
                         :end (s/nilable ::tx-num-tx-id-date)))

   `d/tx-report-queue
   (s/fspec :args (s/cat :connection ::conn))

   `d/with
   (s/fspec :args (s/cat :db ::db :tx-data ::tx-data))})

(defn instrument
  "Instruments the datomic.api namespace.

  Opts are the same as in clojure.spec.test/instrument."
  ([]
   (instrument nil))
  ([opts]
   (st/instrument (st/enumerate-namespace 'datomic.api)
                  (merge {:spec spec-overrides} opts))))
