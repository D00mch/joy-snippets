(ns joy.multimethod
  (:refer-clojure :exclude [get]))

;; Universal Design Pattern
(defn beget [this proto]
  (assoc this ::prototype proto))

(defn get [m k]
  (when m
    (if-let [[_ v] (find m k)]
      v
      (recur (::prototype m) k))))

(def put assoc)

;; cat story (basic use of UDP)
(def cat {:likes-dogs true, :ocd-bathing true})
(def morris (beget {:likes-9lives true} cat))
(def post-traumatic-morris (beget {:likes-dogs nil} morris))

(get cat :likes-dogs)
(get morris :likes-dogs)
(get post-traumatic-morris :likes-dogs)


;; multimethod
(defmulti compiler :os)
(defmethod compiler ::unix [m] (get m :c-compiler))
(defmethod compiler ::osx  [m] (get m :llvm-compiler))

(def clone (partial beget {}))
(def unix {:os ::unix, :c-compiler "cc", :home "/home", :dev "/dev"})
(def osx (-> (clone unix)
             (put :os ::osx)
             (put :llvm-compiler "clang")
             (put :home "/Users")))

(compiler unix)
(compiler osx)

;; ad hoc hierarchies for inherited behaviors
(defmulti home :os)
(defmethod home ::unix [m] (get m :home))
(home unix)
(derive ::osx ::unix)
(home osx)

(parents ::osx)
(ancestors ::osx)
(descendants ::unix)
(isa? ::osx ::unix) ;=> true

;; resolving conflict in hierarchies
(derive ::osx ::bsd)
(defmethod home ::bsd [m] "/home")
(prefer-method home ::unix ::bsd)
(home osx)


;; Arbitrary dispatch

(defmulti compile-cmd (juxt :os compiler))

(defmethod compile-cmd [::osx "clang"]
  [m]
  (str "/usr/bin/" (get m :c-compiler)))

(defmethod compile-cmd :default [m]
  (str "Unsure where to locate " (get m :c-compiler)))

(compile-cmd osx)
(compile-cmd unix)
