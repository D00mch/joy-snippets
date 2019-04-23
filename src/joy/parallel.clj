(ns joy.parallel
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [joy.utils :refer [dothreads!]]
            [clojure.core.reducers :as r])
  (:import (java.util.regex Pattern)
           java.util.concurrent.Executors))




;; some future example


(time (let [x (future (do (Thread/sleep 1000) (+ 23 23)))]
        [@x @x]))

;; help functions to count titles in RSS or ATOM

(defn feed->zipper [uri-str]
  (->> (xml/parse uri-str)
       zip/xml-zip))

(defn normalize [feed]
  (if (= :feed (:tag (first feed)))
    feed
    (zip/down feed)))

(defn feed-children [uri-str]
  (->> uri-str
       feed->zipper
       normalize
       zip/children
       (filter (comp #{:item :entry} :tag))))

(defn title [entry]
  (some->> entry
           :content
           (some #(when (= :title (:tag %)) %))
           :content
           first))

(defn count-text-task [extractor txt feed]
  (let [items (feed-children feed)
        re    (Pattern/compile (str "(?i)" txt))]
    (->> items
         (map extractor)
         (mapcat #(re-seq re %))
         count)))

(count-text-task title
                 "Erlang"
                 "http://feeds.feedburner.com/ElixirLang")

(count-text-task title
                 "Elixir"
                 "http://feeds.feedburner.com/ElixirLang")

;; PARALLEL WITH FUTURE

(def feeds #{"http://feeds.feedburner.com/ElixirLang"
             "http://blog.fogus.me/feed/"})

(let [results (for [feed feeds]
                (future
                  (count-text-task title "Elixir" feed)))]
  (reduce + (map deref results)))

(defmacro as-futures
  {:style/indent 1}
  [[a args] & body]
  (let [parts          (partition-by #{'=>} body)
        [acts _ [res]] (partition-by #{:as} (first parts))
        [_ _ task]     parts]
    `(let [~res (for [~a ~args] (future ~@acts))]
       ~@task)))

(defn occurrences [extractor tag & feeds]
  (as-futures
   [feed feeds]
   (count-text-task extractor tag feed)
   :as results
   =>
   (reduce + (map deref results))))

(occurrences title "released"
             "http://blog.fogus.me/feed/"
             "http://feeds.feedburner.com/ElixirLang"
             "http://www.ruby-lang.org/en/feeds/news.rss")

;; PROMISES (write-once)

(def x (promise))
(def y (promise))
(def z (promise))

(dothreads! #(deliver z (+ @x @y)))
(dothreads! #(do (Thread/sleep 2000) (deliver x 52)))
(dothreads! #(do (Thread/sleep 4000) (deliver y 86)))
(time @z)

(defmacro with-promises [[n tasks _ as] & body]
  (when as
    `(let [tasks#    ~tasks
           n#        (count tasks#)
           promises# (take n# (repeatedly promise))]
       (dotimes [i# n#]
         (dothreads!
          (fn []
            (deliver (nth promises# i#)
                     ((nth tasks# i#))))))
       (let [~n tasks#
             ~as promises#]
         ~@body))))

(defrecord TestRun [run passes failed])

(defn pass [] true)
(defn fail [] false)

(defn run-tests [& all-tests]
  (with-promises
    [tests all-tests :as results]
    (into (TestRun. 0 0 0)
          (reduce #(merge-with + %1 %2)
                  {}
                  (for [r results]
                    (if @r
                      {:run 1 :passed 1}
                      {:run 1 :failed 1}))))))

(run-tests fail fail fail pass)

;; CALLBACK API TO BLOCKING API

(defn feed-items [k feed]
  (k
   (for [item (filter (comp #{:entry :item} :tag)
                      (feed-children feed))]
     (-> item :content first :content))))

;; with callback, or continuation
(feed-items
 count
 "http://blog.fogus.me/feed/")

;; with blocking behavior
(let [p (promise)]
  (feed-items #(deliver p (count %))
              "http://blog.fogus.me/feed/")
  @p)

(defn cps->fn [f k]
  (fn [& args]
    (let [p (promise)]
      (apply f #(deliver p (k %)) args)
      @p)))

(def count-items (cps->fn feed-items count))
(count-items "http://blog.fogus.me/feed/")

;; PARALLEL OPERATIONS

;; pvalues
(pvalues 1 2 (+ 3 4))

;; pvalues are lazy seq
(defn sleeper [s thing] (Thread/sleep (* 1000 s)) thing)
(defn pvs [] (pvalues (sleeper 2 :1st)
                      (sleeper 3 :2nd)
                      (sleeper 2 :3rd)
                      (keyword "4th")))
(-> (pvs) last time)

;; pmap
(->> [1 2 3]
     (pmap (comp inc (partial sleeper 2)))
     doall
     time)

;; pcalls is lazy
(-> (pcalls
     #(sleeper 2 :first)
     #(sleeper 3 :second)
     #(keyword "3rd"))
    doall
    time)


;; REDUCERS
(def big-vec (vec (range 1000000)))

(time (reduce + big-vec))
(time (r/fold + big-vec))

(defn ff []
  (ff))


(defn f []
  (try
   (ff)
   (catch Throwable e
     (prn "e1")))
  (prn "end 2"))


(defn p []
  (try
    (f)
    (catch Exception e
      (prn "exp in p")
      ))
  (prn "end"))
