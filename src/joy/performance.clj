(ns joy.performance)

;; HINTING

(set! *warn-on-reflection* true)

;; warn
(defn asum-sq [xs]
  (let [dbl (amap xs i ret
                  (* (aget xs i)
                     (aget xs i)))]
    (areduce dbl i ret 0
             (+ ret (aget dbl i)))))

;; 2300 millis
(time (dotimes [_ 10000] (asum-sq (double-array [1 2 3 4 5]))))


;; no warn
(defn asum-sq [^doubles xs]
  (let [^doubles dbl (amap xs i ret
                           (* (aget xs i)
                              (aget xs i)))]
    (areduce dbl i ret 0
             (+ ret (aget dbl i)))))

;; 4 millis
(time (dotimes [_ 10000] (asum-sq (double-array [1 2 3 4 5]))))


;; warn
(.intValue (asum-sq (double-array [1 2 3 4 5])))

(defn ^Double asum-sq [^doubles xs]
  (let [^doubles dbl (amap xs i ret
                           (* (aget xs i)
                              (aget xs i)))]
    (areduce dbl i ret 0
             (+ ret (aget dbl i)))))

;; no warn
(.intValue (asum-sq (double-array [1 2 3 4 5])))


;; TRANSIENTS

(defn zencat1 [x y]
  (loop [src y, ret x]
    (if (seq src)
      (recur (next src) (conj ret (first src)))
      ret)))

;; with transients
(defn zencat2 [x y]
  (loop [src y, ret (transient x)]
    (if (seq src)
      (recur (next src) (conj! ret (first src)))
      (persistent! ret))))

(time (dotimes [_ 1000000] (zencat1 [1 2] [3 4]))) ;; better
(time (dotimes [_ 1000000] (zencat2 [1 2] [3 4]))) ;; worse

(def bv (vec (range 1e7)))
(first (time (zencat1 bv bv))) ;; worse
(first (time (zencat2 bv bv))) ;; better

;; you cant modify transient across threads

;; CHANKED SEQUENCES

(def gimme #(do (print \.) %))
(take 1 (map gimme (range 40))) ;; 32 dots will appear

;; how to count 1 (not 32) at time
(defn seq1 [s]
  (lazy-seq
   (when-let [[x] (seq s)]
     (cons x (seq1 (rest s))))))

(take 1 (map gimme (seq1 (range 32)))) ;; 1 dot will appear

;; MEMOIZE DONE RIGHT

(defprotocol CacheProtocol
  (lookup [cache e])
  (has?   [cache e])
  (hit    [cache e])
  (miss   [cache e ret]))

(deftype BasicCache [cache]
  CacheProtocol
  (lookup [_ item]        (get cache item))
  (has?   [_ item]        (contains? cache item))
  (hit    [this item]     this)
  (miss   [_ item result] (BasicCache. (assoc cache item result))))

(defn through [cache f item]
  (if (has? cache item)
    (hit cache item)
    (miss cache item (delay (apply f item)))))

(deftype PluggableMemoization [f cache]
  CacheProtocol
  (lookup [_ item]        (lookup cache item))
  (has?   [_ item]        (has? cache item))
  (hit    [this item]     this)
  (miss   [_ item result] (PluggableMemoization. f (miss cache item result))))

(defn memoization-impl [cache-impl]
  (let [cache (atom cache-impl)]
    (with-meta
      (fn [& args]
        (let [cs (swap! cache through (.f cache-impl) args)]
          @(lookup cs args)))
      {:cache cache})))

(def slowly (fn [x] (Thread/sleep 3000) x))
(def sometimes-slowly (memoization-impl
                       (PluggableMemoization.
                        slowly
                        (BasicCache. {}))))

(time [(sometimes-slowly 108) (sometimes-slowly 108)])

;; COERCION
(defn factorial [original-x]
  (loop [x original-x, acc 1]
    (if (>= 1 x)
      acc
      (recur (dec x) (* x acc)))))
(time (dotimes [_ 1e5] (factorial 20)))

;; only long and double are suitable
(defn factorial [original-x]
  (loop [x (long original-x), acc 1]
    (if (>= 1 x)
      acc
      (recur (dec x) (* x acc)))))
;; ==
(defn factorial [^long original-x]
  (loop [x original-x, acc 1]
    (if (>= 1 x)
      acc
      (recur (dec x) (* x acc)))))

(time (dotimes [_ 1e5] (factorial 20)))

;; do not check for overflow
(set! *unchecked-math* true)
(defn factorial [^long original-x]
  (loop [x original-x, acc 1]
    (if (>= 1 x)
      acc
      (recur (dec x) (* x acc)))))
(set! *unchecked-math* false)

(time (dotimes [_ 1e5] (factorial 20)))

;; but it will get wrong answer for 21 (oferflowed)
;; so try to fix with double
(defn factorial-e [^double original-x]
  (loop [x original-x, acc 1.0]
    (if (>= 1.0 x)
      acc
      (recur (dec x) (* x acc)))))
;; it's as fast as with *unchecked-math*, but less accurate
(time (dotimes [_ 1e5] (factorial-e 20.0)))


;; with auto-promotion
(defn factorial-f [^long original-x]
  (loop [x original-x, acc 1]
    (if (>= 1 x)
      acc
      (recur (dec x) (*' x acc)))))

(time (dotimes [_ 1e5] (factorial-f 21.0)))
