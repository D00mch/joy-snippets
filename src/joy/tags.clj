(ns joy.tags
  (:require [joy.utils :as utils]
            [clojure.edn :as edn]))

;; data-readers is mapped in src/data_reader.clj
(def distance-reader
  (partial utils/convert
           {:m  1
            :km 1000
            :cm 1/100
            :mm [1/10 :cm]}))


#unit/length [12 :km]

;; bind data-reader dynamically
(def time-reader
  (partial utils/convert
           {:sec 1
            :min 60
            :hr  [60 :min]
            :day [24 :hr]}))

(binding [*data-readers* {'unit/time #'joy.tags/time-reader}]
  (read-string "#unit/time [1 :min 30 :sec]"))

;; default
(binding [*default-data-reader-fn* #(-> {:tag %1 :payload %2})]
  (read-string "#nope doesn't-exist"))


;; EDN

(edn/read-string "{:a 3, [:c] 9}")

(def T {'unit/time #'joy.tags/time-reader})

(edn/read-string {:readers T} "#unit/time [1 :min 45 :sec]")

(edn/read-string {:readers T, :default vector} "#unit/nime ")
