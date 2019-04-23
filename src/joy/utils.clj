(ns joy.utils
  (:import java.util.concurrent.Executors))

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
