(ns onodrim.api-test
  (:require [clojure.test :refer :all]
            [onodrim.api :refer :all]
            [datomic.api :as d]
            [datomock.core :as dm]
            [datofu.schema.dsl :as ds]
            [clojure.string :as str])
  (:use clojure.repl clojure.pprint)
  (:import (java.util Date GregorianCalendar Calendar)))

;; TODO explore the Entity API in more details
(comment
  (def db (get-db))
  => #'onodrim.api-test/db

  (d/entity db :chuck-norris)
  => {:db/id 17592186045422}

  (def cn *1)
  => #'onodrim.api-test/cn

  (get cn :person/firstName)
  => "Chuck"

  (get cn ":person/firstName")
  => "Chuck"

  cn
  => {:db/id 17592186045422, :person/firstName "Chuck"}



  (d/pull (d/entity-db cn) '[*] (:db/id cn))
  =>
  {:db/id 17592186045422,
   :db/ident :chuck-norris,
   :person/id "chuck-norris",
   :person/firstName "Chuck",
   :person/lastName "Norris",
   :person/birth-date #inst"1940-03-10T00:00:00.000-00:00",
   :person/gender {:db/id 17592186045417}}

  (d/pull (d/entity-db cn) '[* {:person/gender [:db/ident]}] (:db/id cn))
  =>
  {:db/id 17592186045422,
   :db/ident :chuck-norris,
   :person/id "chuck-norris",
   :person/firstName "Chuck",
   :person/lastName "Norris",
   :person/birth-date #inst"1940-03-10T00:00:00.000-00:00",
   :person/gender {:db/ident :person.gender/male}}

  (class cn)
  => datomic.query.EntityMap

  (type cn)
  => datomic.query.EntityMap

  (ancestors (type cn))
  =>
  #{datomic.Entity                                          ;; http://docs.datomic.com/javadoc/datomic/Entity.html
    clojure.lang.ILookup                                    ;; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/ILookup.java
    clojure.lang.Associative                                ;; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Associative.java
    clojure.lang.IType                                      ;; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/IType.java
    clojure.lang.IPersistentCollection                      ;; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/IPersistentCollection.java
    clojure.lang.Seqable                                    ;; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Seqable.java
    java.lang.Object
    datomic.query.EMapImpl}

  ;; Entity
  (.keySet cn)
  => #{":person/birth-date" ":person/gender" ":person/id" ":person/firstName" ":person/lastName" ":db/ident"}
  ;; NOTE how :db/id's not here

  (type *1)
  => clojure.lang.PersistentHashSet

  (.get cn :person/firstName)
  => "Chuck"

  (.get cn ":person/firstName")
  => "Chuck"



  ;;

  ;; IAssociative
  (.assoc cn :a :b)
  ;java.lang.AbstractMethodError: datomic.query.EntityMap.assoc(Ljava/lang/Object;Ljava/lang/Object;)Lclojure/lang/Associative;

  (.entryAt cn :person/firstName)
  => [:person/firstName "Chuck"]
  (type *1)
  => clojure.lang.MapEntry
  (.entryAt cn ":person/firstName")
  => [:person/firstName "Chuck"]
  (type *1)
  => clojure.lang.MapEntry

  (.entryAt cn :a)
  => nil

  (.entryAt cn :db/id)
  => [:db/id 17592186045422]

  ;; ILookup
  (.valAt cn :person/firstName)
  => "Chuck"
  (.valAt cn ":person/firstName")
  => "Chuck"
  (.valAt cn :a)
  => nil
  (.valAt cn :db/id)
  => 17592186045422
  (.valAt cn :db/ident)
  => :chuck-norris

  ;; keyword coercion
  (get cn ":person/id")
  => "chuck-norris"
  (.valAt cn ":person/id")
  => "chuck-norris"
  (d/entid (d/entity-db cn) :person/id)
  => 63
  (.valAt cn 63)
  ;IllegalArgumentExceptionInfo :db.error/not-a-keyword Cannot interpret as a keyword: 63 of class: class java.lang.Long  datomic.error/arg (error.clj:57)
  (.valAt cn 'person/id)
  => "chuck-norris"

  ;; IType
  ;; nothing to implement here, it's a marker interface


  ;; IPersistentCollection
  (.count cn)
  => 6

  (.cons cn 42)
  ;AbstractMethodError datomic.query.EntityMap.cons(Ljava/lang/Object;)Lclojure/lang/IPersistentCollection;  sun.reflect.NativeMethodAccessorImpl.invoke0 (NativeMethodAccessorImpl.java:-2)

  (.empty cn)
  => {:db/id 17592186045422}
  (= cn *1)
  => true
  (identical? cn *2)
  => false

  (.equiv cn (.empty cn))
  => true

  (.equiv cn nil)
  => false

  (.equiv cn (select-keys cn (keys cn)))
  => false

  (select-keys cn (keys cn))
  =>
  {:db/ident :chuck-norris,
   :person/id "chuck-norris",
   :person/firstName "Chuck",
   :person/lastName "Norris",
   :person/birth-date #inst"1940-03-10T00:00:00.000-00:00",
   :person/gender :person.gender/male}

  (.equiv cn (d/entity (d/entity-db cn) [:person/id "john-doe"]))
  => false

  (d/entity (d/entity-db cn) [:person/id "john-doe"])
  => {:db/id 17592186045420}

  ;; Seqable
  (.seq cn)
  =>
  ([:db/ident :chuck-norris]
    [:person/id "chuck-norris"]
    [:person/firstName "Chuck"]
    [:person/lastName "Norris"]
    [:person/birth-date #inst"1940-03-10T00:00:00.000-00:00"]
    [:person/gender :person.gender/male])
  (type *1)
  => clojure.lang.Cons
  (next *2)
  =>
  ([:person/id "chuck-norris"]
    [:person/firstName "Chuck"]
    [:person/lastName "Norris"]
    [:person/birth-date #inst"1940-03-10T00:00:00.000-00:00"]
    [:person/gender :person.gender/male])
  (type *1)
  => clojure.lang.Cons
  (list? (seq cn))
  => false

  ;; java.lang.Object
  (.equals cn (empty cn))
  => true

  (.equals cn (select-keys cn (keys cn)))
  => false

  ;; TODO refs
  ;; TODO reverse keys


  => #{":person/birth-date" ":person/gender" ":person/id" ":person/firstName" ":person/lastName" ":db/ident"}
  (d/entity db :chuck-norris)



  )

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(def install-txes
  [[(ds/to-one :myapp.entity/type :index)
    (ds/named :myapp.entity.types/person)

    (ds/attr :person/id :string :identity)
    (ds/attr :person/firstName :string)
    (ds/attr :person/lastName :string)
    (ds/attr :person/birth-date :instant)
    (ds/to-one :person/gender :index)
    (ds/named :person.gender/male)
    (ds/named :person.gender/female)
    (ds/to-many :person/is-friend-with :index)
    (ds/to-one :person/biological-father :index)
    (ds/to-one :person/biological-mother)]
   [{:person/id "john-doe"
     :person/firstName "John"
     :person/lastName "Doe"
     :person/birth-date #inst "1991-07-07"
     :person/gender :person.gender/male
     :person/is-friend-with [{:person/id "alice"}
                             {:person/id "chuck-norris"}]
     :person/biological-father {:person/id "chuch-norris"}
     :myapp.entity/type :myapp.entity.types/person}
    ;; has an ident
    {:db/ident :chuck-norris
     :person/id "chuck-norris"
     :person/firstName "Chuck"
     :person/lastName "Norris"
     :person/birth-date #inst "1940-03-10"
     :person/gender :person.gender/male
     :person/is-friend-with []
     :myapp.entity/type :myapp.entity.types/person}
    ;; doesn't have a last name
    {:person/id "alice"
     :person/firstName "Alice"
     :person/birth-date #inst "1993-07-08"
     :person/gender :person.gender/male
     :person/is-friend-with []
     :myapp.entity/type :myapp.entity.types/person}
    {:person/id "bob"
     :person/firstName "Bob"
     :person/lastName "'s your uncle"
     :person/birth-date #inst "1991-03-01"
     :person/gender :person.gender/male
     :person/is-friend-with [{:person/id "alice"}]
     :person/biological-father {:person/id "john-doe"}
     :person/biological-mother {:person/id "alice"}
     :myapp.entity/type :myapp.entity.types/person}]])

(defn get-db []
  (reduce (fn [db tx-data]
            (:db-after (d/with db tx-data)))
    (dm/empty-db) install-txes))

(defn compute-age
  [^Date birth-date, ^Date now]
  (let [today (doto (GregorianCalendar.)
                (.setTime now))
        bday (doto (GregorianCalendar.)
               (.setTime birth-date))
        bdayThisYear (doto (GregorianCalendar.)
                       (.setTime birth-date)
                       (.set Calendar/YEAR (.get bday Calendar/YEAR)))]
    (cond-> (- (.get today Calendar/YEAR) (.get bday Calendar/YEAR))
      (< (.getTimeInMillis today) (.getTimeInMillis bdayThisYear)) dec)))

(defn full-name [db eid [firstName lastName]]
  (when (and firstName lastName)
    (str firstName " " lastName)))

(defn friends [db eid _]
  (d/q '[:find [?friend ...] :in $ ?me :where
         (or-join [?me ?friend]
           [?me :person/is-friend-with ?friend]
           [?friend :person/is-friend-with ?me])]
    db eid))

(defn age [db ident [birth-date]]
  (memoize
    (fn [now]
      (when birth-date
        (compute-age birth-date now)))))

(defn myapp-ent-type
  [db eid _]
  (when-let [tid (:v (first (seq (d/datoms db :eavt :myapp.entity/type))))]
    (d/ident db tid)))

(defn person-uri-path
  [db eid [pid]]
  (str "/person/" pid))

(def schema
  [;; derived scalar attribute
   {:onodrim.attr/id :person/full-name
    :onodrim.attr/deps
    [:person/firstName :person/lastName]
    :onodrim.attr/compute-v #'full-name}
   ;; derived ref-typed attribute
   {:onodrim.attr/id :person/friends
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :onodrim.attr/compute-v #'friends}
   ;; getter
   {:onodrim.attr/id :person/age
    :onodrim.attr/deps
    [:person/birth-date]
    :onodrim.attr/compute-v #'age}
   {:onodrim.attr/id :person/parents
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :onodrim.attr/deps [:person/biological-father :person/biological-mother]
    :onodrim.attr/compute-v
    (fn [db eid parents]
      (println "parents" parents)
      (into #{} (comp (remove nil?) (map entity-id)) parents))}

   ;; overriden attribute - in order to get the ident
   {:onodrim.attr/id :myapp.entity/type
    :onodrim.attr/compute-v #'myapp-ent-type}

   ;; polymorphic attribute
   {:onodrim.attr/id :entity/uri-path
    :onodrim.attr.polymorphic/dispatch-attr :myapp.entity/type}

   ;; impl
   {:onodrim.attr.polymorphic.impl/for :entity/uri-path
    :onodrim.attr.polymorphic.impl/dispatch-value :myapp.entity.types/person
    :onodrim.attr.polymorphic.impl/attr :person/uri-path*}
   {:onodrim.attr/id :person/uri-path*
    :onodrim.attr/deps
    [:person/id]
    :onodrim.attr/compute-v #'person-uri-path}
   ])

(defn datomic-find-eid
  [db ident]
  (d/entid db ident))

(defn datomic-default
  [wrap-result db eid aid]
  (when (keyword? aid)
    (let [^String nm (name aid)]
      (if (str/starts-with? nm "_")
        (when-let [attr (d/attribute db (keyword (namespace aid) (.substring nm 1 (.length nm))))]
          (let [ref-typed? (-> attr :value-type (= :db.type/ref))]
            (when ref-typed?
              (when (:indexed attr)
                (let [vs (map :e (d/datoms db :avet (:id attr) eid))]
                  (when (seq vs)
                    (wrap-result true true vs)))))
            ))
        (when-let [attr (d/attribute db aid)]
          (let [vs (map :v (d/datoms db :eavt eid (:id attr)))]
            (when vs
              (let [many? (-> attr :cardinality (= :db.cardinality/many))
                    ref-typed? (-> attr :value-type (= :db.type/ref))]
                (wrap-result many? ref-typed?
                  (if many? vs (first vs)))))))))))

(defn datomic-find-keyset [db eid]
  (into #{}
    (map (fn [datom]
           (d/ident db
             (:a datom))))
    (seq (d/datoms db :eavt eid))))

(defn test-engine
  []
  (engine schema
    ;; IMPROVEMENT enhanced version which uses the dependency graph to returm the derived keys
    {:onodrim.engine/find-eid #'datomic-find-eid
     :onodrim.engine/default #'datomic-default
     :onodrim.engine/find-keyset #'datomic-find-keyset}))


(comment
  (def db (get-db))
  (def eng (test-engine))
  (def jd (entity eng db [:person/id "john-doe"]))

  jd
  (seq jd)
  (:person/id jd)
  (:person/friends jd)
  (map :person/id *1)
  (:person/parents jd)
  (:person/id (first *1))



  )

