(ns joy.patterns)

;; OBSERVER
(defmacro defformula [nm bindings & formula]
  `(let ~bindings
     (let [formula#   (agent ~@formula)
           update-fn# (fn [key# ref# o# n#]
                        (send formula# (fn [_#] ~@formula)))]
       (doseq [r# ~(vec (map bindings
                             (range 0 (count bindings) 2)))]
         (add-watch r# :update-formula update-fn#))
       (def ~nm formula#))))

(def h  (ref 25))
(def ab (ref 100))

(defformula avg
  [at-bats ab, hits h]
  (float (/ @hits @at-bats)))

@avg
(dosync (ref-set h 33))
@avg

;; STRATEGY
;; multimethod

;; ABSTRACT FACTORY

(def config
  '{:systems {:pump {:type :feeder, :descr "Feeder system"}
              :sim1 {:type :sim,
                     :fidelity :low}
              :sim2 {:type :sim,
                     :fidelity :high, :threads 2}}})

(defn describe-system [name cfg]
  [(:type cfg) (:fidelity cfg)])

(describe-system :pump {:type :feeder, :descr "Feeder system"})

(defmulti construct describe-system)

(defmethod construct :default [name cfg]
  {:name name
   :type (:type cfg)})

(defn construct-subsystems [sys-map]
  (for [[name cfg] sys-map]
    (construct name cfg)))

(construct-subsystems (:systems config))

(defmethod construct [:feeder nil]
  [_ cfg]
  (:descr cfg))

(construct-subsystems (:systems config))

(defrecord LowFiSim [name])
(defrecord HiFiSim [name threads])

(defmethod construct [:sim :low]
  [name cfg]
  (->LowFiSim name))
(defmethod construct [:sim :high]
  [name cfg]
  (->HiFiSim name (:threads cfg)))

(construct-subsystems (:systems config))

;; DI

(def lofi {:type :sim, :descr "Lowfi sim", :fidelity :low})
(def hifi {:type :sim, :descr "Hifi sim", :fidelity :high, :threads 2})

(construct :lofi lofi)

(defprotocol Sys
  (start! [sys])
  (stop!  [sys]))

(defprotocol Sim
  (handle [sim msg]))

(defn build-system [name config]
  (let [sys (construct name config)]
    (start! sys)
    sys))

(extend-type LowFiSim
  Sys
  (start! [this]
    (prn "Started a lofi simulator."))
  (stop!  [this]
    (prn "Stopped a lofi simulator."))
  Sim
  (handle [this msg]
    (* (:weight msg) 3.14)))
