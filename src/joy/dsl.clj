(ns joy.dsl
  (:require [clojure.string :as str]))

(def artists
  #{{:artist "Burial" :genre-id 1}
    {:artist "Magma" :genre-id 2}
    {:artist "Can" :genre-id 3}
    {:artist "Faust" :genre-id 3}
    {:artist "Ikonika" :genre-id 1}
    {:artist "Grouper"}})

(def genres
  #{{:genre-id 1 :genre-name "Dubstep"}
    {:genre-id 2 :genre-name "Zeuhl"}
    {:genre-id 3 :genre-name "Prog"}
    {:genre-id 4 :genre-name "Drone"}})

;; select * example relational algebra
(require '[clojure.set :as ra])

(def ALL identity)

(ra/select ALL genres)

;; select with ids 1, 3
(defn ids [& ids]
  (fn [m] ((set ids) (:genre-id m))))

(ra/select (ids 1 3) genres)

;; joins

(take 2 (ra/select ALL (ra/join artists genres)))

;; SQL-LIKE DSL

#_(defn fantasy-query [max]
  (SELECT [a b c]
          (FROM X
                (LEFT-JOIN Y :ON (= X.a Y.b)))
          (WHERE (< a 5) AND (< b max))))

(use '[clojure.string :as str :only []])

(defn shuffle-expr [expr]
  (if (coll? expr)
    (if (= (first expr) `unquote)
      "?"
      (let [[op & args] expr]
        (str "("
             (str/join (str " " op " ")
                       (map shuffle-expr args))
             ")")))
    expr))

(defn process-where-clause [processor expr]
  (str " WHERE " (processor expr)))

(defn process-left-join-clause [processor table _ expr]
  (str " LEFT JOIN " table
       " ON " (processor expr)))

(defn process-from-clause [processor table & joins]
  (apply str " FROM " table
         (map processor joins)))

(defn process-select-clause [processor fields & clauses]
  (apply str "SELECT " (str/join ", " fields)
         (map processor clauses)))

;; example
(process-select-clause
 shuffle-expr '[a b c]
 (process-from-clause shuffle-expr 'X
  (process-left-join-clause shuffle-expr 'Y :ON '(= X.a Y.b)))
 (process-where-clause shuffle-expr '(AND (< a 5) (< b ~max))))

;; MAKING DSL OUT OF IT
(declare apply-syntax)

(def ^:dynamic *clause-map*
  {'SELECT    (partial process-select-clause apply-syntax)
   'FROM      (partial process-from-clause apply-syntax)
   'LEFT-JOIN (partial process-left-join-clause shuffle-expr)
   'WHERE     (partial process-where-clause shuffle-expr)})

(defn apply-syntax [[op & args]]
  (apply (get *clause-map* op) args))

(defmacro SELECT
  {:style/indent 1}
  [& args]
  {:query    (apply-syntax (cons 'SELECT args))
   :bindings (vec (for [n     (tree-seq coll? seq args)
                        :when (and (coll? n)
                                   (= (first n) `unquote))]
                    (second n)))})

(defn example-query [max]
  (SELECT [a b c]
    (FROM X
          (LEFT-JOIN Y :ON (= X.a Y.b)))
    (WHERE (AND (< a 5) (< b ~max)))))

(example-query 9)
