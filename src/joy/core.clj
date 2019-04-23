(ns joy.core
  (:require #_[joy.collections :as col]
            [clojure.string :as str])
  (:gen-class))

(col/pos even? [1 2 3 4])

;; regex

(class #"example")

(java.util.regex.Pattern/compile "\\d")

(re-seq #"\w+" "one-two/three")

(re-seq #"\w*(\w)" "one-two/three")

;; keys, symbols

(identical? :name :name) ; true

(identical? 'name 'name) ; false
(= 'name 'name)          ; true

(let [x 'goat, y x]
  (identical? x y)) ; (= x y) --> true

(let [x (with-meta 'goat {:anal true})
      y (with-meta 'goat {:anal false})]
  [(= x y)
   (identical? x y)
   (meta x)
   (meta y)])

;; why :keywords
;; 1. always refer to themself
;; 2. act as functions (:key map) | symbols too
;; 3. like enum instances (identical?)

;; previous

;; seq as '(), destructuring, frame, doseq
;; (def make-list2+ #(list %1 %2 %&))

(defn print-seq [s]
  (when (seq s)
    (prn (first s))
    (recur (rest s))))

(def guys-name-map
  {:f-name "Guy" :m-name "Lewis" :l-name "Steele"})

(let [{f-name :f-name, m-name :m-name, l-name :l-name} guys-name-map]
  (str l-name ", " f-name " " m-name))

(let [{:keys [title f-name m-name l-name]
       :or {title "Mr."}} guys-name-map]
  (str title l-name ", " f-name " " m-name))

(let [{:keys [f-name m-name l-name] :as whole} guys-name-map]
  (str whole))

(defn whole-name [& args]
  (let [{:keys [f-name m-name l-name]} args]
    (str l-name ", " f-name " " m-name)))

(whole-name :f-name "Guy" :m-name "Lewis" :l-name "Steele")

(let [{first-thing 0 last-thing 3} [1 2 3 4]]
  (str "first " first-thing ", last " last-thing))

;; (comment
;;   (def ^:private width 256)
;;   (def frame (doto (java.awt.Frame.)
;;                (.setVisible true)
;;                (.setSize width width)))
;;   (def gfx (doto (.getGraphics frame)))

;;   (defn f-values [f xs ys]
;;     (for [x (range xs) y (range ys)]
;;       [x y (Math/abs (rem (f x y) 256))]))

;;   (defn clear [g] (.clearRect g 0 0 width width))

;;   (defn draw-values [f xs ys]
;;     (clear gfx)
;;     (.setSize frame (java.awt.Dimension. xs ys))
;;     (doseq [[x y v] (f-values f width width)]
;;       (doto gfx
;;         (.setColor (java.awt.Color. v v v))
;;         (.fillRect x y 1 1))))
;;   )

;; (draw-values * width width)

;; previous exercise
(defprotocol Concatenable
  (cat [this other]))

(extend-type String
  Concatenable
  (cat [this other]
    (.concat this other)))

(extend-type java.util.List
  Concatenable
  (cat [this other]
    (concat this other)))

(cat [1 2 3] [4 5 6])

(instance? java.util.List [1 2])

;; main


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
