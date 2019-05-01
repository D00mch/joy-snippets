(ns joy.utils
  (:import java.util.concurrent.Executors))

(defn convert [context descriptor]
  (reduce (fn [result [mag unit]]
            (+ result
               (let [val (get context unit)]
                 (if (vector? val)
                   (* mag (convert context val))
                   (* mag val)))))
          0
          (partition 2 descriptor)))

(def thread-pool
  (Executors/newFixedThreadPool
   (+ 2 (. (Runtime/getRuntime) availableProcessors))))

(defn dothreads!
  [f & {thread-count :threads
        exec-count   :times
        :or {thread-count 1, exec-count 1}}]
  (dotimes [t thread-count]
    (.submit thread-pool
             #(dotimes [_ exec-count] (f)))))

(defn neighbors
  ([size yx]
   (neighbors [[-1 0] [1 0] [0 -1] [0 1]]
              size
              yx))
  ([deltas size yx]
   (filter (fn [new-yx] (every? #(< -1 % size) new-yx))
           (map #(vec (map + yx %))
                deltas))))

;; rand

(def ascii (map char (range 65 (+ 65 26))))

(defn rand-str [sz alphabet]
  (apply str (repeatedly sz #(rand-nth alphabet))))

(def rand-sym #(symbol  (rand-str %1 %2)))
(def rand-key #(keyword (rand-str  %1 %2)))

(defn rand-vec [& generators]
  (into [] (map #(%) generators)))

(defn rand-map [sz kgen vgen]
  (into {}
        (repeatedly sz #(rand-vec kgen vgen))))

;; collections, position

(defn- index [coll]
  (cond
    (map? coll) (seq coll)
    (set? coll) (map vector coll coll)
    :else (map vector (iterate inc 0) coll)))

(defn pos [pred coll]
  (for [[i v] (index coll)
        :when (pred v)]
    i))
