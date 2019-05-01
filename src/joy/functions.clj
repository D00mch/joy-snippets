(ns joy.functions
  (:import [java.util ArrayList Collections Comparator]
           [java.util.concurrent FutureTask]))


(ancestors (class #()))


;; FUNCTIONS ARE COMPARATORS

(defn gimme [] (java.util.ArrayList. [1 2 3 4]))
(doto (gimme)
  (Collections/sort (Collections/reverseOrder)))

(doto (gimme)
  (Collections/sort
   (reify Comparator
     (compare [this l r]
       (cond (> l r) -1
             (< l r) 1
             :else   0)))))

;; and with function (all reverse sort)
(doto (gimme) (Collections/sort #(compare %1 %2)))
(doto (gimme) (Collections/sort >))
(doto (gimme) (Collections/sort (complement <)))

;; FUNCTIONS ARE RUNNABLES

(doto (Thread. #(do (Thread/sleep 1000)
                    (prn "h")))
  .start)

;; FUNCTIONS ARE CALLABLE

(let [f (FutureTask. #(do (Thread/sleep 1000) 42))]
  (.start (Thread. #(.run f)))
  (.get f))
