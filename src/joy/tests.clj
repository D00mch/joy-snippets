(ns joy.tests
  (:require  [clojure.test :as t]
             [joy.parallel :as p]))

(def stubbed-feed-children
  (constantly [{:content [{:tag :title
                           :content ["Stub"]}]}]))

(defn count-feed-entries [url]
  (count (p/feed-children url)))

(count-feed-entries "http://blog.fogus.me/feed/")

;; not thread-local
(with-redefs [p/feed-children stubbed-feed-children]
  (count-feed-entries "dummy url"))

(with-redefs [p/feed-children stubbed-feed-children]
  (p/occurrences p/title "Stub" "a" "b" "c"))

;; clojure.test
(t/deftest feed-tests
  (with-redefs [p/feed-children stubbed-feed-children]
    (t/testing "Child Counting"
      (t/is (= 1 (count-feed-entries "Dummy URL"))))
    (t/testing "Occurrence Counting"
      (t/is (= 0 (p/count-text-task
                p/title
                "ZOMG"
                "Dummy URL"))))))

(t/run-tests 'joy.tests)

;; CONTRACTS PROGRAMMING
(require '[joy.macro :as m])

(def sqr (partial
          (m/contract sqr-contract
            [n]
            (require (number? n))
            (ensure (pos? %)))
          #(* % %)))

(defn sqr1 [n]
  {:pre  [(number? n)]
   :post [(pos? %)]}
  (* n n))

;; what about 0?
(doseq [n (range Short/MIN_VALUE Short/MAX_VALUE)]
  (try
    (sqr1 n)
    (catch AssertionError e
      (prn "Error on input" n)
      )))







