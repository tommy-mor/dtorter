(ns dtorter.system
  (:require
   [dtorter.data :as data]
   [dtorter.http :as http]
   [dtorter.db :as db]
   [dtorter.api :as api]))

#_ 
(require '[integrant.core :as ig])

;; might use integrant later


#_"
i think this is a pretty serious problem, because pedestal requires gql (run queries), and gql requires pedestal (keeping track of cookies)
so i can't think of how to make it work. still nice to have (reset) function, but its not as good as having automatic repl vibes.

after mom trip:
rewrite:
  use github.com/mount  https://github.com/tolitius/mount/blob/master/doc/differences-from-component.md#differences-from-component
     or ask what xtdb poeple use.... (maybe nothing??)
  need automatic schema recompilation, using function.


"
#_(defn new-system []
  (merge (component/system-map)
         (http/new-server)
         (api/new-schema-provider)
         (db/new-db)))
