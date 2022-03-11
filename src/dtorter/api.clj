(ns dtorter.api
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.edn  :as edn]))

(def resolver-map {:query/tag-by-id (fn [context args value]
                                      nil)})

(defn load-schema []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)
      schema/compile))

(def schema (load-schema))

(defn q [query-string]
  (lacinia/execute schema query-string nil nil))

(q "{ tag_by_id(id: \"foo\") {id name}}")


