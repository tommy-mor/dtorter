(ns dtorter.permisisons
  (:require [datomic.client.api :as d]))

(comment
  "
things the permission system has to do:

[entity :can/vote tag]

by default, everything denied.

public tags have
[:anybody :can/view tag]
api tags have
[:nobody :can/vote tag]

")

(comment
  "
things the schema system has to do


")
