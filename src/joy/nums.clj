(ns joy.nums)

(let [imadeuapi 3.14159264358979323846264338327950288419716939937M]
  (prn (class imadeuapi))
  imadeuapi)

(let [butieatedit 3.14159264358979323846264338327950288419716939937]
  (prn (class butieatedit))
  butieatedit)


(def clueless 9)
(class (+ clueless 9000000000000000000000)) ; BigInt

(+ Long/MAX_VALUE Long/MAX_VALUE) ; ArithmeticException
(unchecked-add Long/MAX_VALUE Long/MAX_VALUE)

(float 0.00000000000000000000000000000000000000000000001) ; 0.0


(class (+ 0.1 0.111111111111111111111111111111111111111M)) ; Dobule :(


;; why rationals?

1.0E-4300000000               ; 0.0
1.0E-4300000000M              ; NumberFormatExc (nums of digits after . is 4byte)
(rationalize 1.0E-4300000000) ; 0N

;; 1. float aren't associative
(def a  1.0e50)
(def b -1.0e50)
(def c 17.0e00)

(+ (+ a b) c) ; 17
(+ a (+ b c)) ; 0

;; fix with rational | also can fix with M (BigDecimal)
(def a (rationalize  1.0e50))
(def b (rationalize -1.0e50))
(def c (rationalize 17.0e00))

;; rules for rationals
;; 1. don't use Java/Math
