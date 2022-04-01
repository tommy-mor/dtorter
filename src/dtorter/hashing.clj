(ns dtorter.hashing
  (:require [cryptohash-clj.api :as c]
            [cryptohash-clj.encode :as enc]))

(defn hash-pw [pw]
  (c/hash-with :argon2 pw {:type :argon2i :version :v13
                           :iterations 1
                           :mem-cost 10
                           :mem-size 13}))

(defn check-pw [pw hash]
  (c/verify-with :argon2 pw hash))

(comment
  (check-pw "test" (hash-pw "test")))
