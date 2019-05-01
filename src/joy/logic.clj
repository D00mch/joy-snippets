(ns joy.logic
  (:require [clojure.set :as set]
            [joy.utils :as utils]
            [clojure.walk :as walk]))

;; SUDOKU WITH BRUTFORCE
(def b1 '[3 - - - - 5 - 1 -
          - 7 - - - 6 - 3 -
          1 - - - 9 - - - -
          7 - 8 - - - - 9 -
          9 - - 4 - 8 - - 2
          - 6 - - - - 5 - 1
          - - - - 4 - - - 6
          - 4 - 7 - - - 2 -
          - 2 - 6 - - - - 3])

(defn prep [board]
  (map #(partition 3 %)
       (partition 9 board)))

(defn print-board [board]
  (let [row-sep (apply str (repeat 25 "-"))]
    (println row-sep)
    (dotimes [row (count board)]
      (print "| ")
      (doseq [subrow (nth board row)]
        (doseq [cell (butlast subrow)]
          (print (str cell " ")))
        (print (str (last subrow) " | ")))
      (println)
      (when (zero? (mod (inc row) 3))
        (println row-sep)))))

;;(-> b1 prep print-board)

(defn rows [board sz]
  (partition sz board))
(defn row-for [board index sz]
  (nth (rows board sz) (/ index 9)))

(defn column-for [board index sz]
  (let [col (mod index sz)]
    (map #(nth % col)
         (rows board sz))))

(defn subgrid-for [board i]
  (let [rows    (rows board 9)
        sgcol   (/ (mod i 9) 3)
        sgrow   (/ (/ i 9) 3)
        grp-col (column-for (mapcat #(partition 3 %) rows) sgcol 3)
        grp     (take 3 (drop (* 3 (int sgrow)) grp-col))]
    (flatten grp)))

(defn numbers-present-for [board i]
  (set
   (concat (row-for board i 9)
           (column-for board i 9)
           (subgrid-for board i))))

(defn possible-placements [board index]
  (clojure.set/difference #{1 2 3 4 5 6 7 8 9}
                          (numbers-present-for board index)))

(possible-placements b1 1)

(defn solve [board]
  (if-let [[i & _]
           (and (some      '#{-} board)
                (utils/pos '#{-} board))]
    (flatten (map #(solve (assoc board i %))
                  (possible-placements board i)))
    board))

(-> b1 solve prep print-board time)


;; LOGIC

;; SATISFUCTION

(defn lvar? [x] ; logic variable?
  (boolean
   (when (symbol? x)
     (re-matches #"^\?.*" (name x)))))

(defn satisfy [l r knowledge]
  (let [L (get knowledge l l)
        R (get knowledge r r)]
    (cond
      (not knowledge)     nil
      (= L R)             knowledge
      (lvar? L)           (assoc knowledge L R)
      (lvar? R)           (assoc knowledge R L)
      (every? seq? [L R]) (satisfy (rest L)
                                   (rest R)
                                   (satisfy (first L)
                                            (first R)
                                            knowledge)))))
;; examples
(->> {}
     (satisfy '?x '?y)
     (satisfy '?x 1))

(satisfy '(1 2 3) '(1 ?something 3) {})
(satisfy '(?x 2 3 (4 5 ?z))
         '(1 2 ?y (4 5 6))
         {})

;; SUBSTITUTION

(require '[clojure.walk :as walk])

(defn subst [term binds]
  (walk/prewalk
   (fn [expr]
     (if (lvar? expr)
       (or (binds expr) expr)
       expr))
   term))

;; examples
(subst '(1 ?x 3) '{?x 2})
(subst '{:a ?x, :b [1 ?x 3]} '{?x 2})
(subst '(1 ?x 3) '{?x ?y})


;; beauty of using clojure.walk


(def page '[:html
            [:head [:title ?title]]
            [:body [:h1 ?title]]])

(subst page '{?title "Hi!"})

;; UNIFICATION
(defn meld [term1 term2]
  (->> {}
       (satisfy term1 term2)
       (subst term1)))

(meld '(1 ?x 3) '(1 2 ?y))
(meld '(1 ?x) '(?y (?y 2)))


;; CORE.LOGIC


(require '[clojure.core.logic :as logic])

(logic/run* [answer]
            (logic/== answer 5))

(logic/run* [v1 v2]
            (logic/== {:a v1, :b 2}
                      {:a 1,  :b v2}))

;; unification found one unknown var _0
(logic/run* [x y]
            (logic/== x y)) ;;=> ([_0 _0]) 

;; unification fails
(logic/run* [q]
            (logic/== q 1)
            (logic/== q 2)) ;;=> ()

;; different universes
(logic/run* [george]
            (logic/conde
             [(logic/== george :born)]
             [(logic/== george :unborn)]))

;; RELATIONS

(require '[clojure.core.logic.pldb :as pldb])

(pldb/db-rel orbits orbital body)

(def facts
  (pldb/db
   [orbits :mercury :sun]
   [orbits :venus :sun]
   [orbits :earth :sun]
   [orbits :mars :sun]
   [orbits :jupiter :sun]
   [orbits :saturn :sun]
   [orbits :uranus :sun]
   [orbits :neptune :sun]))

(pldb/with-db facts
  (logic/run* [q]
              (logic/fresh [orbital body]
                           (orbits orbital body)
                           (logic/== q orbital))))

;; subgoals
(pldb/db-rel stars star)

(def facts1 (-> facts (pldb/db-fact stars :sun)))

(defn planeto [body]
  (logic/fresh [star]
               (stars star)
               (orbits body star)))

(pldb/with-db facts1
  (logic/run* [q]
              (planeto :earth)
              (logic/== q true))) ;=> (true)

(pldb/with-db facts1
  (logic/run* [q]
              (planeto :sun)
              (logic/== q true))) ;=> ()

(pldb/with-db facts1
  (logic/run* [q]
              (logic/fresh [orbital]
                           (planeto orbital)
                           (logic/== q orbital)))) ;=> (*all-planets*)

(def facts2 (-> facts1 (pldb/db-facts [stars :alpha-centauri]
                                      [orbits :Bb :alpha-centauri])))

(pldb/with-db facts2
  (logic/run* [q]
              (logic/fresh [orbital]
                           (planeto orbital)
                           (logic/== q orbital)))) ;=> (*all-planets*)


(defn satelliteo [body]
  (logic/fresh [p]
               (orbits body p)
               (planeto p)))

(def facts3 (-> facts3 (pldb/db-facts [orbits :moon :earth]
                                      [orbits :phobos :mars]
                                      [orbits :deimos :mars]
                                      [orbits :io :jupiter]
                                      [orbits :europa :jupiter]
                                      [orbits :ganymede :jupiter]
                                      [orbits :callisto :jupiter])))

(pldb/with-db facts3
  (logic/run* [q]
              (satelliteo :io))) ;=> (_0)

;; CONSTRAINT PROGRAMMING

(require '[clojure.core.logic.fd :as fd])

(logic/run* [q]
            (logic/fresh [n]
                         (fd/in n (fd/domain 0 1))
                         (logic/== q n))) ;=> (0 1)

(logic/run* [q]
            (let [coin (fd/domain 0 1)]
              (logic/fresh [heads tails]
                           (fd/in heads 0 coin)
                           (fd/in tails 1 coin)
                           (logic/== q [heads tails]))))


;; SOLVE SUDOKU WITH CORE.LOGIC


(defn rowify [board]
  (->> board
       (partition 9)
       (map vec)
       vec))

(defn colify [rows]
  (apply map vector rows))

(defn subgrid [rows]
  (partition 9
             (for [row (range 0 9 3)
                   col (range 0 9 3)
                   x (range row (+ row 3))
                   y (range col (+ col 3))]
               (get-in rows [x y]))))

(def logic-board #(repeatedly 81 logic/lvar))

(defn init [[lv & lvs] [cell & cells]]
  (if lv
    (logic/fresh []
                 (if (= '- cell)
                   logic/succeed
                   (logic/== lv cell))
                 (init lvs cells))
    logic/succeed))

(defn solve-logically [board]
  (let [legal-nums (fd/interval 1 9)
        lvars      (logic-board)
        rows       (rowify lvars)
        cols       (colify rows)
        grids      (subgrid rows)]
    (logic/run 1 [q]
               (init lvars board)
               (logic/everyg #(fd/in % legal-nums) lvars)
               (logic/everyg fd/distinct rows)
               (logic/everyg fd/distinct cols)
               (logic/everyg fd/distinct grids)
               (logic/== q lvars))))

(time (-> b1
          solve-logically
          first
          prep
          print-board))

;; example from SICP

(require '[clojure.core.logic.nominal :as nom])

(let [interval (fd/interval 1 5)]
  (logic/run 1 [q]
    (logic/fresh [baker fletcher cooper miller smith]
      (fd/in baker    interval)
      (fd/in fletcher interval)
      (fd/in cooper   interval)
      (fd/in miller   interval)
      (fd/in smith    interval)
      (fd/!= baker    5)
      (fd/!= cooper   1)
      (fd/!= fletcher 1)
      (fd/!= fletcher 5)
      (fd/> miller cooper)
      (fd/eq (= (- smith fletcher) 1))  ;; how to do this with abs?
      (fd/eq (= (- fletcher cooper) 1)) ;; how to do this with abs?
      (fd/distinct [baker fletcher cooper miller smith])
      (logic/== q [baker fletcher cooper miller smith]))))

(for [baker    (range 1 6)
      fletcher (range 1 6)
      cooper   (range 1 6)
      miller   (range 1 6)
      smith    (range 1 6)
      :when (not= baker 5)
      :when (not= cooper 1)
      :when (not (#{1 5} fletcher))
      :when (not= (Math/abs (- smith fletcher)) 1)
      :when (not= (Math/abs (- fletcher cooper)) 1)
      :when (> miller cooper)
      :when (distinct? baker fletcher cooper miller smith)]
  [baker fletcher cooper miller smith])

