(ns joy.exception)


(defmacro pairs [& args]
  (if (even? (count args))
    `(partition 2 '~args)
    (throw (IllegalArgumentException. (str "pairs require an even num of args")))))

;; exception thrown at definition
(defn p [] (pairs 1 2 3))


;; CUSTOM EXCEPTIONS
(defn perform-cleaner-act [x y]
  (try
    (/ x y)
    (catch ArithmeticException ex
      (throw (ex-info "You attempted an unclean act"
                      {:args [x y]})))))




