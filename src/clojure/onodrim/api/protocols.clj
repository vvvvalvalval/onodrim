(ns onodrim.api.protocols)

;; IMPROVEMENT move to Java interface ? (Val, 20 May 2017)
(defprotocol OnoEntity
  (onoEngine [this])
  (onoDb [this])
  (onoEid [this]))
