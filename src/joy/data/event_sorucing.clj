(ns joy.data.event-sorucing
  (:require [joy.utils :refer [rand-map]]))

(defn valid? [event]
  (boolean (:result event)))

;; (valid? {}) ;; false
;; (valid? {:result 43}) ;; true

(defn effect [{:keys [ab h] :or {ab 0, h 0}}
              event]
  (let [ab  (inc ab)
        h   (if (= :hit (:result event))
              (inc h)
              h)
        avg (double (/ h ab))]
    {:ab ab, :h h, :avg avg}))

;(effect {:ab 599, :h 180} {:result :out})

(defn apply-effect [state event]
  (if (valid? event)
    (effect state event)
    state))

(def effect-all #(reduce apply-effect %1 %2))

(def events
  (repeatedly
   100
   (fn []
     (rand-map 1
               #(-> :result)
               #(if (< (rand-int 10) 3)
                  :hit
                  :out)))))

(def fx-timeline #(reductions apply-effect %1 %2))

