(ns onodrim.api
  (:require [onodrim.impl :as i]
            [onodrim.impl.protocols :as op]
            [onodrim.api.protocols :as p]))

(defn engine
  [attrs
   {:as opts
    find-eid :onodrim.engine/find-eid
    find-default :onodrim.engine/default
    find-keyset :onodrim.engine/find-keyset}]
  (i/->EngineImpl
    find-eid find-default find-keyset
    (Object.)
    (transduce
      (comp
        (filter :onodrim.attr/id)
        (map (fn [attr-m]
               (i/->DerivedAttrImpl
                 (-> attr-m :onodrim.attr/id)
                 (-> attr-m :db/valueType (= :db.type/ref))
                 (-> attr-m :db/cardinality (= :db.cardinality/many))
                 (-> attr-m :onodrim.attr.polymorphic/dispatch-attr some?)
                 (-> attr-m :onodrim.attr.polymorphic/dispatch-attr)
                 (-> attr-m :onodrim.attr/deps vec)
                 (-> attr-m :onodrim.attr/compute-v)))))
      (completing #(assoc %1 (op/attr-id %2) %2)) {} attrs)
    (->> attrs
      (filter :onodrim.attr.polymorphic.impl/for)
      (map (fn [m]
             [(:onodrim.attr.polymorphic.impl/for m)
              [(:onodrim.attr.polymorphic.impl/dispatch-value m)
               (:onodrim.attr.polymorphic.impl/attr m)]]))
      (reduce (fn [ret [f [dv ca]]]
                (merge-with merge
                  ret
                  {f {dv ca}}))
        {}))))

(defn entity
  [engine db ident]
  (when-let [eid (op/find-eid engine db ident)]
    (op/entity engine db eid)))

(defn entity-engine
  [e]
  (p/onoEngine e))

(defn entity-db
  [e]
  (p/onoDb e))

(defn entity-id
  [e]
  (p/onoEid e))

