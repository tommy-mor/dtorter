(ns dtorter.util
  (:require [clojure.walk :as walk])
  (:import (clojure.lang IPersistentMap)))

;; from https://lacinia.readthedocs.io/en/latest/tutorial/database-1.html

(defn simplify
  "Converts all ordered maps nested within the map into standard hash maps, and
   sequences into vectors, which makes for easier constants in the tests, and eliminates ordering problems."
  [m]
  (walk/postwalk
    (fn [node]
      (cond
        (instance? IPersistentMap node)
        (into {} node)

        (seq? node)
        (vec node)

        :else
        node))
    m))

(defn strip-namespaces [kw]
  (when (keyword? kw)
    (keyword (name kw))))

(defn strip [map]
  (walk/postwalk (some-fn strip-namespaces identity) map))
