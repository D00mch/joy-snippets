(ns joy.arrays)

;; INTO-ARRAY WILL RETURN CHARACTER[], NOT CHAR[]
(doto (StringBuilder. "abc")
  (.append (into-array [\x \y \z])))

;; fix with
(doto (StringBuilder. "abc")
  (.append (char-array [\x \y \z])))


;; CREATING REFERENCE ARRAYS


(into-array ["a" "b" "c"])

(into-array Number [1 2.0 3M 3/4])

(aget (to-array-2d [[1 2 3]
                    [4 5 6]])
      1 1)

(to-array [1 "b"]) ;; object array
;; (into-array [1 "b"]) => IllegalArgumentException



;; ITERATING AND MUTATING
(defn asum-sq [xs]
  (let [dbl (amap xs i ret
                  (* (aget xs i)
                     (aget xs i)))]
    (areduce dbl i ret 0
             (+ ret (aget dbl i)))))

(asum-sq (double-array [1 2 3 4 5]))


;; IDENTIFYING ARRAYS
(defmulti what-is class)

(defmethod what-is
  (Class/forName "[Ljava.lang.String;")
  [_]
  "1d String")

(defmethod what-is
  (Class/forName "[[Ljava.lang.Object;")
  [_]
  "2d Object")

(defmethod what-is
  (Class/forName "[[[[I")
  [_]
  "Primitive 4d int")

(what-is (make-array Integer/TYPE 2 2 2 2))

;; MULTIDIMENSIONAL ARRAYS

(defmethod what-is
  (Class/forName "[[D")
  [_]
  "Primitive 2d double")

(defmethod what-is
  (Class/forName "[Lclojure.lang.PersistentVector;")
  [_]
  "1d Persistent Vector")

(what-is (into-array (map double-array [[1.0] [2.0]])))
(what-is (into-array [[1.0] [2.0]]))

;; VARIADIC METHOD/CONSTURCTOR CALLS
(String/format "An int %d and a String %s"
               (to-array [99 "Bob"]))
