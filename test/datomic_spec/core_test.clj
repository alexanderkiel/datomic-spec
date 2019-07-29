(ns datomic-spec.core-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer :all]
    [datomic.api :as d]
    [datomic-spec.core :as ds :refer :all]
    [datomic-spec.test-util :refer [db-fixture]]
    [juxt.iota :refer [given]])
  (:import
    [java.util Date]))

(use-fixtures :each db-fixture)

(defn connect []
  (d/connect "datomic:mem://test"))

(defn db []
  (d/db (connect)))

(defn valid-args? [fn-sym & args]
  (let [spec (:args (spec-overrides fn-sym))]
    (when-not spec (throw (Exception. (str "Missing fn-spec for " fn-sym))))
    (s/valid? spec args)))

(defn valid-ret? [fn-sym ret]
  (let [spec (:ret (spec-overrides fn-sym))]
    (when-not spec (throw (Exception. (str "Missing fn-spec for " fn-sym))))
    (s/valid? spec ret)))

(deftest connect-uri-test
  (testing "valid"
    (are [uri] (s/valid? ::ds/connect-uri uri)
      "datomic:mem://test"
      "datomic:free://transactor-host:8080/foo"
      {:protocol :sql
       :db-name "foo"
       :data-source :bar}
      {:protocol :cass
       :db-name "foo"
       :table "bar"
       :cluster :baz}))
  (testing "invalid"
    (are [uri] (not (s/valid? ::ds/connect-uri uri))
      "\ndatomic:free://transactor-host:8080/foo")))

(deftest as-of
  (is (valid-args? `d/as-of (db) 1))
  (is (valid-args? `d/as-of (db) (Date.))))

(deftest as-of-t
  (is (valid-args? `d/as-of-t (db))))

(deftest attribute
  (is (valid-args? `d/attribute (db) :db/ident)))

(deftest basis-t
  (is (valid-args? `d/basis-t (db))))

(deftest connect-test
  (is (valid-args? `d/connect "datomic:mem://test")))

(deftest create-database
  (is (valid-args? `d/create-database "datomic:mem://test")))

(deftest datoms
  (testing "EAVT"
    (is (valid-args? `d/datoms (db) :eavt)))
  (testing "EAVT with entity id"
    (is (valid-args? `d/datoms (db) :eavt 1)))
  (testing "EAVT with ident"
    (is (valid-args? `d/datoms (db) :eavt :db/ident)))
  (testing "EAVT with lookup-ref"
    (is (valid-args? `d/datoms (db) :eavt [:db/ident :db/ident])))
  (testing "AEVT"
    (is (valid-args? `d/datoms (db) :aevt)))
  (testing "AEVT with attribute"
    (is (valid-args? `d/datoms (db) :aevt :db/ident)))
  (testing "AVET"
    (is (valid-args? `d/datoms (db) :avet)))
  (testing "AVET with attribute and value"
    (is (valid-args? `d/datoms (db) :avet :db/ident "1")))
  (testing "VAET"
    (is (valid-args? `d/datoms (db) :vaet))))

(deftest db-test
  (is (valid-args? `d/db (connect))))

(deftest delete-database
  (is (valid-args? `d/delete-database "datomic:mem://test")))

(deftest entid
  (is (valid-args? `d/entid (db) 10))
  (is (valid-args? `d/entid (db) :db/ident))
  (is (valid-args? `d/entid (db) [:db/ident :db/ident])))

(deftest entid-at
  (testing "with t"
    (is (valid-args? `d/entid-at (db) :db.part/user 0)))
  (testing "with partition entity id and t"
    (is (valid-args? `d/entid-at (db) 0 0)))
  (testing "with partition lookup-ref and t"
    (is (valid-args? `d/entid-at (db) [:db/ident :db.part/user] 0)))
  (testing "with date"
    (is (valid-args? `d/entid-at (db) :db.part/user (Date.)))))

(deftest entity
  (is (valid-args? `d/entity (db) 10))
  (is (valid-args? `d/entity (db) :db/ident))
  (is (valid-args? `d/entity (db) [:db/ident :db/ident])))

(deftest entity-db
  (is (valid-args? `d/entity-db (d/entity (db) :db/ident))))

(deftest filter-test
  (is (valid-args? `d/filter (db) (constantly true))))

(comment (db-fixture #(filter-test)))

(deftest function-test
  ;; TODO
  (is true))

(deftest gc-storage
  ;; TODO
  (is true))

(deftest get-database-names
  (is (valid-args? `d/get-database-names "datomic:mem://*")))

(deftest history
  (is (valid-args? `d/history (db))))

(deftest ident
  (is (valid-args? `d/ident (db) 10))
  (is (valid-args? `d/ident (db) :db/ident)))

(deftest index-range
  (is (valid-args? `d/index-range (db) 10 0 nil))
  (is (valid-args? `d/index-range (db) :db/ident 0 nil))
  (is (valid-args? `d/index-range (db) [:db/ident :db/ident] 0 nil))
  (is (valid-args? `d/index-range (db) 10 0 1)))

(deftest invoke
  ;; TODO
  (is true))

(deftest is-filtered
  (is (valid-args? `d/is-filtered (db))))

(deftest log
  (is (valid-args? `d/log (connect))))

(deftest next-t
  (is (valid-args? `d/next-t (db))))

(deftest part
  (is (valid-args? `d/part 10)))

(deftest pull
  (testing "Pattern with attribute name as vector"
    (is (valid-args? `d/pull (db) '[:db/unique] :db/ident)))
  (testing "Pattern with attribute name as list"
    (is (valid-args? `d/pull (db) '(:db/unique) :db/ident)))
  (testing "Wildcard"
    (is (valid-args? `d/pull (db) '[*] :db/ident)))
  (testing "Map spec with attribute name and pattern"
    (is (valid-args? `d/pull (db) '[{:db/unique [:db/ident]}] :db/ident)))
  (testing "Map spec with attribute name and recursion limit (number)"
    (is (valid-args? `d/pull (db) '[{:db/unique 1}] :db/ident)))
  (testing "Map spec with attribute name and recursion limit (ellipsis)"
    (is (valid-args? `d/pull (db) '[{:db/unique ...}] :db/ident)))
  (testing "Map spec with two entries"
    (is (valid-args? `d/pull (db) '[{:db/unique [:db/ident]
                                     :db/type [:db/ident]}] :db/ident)))
  (testing "Map spec with limit expression and pattern"
    (is (valid-args? `d/pull (db) '[{(limit :db/unique 10) [:db/ident]}] :db/ident)))
  (testing "Map spec with limit expression with nil and pattern"
    (is (some? (valid-args? `d/pull (db) [{`(~'limit :db/unique ~nil) [:db/ident]}] :db/ident))))
  (testing "Attribute expression with limit expression"
    (is (valid-args? `d/pull (db) '[(limit :db/unique 10)] :db/ident)))
  (testing "Attribute expression with default expression"
    (is (valid-args? `d/pull (db) '[(default :foo :bar)] :db/ident)))
  (testing "Plus is invalid"
    (is (not (valid-args? `d/pull (db) '[+] :db/ident)))))

(deftest pull-many
  (is (valid-args? `d/pull-many (db) '[*] [:db/ident :db/unique])))

(deftest q
  (is (valid-args? `d/q '[:find ?e :where [?e :db/ident]] (db))))

(deftest query
  (is (valid-args? `d/query {:query '[:find ?e :where [?e :db/ident]]
                             :args [(db)]
                             :timeout 100})))

(deftest release
  (is (valid-args? `d/release (connect))))

(deftest remove-tx-report-queue
  (is (valid-args? `d/remove-tx-report-queue (connect))))

(deftest rename-database
  (is (valid-args? `d/rename-database "datomic:mem://test" "test-1")))

(deftest request-index
  (is (valid-args? `d/request-index (connect))))

(deftest resolve-tempid
  (is (valid-args? `d/resolve-tempid (db) {-1 1} (d/tempid :db.part/user -1))))

(deftest seek-datoms
  (testing "EAVT"
    (is (valid-args? `d/seek-datoms (db) :eavt)))
  (testing "EAVT with entity id"
    (is (valid-args? `d/seek-datoms (db) :eavt 1)))
  (testing "EAVT with ident"
    (is (valid-args? `d/seek-datoms (db) :eavt :db/ident)))
  (testing "EAVT with lookup-ref"
    (is (valid-args? `d/seek-datoms (db) :eavt [:db/ident :db/ident])))
  (testing "EAVT"
    (is (valid-args? `d/seek-datoms (db) :aevt)))
  (testing "EAVT"
    (is (valid-args? `d/seek-datoms (db) :avet)))
  (testing "EAVT"
    (is (valid-args? `d/seek-datoms (db) :vaet))))

(deftest shutdown
  (is (valid-args? `d/shutdown true))
  (is (valid-args? `d/shutdown false)))

(deftest since
  (is (valid-args? `d/since (db) 1))
  (is (valid-args? `d/since (db) (Date.))))

(deftest since-t
  (is (valid-args? `d/since-t (db))))

(deftest squuid
  (is (valid-args? `d/squuid))
  (is (valid-ret? `d/squuid #uuid "a1f44a17-00b7-4825-94a6-9e46917335fa")))

(deftest squuid-time-millis
  (is (valid-args? `d/squuid-time-millis #uuid "a1f44a17-00b7-4825-94a6-9e46917335fa"))
  (is (valid-ret? `d/squuid-time-millis 2717141527000)))

(deftest sync-test
  (is (valid-args? `d/sync (connect)))
  (is (valid-args? `d/sync (connect) 1)))

(deftest sync-excise
  (is (valid-args? `d/sync-excise (connect) 1)))

(deftest sync-index
  (is (valid-args? `d/sync-index (connect) 1)))

(deftest sync-schema
  (is (valid-args? `d/sync-schema (connect) 1)))

;; TODO: errors in clojure.spec.test.alpha$spec_checking_fn$fn__3026 cannot be cast to clojure.lang.IFn$LO
;; (deftest t->tx
;;   (is (valid-args? `d/t->tx 1))
;;   (is (valid-ret? `d/t->tx 1)))

(deftest tempid
  (is (valid-args? `d/tempid :foo))
  (is (valid-args? `d/tempid :foo -1))
  (is (valid-ret? `d/tempid (d/tempid :foo))))

(deftest touch
  (is (valid-args? `d/touch (d/entity (db) :db/ident))))

(deftest transact
  (is (valid-args? `d/transact (connect) [{:db/id (d/tempid :foo)}])))

(deftest transact-async
  (is (valid-args? `d/transact-async (connect) [{:db/id (d/tempid :foo)}])))

;; TODO: errors in clojure.spec.test.alpha$spec_checking_fn$fn__3026 cannot be cast to clojure.lang.IFn$LO
;; (deftest tx->t
;;   (is (valid-args? `d/tx->t 1))
;;   (is (valid-ret? `d/tx->t 1)))

(deftest tx-report-queue
  (is (valid-args? `d/tx-report-queue (connect))))

(deftest with
  (is (valid-args? `d/with (db) [{:db/id (d/tempid :foo)}])))

;; ---- Pull Pattern ----------------------------------------------------------

(deftest attr-spec
  (testing "attr-name"
    (is (= [:attr :a] (s/conform ::ds/attr-spec :a))))
  (testing "wildcard"
    (is (= [:wildcard '*] (s/conform ::ds/attr-spec '*))))
  (testing "limit-expr"
    (is (= [:attr-expr
            [:limit-expr
             {:key 'limit, :attr :a, :limit [:pos-int 1]}]]
           (s/conform ::ds/attr-spec ['limit :a 1])))))

(deftest limit-expr
  (is (= {:key 'limit, :attr :a, :limit [:pos-int 1]}
         (s/conform ::ds/limit-expr ['limit :a 1]))))

(deftest default-expr
  (is (= {:key 'default, :attr :a, :val 1}
         (s/conform ::ds/default-expr ['default :a 1]))))

;; ---- Query -----------------------------------------------------------------

(deftest query-spec
  (testing "find with single relational var"
    (given (s/conform ::ds/query '[:find ?x])
      [:find :spec val 0] := '[:var ?x])
    (given (s/conform ::ds/query '{:find [?x]})
      [:find :spec val 0] := '[:var ?x]))

  (testing "find with two relational vars"
    (given (s/conform ::ds/query '[:find ?x ?y])
      [:find :spec val 0] := '[:var ?x]
      [:find :spec val 1] := '[:var ?y])
    (given (s/conform ::ds/query '{:find [?x ?y]})
      [:find :spec val 0] := '[:var ?x]
      [:find :spec val 1] := '[:var ?y]))

  (testing "single where clause with data pattern"
    (given (s/conform ::ds/query '[:find ?e :where [?e :db/ident]])
      [:find :spec val 0] := '[:var ?e]
      [:where :clauses 0 val val :elems 0] := '[:var ?e]
      [:where :clauses 0 val val :elems 1] := '[:cst [:kw :db/ident]])
    (given (s/conform ::ds/query '{:find [?e] :where [[?e :db/ident]]})
      [:find :spec val 0] := '[:var ?e]
      [:where :clauses 0 val val :elems 0] := '[:var ?e]
      [:where :clauses 0 val val :elems 1] := '[:cst [:kw :db/ident]]))

  (testing "two where clauses with data pattern"
    (given (s/conform ::ds/query '[:find ?e :where [?e ?a]
                                   [?a :db/type :db.type/string]])
      [:find :spec val 0] := '[:var ?e]
      [:where :clauses 0 val val :elems 0] := '[:var ?e]
      [:where :clauses 0 val val :elems 1] := '[:var ?a]
      [:where :clauses 1 val val :elems 0] := '[:var ?a]
      [:where :clauses 1 val val :elems 1] := '[:cst [:kw :db/type]]
      [:where :clauses 1 val val :elems 2] := '[:cst [:kw :db.type/string]])
    (given (s/conform ::ds/query '{:find [?e]
                                   :where [[?e ?a] [?a :db/type :db.type/string]]})
      [:find :spec val 0] := '[:var ?e]
      [:where :clauses 0 val val :elems 0] := '[:var ?e]
      [:where :clauses 0 val val :elems 1] := '[:var ?a]
      [:where :clauses 1 val val :elems 0] := '[:var ?a]
      [:where :clauses 1 val val :elems 1] := '[:cst [:kw :db/type]]
      [:where :clauses 1 val val :elems 2] := '[:cst [:kw :db.type/string]]))

  (testing "collection binding input"
    (given (s/conform ::ds/query '[:find ?a .
                                   :in $ [?a ...]
                                   :where [?a :db/type :db.type/string]])
      [:find :spec val :elem] := '[:var ?a]
      [:in :inputs 0] := '[:src-var $]
      [:in :inputs 1 val val :var] := '?a
      [:where :clauses 0 val val :elems 0] := '[:var ?a]
      [:where :clauses 0 val val :elems 1] := '[:cst [:kw :db/type]]
      [:where :clauses 0 val val :elems 2] := '[:cst [:kw :db.type/string]])
    (given (s/conform ::ds/query '{:find [?a .]
                                   :in [$ [?a ...]]
                                   :where [[?a :db/type :db.type/string]]})
      [:find :spec val :elem] := '[:var ?a]
      [:in :inputs 0] := '[:src-var $]
      [:in :inputs 1 val val :var] := '?a
      [:where :clauses 0 val val :elems 0] := '[:var ?a]
      [:where :clauses 0 val val :elems 1] := '[:cst [:kw :db/type]]
      [:where :clauses 0 val val :elems 2] := '[:cst [:kw :db.type/string]])))

(deftest find-coll
  (given (s/conform ::ds/find-coll '[?x ...])
    [:elem val] := '?x))

(deftest find-scalar
  (given (s/conform ::ds/find-scalar '[?x .])
    [:elem val] := '?x))

(deftest find-tuple
  (given (s/conform ::ds/find-tuple '[?x])
    [0 val] := '?x)
  (given (s/conform ::ds/find-tuple '[?x ?y])
    [0 val] := '?x
    [1 val] := '?y))

(deftest find-elem
  (are [x c] (= c (s/conform ::ds/find-elem x))
    '?x '[:var ?x])
  (given (s/conform ::ds/find-elem '(pull ?x [*]))
    [val :var] := '?x
    [val :pattern] := '[[:wildcard *]])
  (given (s/conform ::ds/find-elem '(max ?x))
    [val :name] := 'max
    [val :args 0] := '[:var ?x]))

(deftest constant
  (are [c x] (= x (s/unform ::ds/constant c))
    [:str "foo"] "foo"
    [:num 1] 1
    [:kw :foo] :foo))

(deftest pred-expr
  (given (s/conform ::ds/pred-expr '[(< ?year 1600)])
    [:expr :pred] := '[:sym <]
    [:expr :args 0] := [:var '?year]
    [:expr :args 1] := [:cst [:num 1600]])
  (given (s/conform ::ds/pred-expr '[(nil? ?x)])
    [:expr :pred] := '[:sym nil?]
    [:expr :args 0] := [:var '?x])
  (given (s/conform ::ds/pred-expr '[(#{1 2} ?x)])
    [:expr :pred] := [:set #{1 2}]
    [:expr :args 0] := [:var '?x]))

(deftest binding-test
  (given (s/conform ::ds/binding '?x)
    val := '?x)
  (given (s/conform ::ds/binding '[?x])
    [val 0] := '[:var ?x])
  (given (s/conform ::ds/binding '[?x ...])
    [val :var] := '?x))

(deftest bind-scalar
  (is (= '?x (s/conform ::ds/bind-scalar '?x))))

(deftest bind-tuple
  (given (s/conform ::ds/bind-tuple '[?x])
    0 := '[:var ?x])
  (given (s/conform ::ds/bind-tuple '[_])
    0 := '[:unused _])
  (given (s/conform ::ds/bind-tuple '[?x ?y])
    0 := '[:var ?x]
    1 := '[:var ?y])
  (given (s/conform ::ds/bind-tuple '[?x _])
    0 := '[:var ?x]
    1 := '[:unused _]))

(deftest bind-coll
  (given (s/conform ::ds/bind-coll '[?x ...])
    :var := '?x))

(deftest bind-rel
  (given (s/conform ::ds/bind-rel '[[?x]])
    [0 0] := '[:var ?x])
  (given (s/conform ::ds/bind-rel '[[_]])
    [0 0] := '[:unused _])
  (given (s/conform ::ds/bind-rel '[[?x ?y]])
    [0 0] := '[:var ?x]
    [0 1] := '[:var ?y])
  (given (s/conform ::ds/bind-rel '[[?x _]])
    [0 0] := '[:var ?x]
    [0 1] := '[:unused _]))

(deftest rules
  (given (s/conform ::ds/rules '[[(c-type ?c ?t)
                                  [?c :c/type ?t]]])
    [0 #(s/unform ::ds/rule %)] := '[(c-type ?c ?t)
                                     [?c :c/type ?t]]))

(deftest rule
  (given (s/conform ::ds/rule '[(twitter? ?c)
                                [?c :c/type :c.type/twitter]])
    [:head :name] := 'twitter?
    [:clauses 0 #(s/unform ::ds/clause %)] := '[?c :c/type :c.type/twitter])
  (given (s/conform ::ds/rule '[(c-type ?c ?t)
                                [?c :c/type ?t]])
    [:head :name] := 'c-type
    [:head :vars] := '[:vars [?c ?t]]
    [:clauses 0 #(s/unform ::ds/clause %)] := '[?c :c/type ?t])
  (given (s/conform ::ds/rule '[(c-type [?c] ?t)
                                [?c :c/type ?t]])
    [:head :vars] := '[:vars* {:in [?c], :out [?t]}]))

(deftest rule-head
  (given (s/conform ::ds/rule-head '(twitter? ?c))
    :name := 'twitter?
    :vars := '[:vars [?c]]))
