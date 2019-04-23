(ns joy.concurrency
  (:require [joy.utils :as utils])
  (:import java.util.concurrent.Executors))

;;                Ref Agent Atom Var
;; coordinated    +
;; asynchronous         +
;; retrieble      +           +
;; thread-local                    +

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

;(dothreads! #(pr "Hi ") :threads 2 :times 2)


;; using refs for a mutable game board


(def initial-board
  [[:- :k :-]
   [:- :- :-]
   [:- :K :-]])

(defn board-map [f board]
  (vec (map #(vec (for [s %] (f s)))
            board)))

(defn reset-board! []
  (def board (board-map ref initial-board))
  (def to-move (ref [[:K [2 1]] [:k [0 1]]]))
  (def num-moves (ref 0)))

(def king-moves
  (partial utils/neighbors
           [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]] 3))

(defn good-move?
  [to enemy-sq]
  (when (not= to enemy-sq)
    to))

(defn choose-move [[[mover mpos] [_ enemy-pos]]]
  [mover (some #(good-move? % enemy-pos)
               (shuffle (king-moves mpos)))])

;; (reset-board!)
;; (take 5 (repeatedly #(choose-move @to-move)))

(defn place [from to] to)

(defn move-piece [[piece dest] [[_ src] _]]
  (commute (get-in board dest) place piece)
  (commute (get-in board src)  place :-)
  (commute num-moves inc))

(defn update-to-move [move]
  (alter to-move #(vector (second %) move)))

(defn make-move []
  (dosync
   (let [move (choose-move @to-move)]
     (move-piece move @to-move)
     (update-to-move move))))

;; (reset-board!)
;; (dothreads! make-move :threads 100 :times 100)
;; (board-map deref board)

;; WHEN NOT TO USE TRANSACTIONS (DOSYNC)
;; I/O, use (io! ...) instead
;; object mutations (as it's not idempotent)


;; don't mix both long- and short-running transactions

(defn stress-ref [r]
  (let [slow-tries (atom 0)]
    (future
      (dosync
       (swap! slow-tries inc)
       (Thread/sleep 200)
       @r)
      (prn (format "r is: %s, history: %d, after: %d tries"
                   @r (.getHistoryCount r) @slow-tries)))

    (dotimes [i 500]
      (Thread/sleep 10)
      (dosync (alter r inc)))
    :done))

;; tweak history to increase tried count of long-running op
;; (stress-ref (ref 0 :max-history 30 :min-history 15))

;; AGENT

(defn exercise-agents [send-fn]
  (let [agents (map #(agent %) (range 24))]
    (doseq [a agents]
      (send-fn a (fn [_] (Thread/sleep 100))))
    (doseq [a agents]
      (await a))))

;; (time (exercise-agents send-off)) ;; use it for IO
;; (time (exercise-agents send))     ;; don't use for blocking tasks

;; :error mode
(def log-agent (agent []))
;; (send log-agent (fn [] 2000))
;; @log-agent
;; (agent-error log-agent)
;; (restart-agent log-agent 2500 :clear-actions true)

;; :continue mode
(defn handle-log-error [the-agent the-err]
  (prn "An action send to the log-agent threw " the-err))
(set-error-handler! log-agent handle-log-error)
(set-error-mode! log-agent :continue)

;; ATOM
(defn manipulable-memoize [function]
  (let [cache (atom {})]
    (with-meta
      (fn [& args]
        (or (get @cache args)
            (let [ret (apply function args)]
              (swap! cache assoc args ret)
              ret)))
      {:cache cache})))

(def slowly (fn [x] (Thread/sleep 1000) x))
;; (time [(slowly 9) (slowly 9)])

(def mem-slowly (manipulable-memoize slowly))
;; (time [(mem-slowly 9) (mem-slowly 9)])

;; (let [cache (:cache (meta mem-slowly))]
;;   (swap! cache dissoc '(9)))

;; WHEN TO USE LOCKS

(defprotocol SafeArray
  (saset  [this i f])
  (saget  [this i])
  (sasize [this])
  (saseq  [this]))

(defn pummel [a]
  (dothreads! #(dotimes [i (sasize a)]
                 (saset a i inc))
              :threads 100))

;; implement SafeArray with "locking" (synchronize)

(defn make-safe-array [t sz]
  (let [a (make-array t sz)]
    (reify
      SafeArray
      (sasize [_] (count a))
      (saseq  [_] (seq a))
      (saget  [_ i]
        (locking a
          (aget a i)))
      (saset [this i f]
        (locking a
          (aset a i (f (saget this i))))))))

(def A (make-safe-array Integer/TYPE 32))
;;(pummel A)
;; (saseq A)

;; implement SafeArray with reentrant lock

(defn lock-i [target-index num-locks]
  (mod target-index num-locks))

(import 'java.util.concurrent.locks.ReentrantReadWriteLock)

(defn make-smart-array [t sz]
  (let [a   (make-array t sz)
        Lsz (/ sz 2)
        L   (into-array (take Lsz (repeatedly #(ReentrantReadWriteLock.))))]
    (reify
      SafeArray
      (sasize [_] (count a))
      (saseq  [_] (seq a))
      (saget  [_ i]
        (let [lk (aget L (lock-i (inc i) Lsz))]
          (.lock lk)
          (try
            (aget a i)
            (finally (.unlock lk)))))
      (saset [this i f]
        (let [lk (aget L (lock-i (inc i) Lsz))]
          (.lock lk)
          (try
            (aset a i (f (saget this i)))
            (finally (.unlock lk))))))))

(def S (make-smart-array Integer/TYPE 32))
;;(pummel S)
;; (saseq S)


;; VARS


#'S
(var S)

;; (/ 10M 3) ;;=> java.lang.ArithmeticException

(with-precision 5
  (/ 10M 3))

;; (with-precision 4
;;   (map #(/ % 3) (range 1M 4))) ;;=> java.lang.ArithmeticException

(with-precision 4
  (doall (map #(/ % 3) (range 1M 4))))

(with-precision 4
  (map (bound-fn [x] (/ x 3)) (range 1M 4)))

(try
  (map (bound-fn [x] (/ x 3)) (range 1M 3))
  (catch Exception e
    (prn e)))


;; bindigns are thread-local in java-sense
