(ns joy.interfaces)


(definterface ISliceable
  (slice [^long s ^long e])
  (^long sliceCount []))

(def dumb
  (reify ISliceable
    (slice [_ s e] [:empty])
    (sliceCount [_] 42)))

(.slice dumb 1 2)
(.sliceCount dumb)

;; EXTEND INTERFACE WITH PROTOCOL METHODS
(defprotocol Sliceable
  (slice [this s e])
  (sliceCount [this]))

(extend ISliceable
  Sliceable
  {:slice (fn [this s e] (.slice this s e))
   :sliceCount (fn [this] (.sliceCount this))})

(sliceCount dumb)
(slice dumb 0 0)
