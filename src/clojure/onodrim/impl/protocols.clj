(ns onodrim.impl.protocols)

(defprotocol Engine
  (find-eid [this db ident] "given a database value and an identifier, returns either a normalized eid or nil if not found.")
  (entity [this db eid] "builds an entity with this engine")
  (find-derived-attr [this attr-id] "finds a derived attribute of id attr-id, or nil if there is none")
  (find-impl-attr [this attr-id dispatch-v] "given a polymorphic attribute id `attr-id`, finds the id of the concrete attribute matching a given dispatch value")
  (lookup-default [this wrap-result db eid attr-id] "generic function to lookup the value for a non-derived attribute")
  (find-keyset [engine db eid] "returns the set of attribute ids which are in the keyset")
  (missing [this] "A constant object which indicates a missing value in the cache (because we can't put nil)"))

(defprotocol DerivedAttr
  (attr-id [this] "Returns the id for this attribute, a keyword")
  (ref? [this] "whether this attribute is ref-typed")
  (many? [this] "whether this attribute is cardinalty-many")
  (polymorphic? [this] "whether this attribute is polymorphic")
  (dispatch-attr [this] "If this attribute is polymorphic, the dispatch attribute")
  (deps [this] "If this attribute is not polymorphic, the ids of the dependency attributes")
  (compute-v [this db eid deps]
    "If this attribute is not polymorphic, computes the value. Returns nil if no value. Returns an ident for ref-typed attributes."))
