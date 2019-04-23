(ns joy.collections)

(defn index [coll]
  (cond (map? coll) (seq coll)
        (set? coll) (map vector coll coll)
        :else (map vector (iterate inc 0) coll)))

(defn pos [pred coll]
  (for [[i v] (index coll) :when (pred v)] i))

(pos even? [1 2 3 4])
(pos #{1 2} {:a 1 :b 2})
(pos #{\a} "bob a d")

;; MAPS

;; insertion ordering
(seq (-> (array-map :a 1, :b 2, :c 3) (assoc :d 4)))

;; sorted
(sorted-map :b 2 :a 2)
(sorted-map-by > 1 \b, 2 \a)

(sorted-map-by #(compare (subs %1 1) (subs %2 1)) "bac" 1 "abc" 2)

;; sorted maps hadle numeric keys differently
(assoc {1 :int} 1.0 :float)            ;; hashMap has both
(assoc (sorted-map 1 :int) 1.0 :float) ;; {1 :float}
;; because
(compare 1.0 1) ;; 0


;; map.entries are vectors
(seq {:a 1, :b 2}) ;; -> ([:a 1] [:b 2])
(into {} '([:a 1] [:b 2]))
(sorted-map :a 1 :b 2)
(array-map :a 1 :b 2)
(apply hash-map [:a 1 :b 2])

(zipmap [:a :b :c] [1 2]) ;;  -> {:a 1, :b 2}

;; PERSISTENT SETS
(def my-set #{:a :b :c})
(my-set :c)              ; -> :c

(my-set :e)              ; -> nil
(get my-set :z :nothing) ; -> :nothing

(contains? my-set :a)    ; -> true

;; how to use contains for [] and '()
(some my-set [1 2 3 :c])


;; clojure.set

(clojure.set/intersection #{:a :b :c} #{:c :d :e} #{:a :c})
(clojure.set/union #{1 2 3} #{4 5} #{1 3 5 6})
(clojure.set/difference #{1 2 3 4} #{3 4 5}) ;; -> {1 2}, like substruction

;; sorted
(def my-sort-set (sorted-set 4 2 3))
(conj my-sort-set 5 1)

;; (sorted-set :a 1) ;; ClassCastException


;; PERSISTANT QUEUES

(defmethod print-method clojure.lang.PersistentQueue
  [q w]
  (print-method '<- w)
  (print-method (seq q) w)
  (print-method '-< w))

(def my-q (-> (clojure.lang.PersistentQueue/EMPTY)
              (conj 0 1 2 3 4)))

(def schedule
  (conj clojure.lang.PersistentQueue/EMPTY
        :wake-up :shower :brush-teeth))

(pop schedule)
(peek schedule)

(next schedule)

;; LISTS
;; basically for holding code

;; right way to add item at the start of a list is not cons (wich returns seq)
(conj '(1 2) 0) ;; -> list of 0 1 2

;; VECTORS

;; are not good for:
;; - inserting, removing in any position
;; - contains? -- return index
(contains? [1 2 3] 0) ;; -> true

(conj [1 2] 0)

;; vec as map entries
(vector? (first {:a 1}))
(let [[key val] (first {:a 1})]
  [key val])

;; subvectors
(def a-to-j  [\a \b \c \d \e \f \g \h \i \j])
(subvec a-to-j 3 6)

;; vec instead of reverse

;; as in all the lisps
(defn strict-map1 [f coll]
  (loop [remains coll, acc nil]
    (if (empty? remains)
      (reverse acc) ;(need reverse)
      (recur (rest remains)
             (cons (f (first remains)) ;; fix by using conj (not cons)
                   acc)))))


;; vectors as stacks

(def my-stack [1 2 3])

(peek my-stack) ;; last item
(pop my-stack)  ;; like drop last, returns [1 2]


;; if algorithm involved calls for a stack, use
;; - conj not assoc,
;; - peek not last (less efficient with vectors - O(n)),
;; - pop  not dissoc

;; structural sharing
(def v [1 2 3])
(assoc v 0 "adding string instead of 1")

(replace {2 "change 2 to this str", 3 10} v)

(def matrix [[1 2 3]
             [4 5 6]
             [7 8 9]])

(get-in matrix [1 2])          ;; -> 6
(assoc-in  matrix [1 2] 'x)    ;; replace 6 with 'x
(update-in matrix [1 2] * 100) ;; replace 6 with 600

(map vector [1 2 3])

(into (vector-of :int) [Math/PI 2 1.3])

;; reverse order
(rseq v) ;; -> (3 2 1)

;; getting neighbors in matrix

(defn neighbors
  ([size yx]
   (neighbors [[-1 0] [1 0] [0 -1] [0 1]]
              size
              yx))
  ([deltas size yx]
   (filter (fn [new-yx] (every? #(< -1 % size) new-yx))
           (map #(vec (map + yx %))
                deltas))))

(map #(get-in matrix %) (neighbors 3 [1 1]))


;; getting values
(nth v 0 :nothing)
(get v 0 :nothing)
(v 0)

;;(nth [] 0)  ;; throws IndexOutOfBound
(nth nil 0) ;; -> nil

(get [] 0)  ;; -> nil
(get nil 0) ;; -> nil

;;(nil 0)     ;; throws IllegalArg


;; JAVA ARRAYS
(def ds (into-array [:w :b :a]))
(seq ds)

(aset ds 1 :q)

(rest [1])

(= '(1 2)
   [1 2]
   (doto (java.util.ArrayList.)
     (.add 1)
     (.add 2)))

(seq (keys (hash-map :a 1)))
(class (seq (hash-map :a 1)))
