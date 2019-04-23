(ns joy.namespaces)

;; for repl experimentations; need to refer core; don't need to import java.lang
(in-ns 'gibbon)
(reduce + [1 2 (Integer. 1)])

(clojure.core/refer 'clojure.core)


;; creating and interfering ns maps

(def b (create-ns 'bonobo))

((ns-map b) 'String) ;=> java.lang.String

(intern b 'reduce clojure.core/reduce)
(intern b '+ clojure.core/+)

(in-ns 'bonobo)
(reduce + [1 2 3])

(clojure.core/ns-unmap 'bonobo 'reduce)

(remove-ns 'bonobo)
(all-ns)

;; declarative exclusions and inclusions
(ns joy.ns-ex
  (:refer-clojure :exclude [defstruct])
  (:use (clojure set xml))
  (:use [clojure.test :only (are is)])
  (:require (clojure [zip :as z]))
  (:import (java.util Date)
           (java.io File)))
