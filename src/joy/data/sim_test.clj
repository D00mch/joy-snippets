(ns joy.data.sim-test
  (:require [joy.data.event-sorucing :as es]
            [joy.utils :refer [rand-map]]
            [clojure.set :as sql]))

(def PLAYERS #{{:player "Nick", :ability 32/100}
               {:player "Matt", :ability 26/100}
               {:player "Ryan", :ability 19/100}})

(defn lookup [db name]
  (first (sql/select
          #(= name (:player %))
          db)))

(defn update-stats [db event]
  (let [player  (lookup db (:player event))
        less-db (sql/difference db #{player})]
    (conj less-db
          (merge player (es/effect player event)))))

(defn commit-event [db event]
  (dosync (alter db update-stats event)))

;;(commit-event (ref PLAYERS) {:player "Nick", :result :hit})

(defn rand-event [{ability :ability}]
  (let [able (numerator ability)
        max  (denominator ability)]
    (rand-map 1
              #(-> :result)
              #(if (< (rand-int max) able)
                 :hit
                 :out))))

(defn rand-events [total player]
  (take total
        (repeatedly #(assoc (rand-event player)
                            :player
                            (:player player)))))

;; (rand-events 3 {:player "Nick", :ability 32/100})

(def agent-for-player
  (memoize
   (fn [player-name]
     (agent []))))

(defn feed [db event]
  (let [a (agent-for-player (:player event))]
    (send a
          (fn [state]
            (commit-event db event)
            (conj state event)))))

(defn feed-all [db events]
  (doseq [event events]
    (feed db event))
  db)

;; (let [db (ref PLAYERS)]
;;   (feed-all db (rand-events 100 {:player "Nick", :ability 32/100}))
;;   db)

;; (count @(agent-for-player "Nick"))

(defn simulate [total players]
  (let [events  (apply interleave
                        (for [player players]
                          (rand-events total player)))
        results (feed-all (ref players) events)]
    (apply await (map #(agent-for-player (:player %)) players))
    @results))

;; (simulate 100 PLAYERS)



