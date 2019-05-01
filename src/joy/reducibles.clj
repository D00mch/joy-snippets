(ns joy.reducibles
  (:require [clojure.core.reducers :as r]))

;; + performance
;; - not-lazy

(defn empty-range? [start end step]
  (or (and (pos? step) (>= start end))
      (and (neg? step) (<= start end))))

(defn reducible-range [start end step]
  (fn [reducible-fn init]
    (loop [result init, i start]
      (if (empty-range? i end step)
        result
        (recur (reducible-fn result i)
               (+ i step))))))

(defn half [x] (/ x 2))

;; not flexible
(defn sum-half [result input]
  (+ result (half input)))

(reduce sum-half (range 0 10 2)) ;;==
((reducible-range 0 10 2) sum-half 0)

;; more flexible
(defn half-transformer [f1]
  (fn f1-half [result input]
    (f1 result (half input))))

((reducible-range 0 10 2) (half-transformer +) 0)
((reducible-range 0 10 2) (half-transformer conj) [])

;; more flexible
(defn mapping [map-fn]
  (fn map-transformer [f1]
    (fn [result input]
      (f1 result (map-fn input)))))

((reducible-range 0 10 2) ((mapping half) +) 0)
((reducible-range 0 10 2) ((mapping half) conj) [])
((reducible-range 0 10 2) ((mapping list) conj) [])

;; so why? not this
(->> (range 0 10 2) (map half) (reduce + 0))

;; ...

(defn filtering [filter-pred]
  (fn [f1]
    (fn [result input]
      (if (filter-pred input)
        (f1 result input)
        result))))

((reducible-range 0 10 2) ((filtering #(not= % 2)) +) 0)
((reducible-range 0 10 2) ((filtering #(not= % 2)) conj) [])

;; chaining transformers
((reducible-range 0 10 2)
 ((filtering #(not= % 2))
  ((mapping half) conj))
 [])

(defn mapcatting [map-fn]
  (fn [f1]
    (fn [result input]
      (let [reducible (map-fn input)]
        (reducible f1 result)))))

(defn and-plus-ten [x]
  (reducible-range x (+ 11 x) 10))

((reducible-range 0 10 2) ((mapcatting and-plus-ten) conj) [])

;; fixing syntax

;; transformers
(defn r-map [mapping-fn reducible]
  (fn new-reducible [reducing-fn init]
    (reducible ((mapping mapping-fn) reducing-fn) init)))

(defn r-filter [filter-pred reducible]
  (fn new-reducible [reducing-fn init]
    (reducible ((filtering filter-pred) reducing-fn) init)))

((->> (reducible-range 0 10 2)
      (r-map half)
      (r-filter #(not= % 2)))
 conj
 [])

;; converting transformers to core reducibles
(require '[clojure.core.reducers :as r])

(defn core-r-map [mapping-fn core-reducible]
  (r/reducer core-reducible (mapping mapping-fn)))

(defn core-r-filter [filter-pred core-reducible]
  (r/reducer core-reducible (filtering filter-pred)))

(reduce conj []
        (core-r-filter #(not= % 2)
                       (core-r-map half [0 2 4 6])))

;; implementing a reducible-range via  the CollReduce protocol
(defn reduce-range [reducing-fn init, start end step]
  (loop [result init, i start]
    (if (empty-range? i end step)
      result
      (recur (reducing-fn result i)
             (+ i step)))))

(defn core-reducible-range [start end step]
  (reify clojure.core.protocols.CollReduce
    (coll-reduce [this reducing-fn init]
      (reduce-range reducing-fn init, start end step))
    (coll-reduce [this reducing-fn]
      (if (empty-range? start end step)
        (reducing-fn)
        (reduce-range reducing-fn start, (+ start step) end step)))))

(reduce conj []
        (core-r-filter #(not= % 2)
                       (core-r-map half
                                   (core-reducible-range 0 10 2))))

;; converting transformers to core foldables
(defn core-f-map [mapping-fn core-reducible]
  (r/folder core-reducible (mapping mapping-fn)))

(defn core-f-filter [filter-pred core-reducible]
  (r/folder core-reducible (filtering filter-pred)))

(r/fold +
        (core-f-filter #(not= % 2)
                       (core-f-map half
                                   [0 2 4 6 8])))
;; ==
(r/fold +
        (r/filter #(not= % 2)
                  (r/map half
                         [0 2 4 6 8])))

;; fold with no init could be work like this
(r/fold (fn ([] 100) ([a b] (+ a b)))
        (range 10))
;; ==
(r/fold (r/monoid + (constantly 100))
        (range 10))

(r/fold 512                           ; elements per piece of work
        (r/monoid + (constantly 100)) ; combining function
        +                             ; reducing function
        (range 10))

(r/fold 4
        (r/monoid into (constantly []))
        conj
        (range 10))

(->> (range 10)
     (r/filter even?)
     r/foldcat
     seq)

(def big-vector (vec (range 0 (* 10 1000 1000) 2)))

;; performance check
(time (r/fold + (core-f-filter even? (core-f-map half big-vector))))
(time (reduce + 0 (filter even? (map half big-vector))))
