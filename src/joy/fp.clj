(ns joy.fp
  (:require [joy.utils :refer [neighbors]]))


(:val {:val 5 :L nil :R nil})


(defn xconj [t v]
  (cond (nil? t)       {:val v :L nil :R nil}
        (< v (:val t)) {:val (:val t)
                        :L   (xconj (:L t) v)
                        :R   (:R t)}
        :else          {:val (:val t)
                        :L   (:L t)
                        :R   (xconj (:R t) v)}))

(defn xseq [t]
  (when t
    (concat (xseq (:L t)) [(:val t)] (xseq (:R t)))))

(-> (xconj nil 5)
    (xconj 3)
    (xconj 2)
    (xconj 4)
    xseq)

(def tree1 (-> (xconj nil 5)
               (xconj 3)))
(def tree2 (xconj tree1 7))
(identical? (:L tree1) (:L tree2))


;; LAZY

;; rules
;; 1. use lazy-seq outer-most
;; 2. use rest, not next (as it's more lazy)
;; 3. prefer high-order functions when building lazy
;; 4. don't lose head


(defn lz-rec-step [s]
  (-> (if (seq s)
        [(first s) (lz-rec-step (rest s))]
        [])
      lazy-seq))

(lz-rec-step [1 2 3 4])

(dorun (lz-rec-step (range 2000000000)))

(def s (lz-rec-step (range 2000000000)))

(defn simple-range [i limit]
  (-> (when (< i limit) (cons i (simple-range (inc i) limit)))
      lazy-seq))

(defn prn-lazy [i]
  (-> (do
        (prn "creating item " i)
        (cons i (prn-lazy (inc i))))
      lazy-seq))

(class (iterate inc 1))

;; DELAY/FORCE

(defn defer-expensive [cheap expensive]
  (if-let [good-enough (force cheap)]
    good-enough
    (force expensive)))

(defer-expensive
  (delay nil)
  (delay (do (Thread/sleep 1000) :expensive)))

;; LAZY QUICKSORT

(defn rand-ints [n]
  (take n (repeatedly #(rand-int n))))

(defn sort-parts [work]
  (-> (loop [[part & parts] work]
        (if-let [[pivot & xs] (seq part)]
          (let [smaller? #(< % pivot)]
            (recur (list*
                    (filter smaller? xs)
                    pivot
                    (remove smaller? xs)
                    parts)))
          (when-let [[x & parts] parts]
            (cons x (sort-parts parts)))))
      lazy-seq))

(defn qsort [xs]
  (sort-parts (list xs)))


;; FP

;; composite type as function

(map [:a :b :c :d] #{0 2})

;; first class functions

(defn fnth [n]
  (apply comp
         (cons first
               (take (dec n) (repeat rest)))))

(defn fnth [n]
  (->> (take (dec n) (repeat rest))
       (cons first)
       (apply comp)))

((fnth 5) '[a b c d e])


;; function builders: comp, partial, complement


(map (comp keyword #(.toLowerCase %) name) '(a B C))

((partial + 5) 100 200)
;;==
(#(apply + 5 %&) 100 200)

((complement odd?) 2)
;;==
(#(not (odd? %)) 2)


;; functions as data


(.toString fnth)

(defn join
  {:test (fn [] (assert (= (join "," [1 2 3]) "1,2,3")))}
  [sep s]
  (apply str (interpose sep s)))

(use '[clojure.test :as t])
(t/run-tests)

;; functions as args
(sort [[1 2 3] [0 9999 4]])
(sort > '(1 2))

(sort-by str ["z" \a 1 'b])
(sort-by :age [{:age 99}, {:age 13}, {:age 7}])

(def plays [{:band "Burial", :plays 879, :loved 9}
            {:band "Eno", :plays 2333, :loved 15}
            {:band "Bill Evans", :plays 979, :loved 9}
            {:band "Magma", :plays 2665, :loved 31}])
(def sort-by-loved-ratio (partial sort-by #(/ (:plays %) (:loved %))))
(sort-by-loved-ratio plays)

(defn columns [column-names]
  (fn [row]
    (vec (map row column-names))))

(sort-by (columns [:plays :loved :band]) plays)

(defn keys-apply [f ks m]
  (let [only (select-keys m ks)]
    (zipmap (keys only)
            (map f (vals only)))))

(keys-apply #(.toUpperCase %) #{:band} (plays 0))

(defn manip-map [f ks m]
  (merge m (keys-apply f ks m)))

;; named args
(defn slope
  [& {:keys [p1 p2] :or {p1 [0 0] p2 [1 1]}}]
  (float (/ (- (p2 1) (p1 1))
            (- (p2 0) (p1 0)))))

(slope :p1 [4 15] :p2 [3 21])
(slope)

;; pre- and postconditions
(defn slope [p1 p2]
  ;; will throw java.lang.AssertionError
  {:pre  [(not= p1 p2) (vector? p1) (vector? p2)]
   :post [(float? %)]}
  (/ (- (p2 1) (p1 1))
     (- (p2 0) (p1 0))))

(slope [3.0 4] [1 2])

;; decoupling assertions from functions
(defn put-things [m]
  (into m {:meat "beef" :veggie "broccoli"}))

(defn vegan-constrains [f m]
  {:pre  [(:veggie m)]
   :post [(:veggie %) (nil? (:meat %))]}
  (f m))

(vegan-constrains put-things {:veggie "carrot"})

;; CLOJURES
(def add-and-get
  (let [ai (java.util.concurrent.atomic.AtomicInteger.)]
    (fn [y] (.addAndGet ai y))))

(add-and-get 8)

(defn times-n [n]
  (fn [y] (* y n)))

((times-n 4) 3)

(def bearings [{:x  0 :y  1}   ; north
               {:x  1 :y  0}   ; east
               {:x  0 :y -1}   ; south
               {:x -1 :y  0}]) ; west

(defn forward [x y bearing-num]
  [(+ x (:x (bearings bearing-num)))
   (+ y (:y (bearings bearing-num)))])

(forward 5 5 0)

(defn bot [x y bearing-num]
  {:coords     [x y]
   :bearing    ([:north :east :south :west] bearing-num)
   :forward    (fn [] (bot (+ x (:x (bearings bearing-num)))
                           (+ y (:y (bearings bearing-num)))
                           bearing-num))
   :turn-right (fn [] (bot x y (mod (+ 1 bearing-num) 4)))
   :turn-left  (fn [] (bot x y (mod (- 1 bearing-num) 4)))})

(:bearing ((:forward ((:forward ((:turn-right (bot 5 5 0))))))))
(:coords ((:forward ((:forward ((:turn-right (bot 5 5 0))))))))

(defn mirror-bot [x y bearing-num] ;; polymorphism example
  {:coords     [x y]
   :bearing    ([:north :east :south :west] bearing-num)
   :forward    (fn [] (mirror-bot (- x (:x (bearings bearing-num)))
                                  (- y (:y (bearings bearing-num)))
                                  bearing-num))
   :turn-right (fn [] (mirror-bot x y (mod (- 1 bearing-num) 4)))
   :turn-left  (fn [] (mirror-bot x y (mod (+ 1 bearing-num) 4)))})

;; RECURSION

;; tail rec
(defn pow [base exp]
  (letfn [(kapow [base exp acc]
            (if (zero? exp)
              acc
              (recur base (dec exp) (* base acc))))]
    (kapow base exp 1)))

(pow 2 10)

;; or use lazy-seq to avoid stackOverflow

;; recursive units calculator


(partition 2 simple-metric)

(defn convert [context descriptor]
  (reduce (fn [result [mag unit]]
            (+ result
               (let [val (get context unit)]
                 (if (vector? val)
                   (* mag (convert context val))
                   (* mag val)))))
          0
          (partition 2 descriptor)))

(def simple-metric {:meter 1
                    :km    1000
                    :cm    1/100
                    :mm    [1/10 :cm]})

(convert simple-metric [3 :km 10 :meter 80 :cm 10 :mm])
(convert {:bit 1, :byte 8, :nibble [1/2 :byte]} [32 :nibble])


;; trampoline, mutually recursive


(defn elevator [commands]
  (letfn
   [(ff-open [[_ & r]]
      "When the elevator is open in the 1st floor
          it can either close or be done."
      #(case _
         :close (ff-closed r)
         :done  true
         false))
    (ff-closed [[_ & r]]
      "When the elevator is closed on the 1st floor
       it can either open or go up."
      #(case _
         :open (ff-open r)
         :up   (sf-closed r)
         false))
    (sf-closed [[_ & r]]
      "When the elevator is closed on the 2nd floor
       it can either go down or open."
      #(case _
         :down (ff-closed r)
         :open (sf-open r)
         false))
    (sf-open [[_ & r]]
      "When the elevator is open on the 2nd floor
       it can either close or be done"
      #(case _
         :close (sf-closed r)
         :done  true
         false))]
    (trampoline ff-open commands)))

(elevator [:close :up :open :close :down :open :done])
;; (elevator (cycle [:close :open])) ;; forever without stackoverflow



;; A*


(def world [[  1   1   1   1   1]
            [999 999 999 999   1]
            [  1   1   1   1   1]
            [  1 999 999 999 999]
            [  1   1   1   1   1]])

(defn estimate-cost [step-cost-est size x y]
  (-> (+ size size)
      (- y x 2)
      (* step-cost-est)))

(defn path-cost [node-cost cheapest-nbr]
  (-> (or (:cost cheapest-nbr) 0)
      (+ node-cost)))

(defn total-cost [newcost step-cost-est size x y]
  (-> (estimate-cost step-cost-est size y x)
      (+ newcost)))

(defn min-by [f coll]
  (when (seq coll)
    (reduce (fn [min other]
              (if (> (f min) (f other))
                other
                min))
            coll)))

(defn astar [start-yx step-est cell-costs]
  (let [size (count cell-costs)]
    (loop [steps 0
           routes (vec (repeat size (vec (repeat size nil))))
           work-todo (sorted-set [0 start-yx])]
      (if (empty? work-todo)
        [(peek (peek routes)) :steps steps]
        (let [[_ yx :as work-item] (first work-todo)
              rest-work-todo (disj work-todo work-item)
              nbr-yxs (neighbors size yx)
              cheapest-nbr (min-by :cost
                                   (keep #(get-in routes %)
                                         nbr-yxs))
              newcost (path-cost (get-in cell-costs yx)
                                 cheapest-nbr)
              oldcost (:cost (get-in routes yx))]
          (if (and oldcost (>= newcost oldcost))
            (recur (inc steps) routes rest-work-todo)
            (recur (inc steps)
                   (assoc-in routes yx
                             {:cost newcost
                              :yxs (conj (:yxs cheapest-nbr [])
                                         yx)})
                   (into rest-work-todo
                         (map
                          (fn [w]
                            (let [[y x] w]
                              [(total-cost newcost step-est size y x) w]))
                          nbr-yxs)))))))))

(astar [0 0] 900 [[  1 999]
                  [  1 222]])
