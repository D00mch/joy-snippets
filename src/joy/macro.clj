(ns joy.macro
  (:require [clojure.string :as str]
            [clojure.walk :as walk])
  (:import [java.net URL]
           [java.io BufferedReader InputStreamReader]))

;; def control structures without syntax-quote

(defmacro do-until [& clauses]
  (when clauses
    (list 'clojure.core/when (first clauses)
          (if (next clauses)
            (second clauses)
            (throw (IllegalArgumentException.
                    "do-until requires an even number of forms")))
          (cons 'do-until (nnext clauses)))))

(macroexpand-1 '(do-until true (prn 1) false (prn 2)))

(macroexpand '(do-until true (prn 1) false (prn 2)))

(walk/macroexpand-all '(do-until true (prn 1) false (prn 2)))


;; def control structures using syntax-quote and unquoting


(defmacro unless [condition & body]
  `(if (not ~condition)
     (do ~@body)))

(walk/macroexpand-all '(unless (= 1 2) (prn "bob") (prn "bob2")))

(defmacro def-watched [name & value]
  `(do (def ~name ~@value)
       (add-watch (var ~name)
                  :re-bind
                  (fn [~'key ~'r old# new#]
                    (println old# " -> " new#)))))

(walk/macroexpand-all '(def-watched x (* 2 2)))



;; using macros to change (build) forms


(defmacro domain [name & body]
  `{:tag :domain
    :attrs {:name (str '~name)}
    :content [~@body]})

(declare handle-things)

(defmacro grouping [name & body]
  `{:tag :grouping
    :attrs {:name (str '~name)}
    :content [~@(handle-things body)]})

(declare grok-attrs grok-props)

(defn handle-things [things]
  (for [t things]
    {:tag :thing
     :attrs (grok-attrs (take-while (comp not vector?) t))
     :content (if-let [c (grok-props (drop-while (comp not vector?) t))]
                [c]
                [])}))

(defn grok-attrs [attrs]
  (into {:name (str (first attrs))}
        (for [a (rest attrs)]
          (cond (list? a) [:isa (str (second a))]
                (string? a) [:comment a]))))

(defn grok-props [props]
  (when props
    {:tag :properties
     :attrs nil
     :content (apply vector
                     (for [p props]
                       {:tag :property
                        :attrs {:name (str (first p))}
                        :content nil}))}))

(def d
  (domain man-vs-monster
          (grouping people
                    (Human "A stock human")
                    (Man (isa Human)
                         "A man, baby"
                         [name]
                         [has-beard?]))
          (grouping monsters
                    (Chupaabra
                     "A fierce creature"
                     [eats-goats?]))))

;; using macros to control symbolic resolution time

(defmacro resolution [] `x)
(def x 1)

(let [x 2] (resolution)) ;;=> 1


;; using macros to manage resources
(defn joc-www []
  (-> "http://joyofclojure.com/hello" URL.
      .openStream
      InputStreamReader.
      BufferedReader.))

(let [stream (joc-www)]
  (with-open [page stream]
    (prn (.readLine page))
    (prn "The stream will now close")))

(defmacro with-resource [binding close-fn & body]
  `(let ~binding
     (try
       (do ~@body)
       (finally
         (~close-fn ~(binding 0))))))

;; macros returning functions

(declare collect-bodies)

(defmacro contract [name & forms]
  (list* `fn name (collect-bodies forms)))

(declare build-contract)

(defn collect-bodies [forms]
  (for [form (partition 3 forms)]
    (build-contract form)))

(defn build-contract [c]
  (let [args (first c)]
    (list
     (into '[f] args)
     (apply merge
            (for [con (rest c)]
              (cond (= (first con) 'require)
                    (assoc {} :pre (vec (rest con)))
                    (= (first con) 'ensure)
                    (assoc {} :post (vec (rest con)))
                    :else
                    (throw (Exception. (str "unknown tag" (first con)))))))
     (list* 'f args))))

(collect-bodies '([x]
                 (require
                  (pos? x))
                 (ensure
                  (= (* 2 x) %))))

(build-contract '([x]
                 (require
                  (pos? x))
                 (ensure
                  (= (* 2 x) %))))

(def doubler-conract
  (contract doubler
            [x]
            (require
             (pos? x))
            (ensure
             (= (* 2 x) %))
            [x y]
            (require
             (pos? x)
             (pos? y))
            (ensure
             (= (* 2 (+ x y)) %))))

(def times2 (partial doubler-conract #(* 2 %)))
(times2 1)

((partial doubler-conract #(+ %1 %1 %2 %2)) 2 3)
