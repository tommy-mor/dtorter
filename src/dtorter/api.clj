(ns dtorter.api
  (:require [clojure.java.io :as io]
            [clojure.edn  :as edn]
            [clojure.walk :refer [postwalk]]

            [dtorter.queries :as queries]
            [dtorter.mutations :as mutations]
            [dtorter.math :as math]
            [dtorter.util :refer [strip]]
            [xtdb.api :as xt]))

;; TODO might be easier to to have tag-by-id return entire tag with all caluclations at once. would save some duplicate queries we are having...








