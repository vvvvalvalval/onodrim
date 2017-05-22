(ns onodrim.impl
  (:require [onodrim.impl.protocols :as op]
            [onodrim.api.protocols :as p])
  (:import (java.util.concurrent ConcurrentMap ConcurrentHashMap)
           (clojure.lang ILookup Associative IPersistentCollection Seqable MapEntry IMapEntry)))

;; ------------------------------------------------------------------------------
;; engine impl

(defrecord WrappedResult
  [many ref-typed? result])

(defn lookup-coerce-v
  [engine db ref-typed? many? v]
  (if ref-typed?
    (if many?
      (when (seq v)
        (map #(op/entity engine db %) v))
      (when v
        (op/entity engine db v)))
    (if many? (seq v) v)))

;; IMPROVEMENT distinguish missing value and unknown attribute
(defn lookup-raw
  [engine db eid
   ^ConcurrentMap cache
   attr-id]
  ;; FIXME coerce string to keyword (externally because this one recurses)
  ;; FIXME :db/ident
  (let [hit (.get cache attr-id)]
    (if-not (nil? hit)
      ;; cache hit
      (if (identical? hit (op/missing engine))
        nil
        hit)
      ;; cache miss
      (let
        [ret
         (if-let [derived-attr (op/find-derived-attr engine attr-id)]
           (let [v (if-let [dispatch-attr-id (op/dispatch-attr derived-attr)]
                     ;; NOTE not quite sure we want to return nil when dispatch-v is nil (Val, 20 May 2017)
                     (when-let [dispatch-v (lookup-raw engine db eid cache dispatch-attr-id)]
                       (if-let [concrete-attr-key (op/find-impl-attr engine attr-id dispatch-v)]
                         (lookup-raw engine db eid cache concrete-attr-key)
                         (throw (ex-info (str "No concrete attribute registered for implementing " attr-id
                                           " with dispatch value " dispatch-v
                                           " for dispatch attr " dispatch-attr-id)
                                  {:polymorphic-attr attr-id
                                   :dispatch-attr dispatch-attr-id
                                   :dispatch-value dispatch-v}))))
                     (let [deps (into []
                                  (map #(lookup-raw engine db eid cache %))
                                  (op/deps derived-attr))]
                       (op/compute-v derived-attr db eid deps)))
                 ref-typed? (op/ref? derived-attr)
                 many (op/many? derived-attr)]
             (lookup-coerce-v engine db ref-typed? many v))
           ;; falling back to default
           (let [{:keys [many ref-typed? result]} (op/lookup-default engine ->WrappedResult
                                                db eid attr-id)]
             (lookup-coerce-v engine db ref-typed? many result)))
         cache-v (if (some? ret) ret (op/missing engine))]
        (.put cache attr-id cache-v)
        ret))))

(defn lookup-key
  [engine db eid
   ^ConcurrentMap cache
   key]
  (if (= key :db/id)
    eid
    (if-let [attr-id
             (cond
               (keyword? key) key

               (string? key)
               (if-let [[_ ns nm] (re-matches #":([^/]*)/?(.+)" key)]
                 (keyword ns nm)
                 (throw (IllegalArgumentException. (str "Cannot coerce string key to keyword: " key))))

               (symbol? key)
               (keyword (namespace key) (name key))
               )]
      (lookup-raw engine db eid cache attr-id)
      nil)))

(defn keyset
  [engine db eid]
  (op/find-keyset engine db eid))

(defn ^IMapEntry map-entry
  [k v]
  ;; HACK relying on clojure.lang concrete type - not part of the API
  (MapEntry. k v))

(deftype DatpullEntity
  [engine db eid cache]
  ;; TODO cannot really support Entity as returning a db value may make no sense ?
  ;Entity
  ;(get [this key]
  ;  )

  p/OnoEntity
  (onoEngine [this] engine)
  (onoDb [this] db)
  (onoEid [this] eid)


  ILookup
  (valAt [this key]
    (.valAt this key nil))
  (valAt [this key not-found]
    (let [v (lookup-key engine db eid cache key)]
      (if (nil? v)
        not-found
        v)))

  Associative
  (assoc [this k v]
    (throw (UnsupportedOperationException.)))
  (entryAt [this k]
    (let [v (lookup-key engine db eid cache k)]
      (if (nil? v)
        nil
        (map-entry k v))))

  IPersistentCollection
  (count [this]
    (count (keyset engine db eid)))
  (empty [this]
    (op/entity engine db eid))
  (equiv [this o]
    (.equals this o))

  Seqable
  (seq [this]
    (map #(.entryAt this %) (keyset engine db eid)))

  Object
  (equals [this other]
    (cond
      (nil? other)
      false

      (not (instance? DatpullEntity other))
      false

      (and
        (= eid (:eid other))
        (= db (:db other))
        (= engine (:engine other)))
      true
      ))
  ;; IMPROVEMENT needs better performance, may see how caching's done for Clojure persistent collections
  (hashCode [this]
    (hash [engine db eid]))
  ;; FIXME show cached keys ?
  (toString [this]
    (str
      (into {} (seq this))))
  )

;; ------------------------------------------------------------------------------
;; engine impl

(defrecord EngineImpl
  [findEid find-default findKeyset
   MISSING
   by-id
   dispatch-tables]
  op/Engine
  (find-eid [this db ident]
    (findEid db ident))
  (entity [this db eid]
    (->DatpullEntity this db eid
      (ConcurrentHashMap. 8 0.9 1)))
  (find-derived-attr [this attr-id]
    (get by-id attr-id))
  (find-impl-attr [this attr-id dispatch-v]
    (-> dispatch-tables (get attr-id) (get dispatch-v)))
  (lookup-default [this wrap-result db eid attr-id]
    (find-default wrap-result db eid attr-id))
  (find-keyset [this db eid]
    (findKeyset db eid))
  (missing [this] MISSING))

(defrecord DerivedAttrImpl
  [attrId
   isRef isMany
   isPolymorphic dispatchAttr
   deps computeV]
  op/DerivedAttr
  (attr-id [this] attrId)
  (ref? [this] isRef)
  (many? [this] isMany)
  (polymorphic? [this] isPolymorphic)
  (dispatch-attr [this] dispatchAttr)
  (deps [this] deps)
  (compute-v [this db eid deps]
    (computeV db eid deps)))
