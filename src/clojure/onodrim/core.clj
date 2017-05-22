;; FIXME deprecated, remove
(ns onodrim.core
  (:require [clojure.string :as str]
            [onodrip.impl.protocols :as op])
  (:import (clojure.lang Associative ILookup IType IPersistentCollection Seqable IMapEntry MapEntry)
           (java.util.concurrent ConcurrentHashMap ConcurrentMap)
           (datomic Attribute Entity)))

;; FIXME
(defn puller
  [attrs
   {:as opts
    find-eid :onodrim.engine/find-eid
    find-default :onodrim.engine/default
    find-keyset :onodrim.engine/find-keyset}]
  (merge
    opts
    {:by-id
     (transduce
       (filter :onodrim.attr/id)
       (completing #(assoc %1 (:onodrim.attr/id %2) %2)) {} attrs)
     :dispatch-tables
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
         {}))
     :MISSING (Object.)}))

(defn entity-db
  [e])

(defn entity-ident
  [e])


