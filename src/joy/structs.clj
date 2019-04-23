(ns joy.structs)

;; RECORDS OVERTHROW STRUCTS
(defrecord TreeNode [val l r])

(TreeNode. 5 nil nil)

(defn xconj [t v]
  (cond (nil? t)       (TreeNode. v nil nil)
        (< v (:val t)) (TreeNode. (:val t) (xconj (:l t) v) (:r t))
        :else          (TreeNode. (:val t) (:l t) (xconj (:r t) v))))

(defn xseq [t]
  (when t
    (concat (xseq (:l t)) [(:val t)] (xseq (:r t)))))

(-> (xconj nil 5)
    (xconj 3)
    (xconj 2)
    (xconj 4)
    xseq)

(def tree1 (-> (xconj nil 5)
               (xconj 3)))
(def tree2 (xconj tree1 7))
(identical? (:L tree1) (:L tree2))
(dissoc tree1 :r :l) ;; => map

;; PROTOCOLS

(defprotocol FIXO
  (fixo-push [fixo value])
  (fixo-pop [fixo])
  (fixo-peek [fixo]))

(extend-type TreeNode
  FIXO
  (fixo-push [node value]
    (xconj node value)))

(xseq (fixo-push tree1 2))

(extend-type clojure.lang.PersistentVector
  FIXO
  (fixo-push [this value]
    (conj this value))
  (fixo-peek [this]
    (peek this))
  (fixo-pop [this]
    (pop this)))

(extend-type nil
  FIXO
  (fixo-push [t v]
    (TreeNode. v nil nil)))

(xseq (reduce fixo-push nil [2 4 1 5]))

;; SHARING METHOD IMPLEMENTATIONS

;; use only protocols functions to implement the method
(defn fixo-into [c1 c2]
  (reduce fixo-push c1 c2))

;; mixins
(defprotocol StringOps (rev [s]) (upp [s]))
(def rev-mixin {:rev clojure.string/reverse})
(def upp-mixin {:upp #(.toUpperCase %)})
(extend String StringOps (merge rev-mixin upp-mixin))

;; REIFY
(defn limited-fixo
  ([limit] (fixed-fixo limit []))
  ([limit vector]
   (reify FIXO
     (fixo-push [this value]
       (if (< (count vector) limit)
         (fixed-fixo limit (conj vector value))
         this))
     (fixo-peek [_]
       (peek vector))
     (fixo-pop [_]
       (pop vector)))))

(def vv (limited-fixo 10))
(-> (fixo-push vv 3) fixo-peek)


(def my-callable
  (reify java.util.concurrent.Callable
    (call [this]
      2)))

(.call my-callable)

;; METHOD IMPLEMENTATION IN DEFRECORD

;; extend is less performant than
(defrecord TreeNode [val l r]
  FIXO
  (fixo-push [t v]
    (if (< v val)
      (TreeNode. val (fixo-push l v) r)
      (TreeNode. val l (fixo-push r v))))
  (fixo-peek [t]
    (if l
      (fixo-peek l)
      val))
  (fixo-pop [t]
    (if l
      (TreeNode. val (fixo-pop l) r)
      r)))

(def sample-tree2 (reduce fixo-push (TreeNode. 3 nil nil) [5 2 3 4]))
(xseq sample-tree2)


(defrecord runnable [n]
  Runnable
  (run [this]
    (prn 3)))

(.run (runnable. 2))


;; DEFTYPE (like records, but without any predefined methods)

(deftype InfiniteConstant [i]
  clojure.lang.ISeq
  (seq [this]
    (lazy-seq (cons i (seq this)))))

(take 3 (InfiniteConstant. 1))
