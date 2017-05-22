# Onodrim

This library provides an abstraction similar to the Datomic Entity API, but enhanced with additional capabilities:

 * derived data
 * allow for other source of truths than Datomic Database values
 * allow for other identifiers than Datomic idents
 * allow for other value types than what Datomic can store
 * polymorphism - TODO does this need to be provided by a library, or should the implement it on top of derived data using multimethods / protocols ?
 * getters (i.e keys with arguments) - TODO is this really such a good idea ? (Val, 20 May 2017)

Helps you put a healthy indirection between your business logic and the schema of your Datomic storage, which yields better evolvability.

Derived data is often a necessity for business logic, but the business logic usually needn't know the difference between what information is derived
and what information is stored.
 
Tools like GraphQL implementations / server-side Om Next parsers can help you offer derived data, but they will typically make it available
at the boundary between your server and your clients, i.e in a place where only your clients can leverage it.
But it can be useful to make such derived data available at the *foundation* of your business logic; then both your serverside business logic
and your implementation client-serving tools can benefit from .

TODO is there such a difference between this and what a lib like Plumatic/Graph can offer, once you've taken Datomic out? 
  - yup, maybe polymorphism, (assuming you really need polymorphism to be implemented in this way).
  - maybe also getters, but you may implement those by putting memoized functions as values anyway
  - essentially, the distinction between scalar and ref values

TODO should derived ref attributes receive an entity or an eid ?

## Usage

FIXME

## TODO

* rename to Onodrim

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
