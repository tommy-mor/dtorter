(ns shared.spec
  (:require [clojure.spec.alpha :as s]
            #?@(:clj
                [[dtorter.queries :as q]
                 [dtorter.api :refer [strip]]])))



(def uuid-regex #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

(def uuid-str #(re-matches uuid-regex %))


(s/def ::tag (s/keys :req-un [:xt/id :tag/name :tag/description :tag/owner]))
(s/def :xt/id uuid-str)
(s/def :tag/owner uuid-str)
(s/def :tag/name string?)
(s/def :tag/description string?)

(s/def ::item (s/keys :req-un [:xt/id :item/name :item/owner :item/tags :item/url]))

(s/def :item/name string?)
(s/def :item/owner uuid-str)
(s/def :item/tags (s/coll-of uuid-str))
(s/def :item/url string?)

(s/def ::vote (s/keys :req0-un [:xt/id
                                :vote/left-item
                                :vote/right-item
                                :vote/tag
                                :vote/magnitude
                                :vote/owner
                                :vote/attribute]))
(s/def :vote/left-item uuid-str)
(s/def :vote/left-item uuid-str)
(s/def :vote/right-item uuid-str)
(s/def :vote/tag uuid-str)
(s/def :vote/attribute string?)
(s/def :vote/magnitude (s/and int? #(and (> % 0) (< % 100))))

(s/def ::show (s/keys :opt-un [::add_items ::vote_panel ::vote_edit ::edit_tag]))


(comment
  (def tag (first (q/all-tags dtorter.http/db)))
  (def item (first (q/items-for-tag dtorter.http/db (:xt/id tag))))
  (def vote (first (q/votes-for-tag dtorter.http/db (:xt/id tag))))
  (s/explain ::tag (strip tag))
  (s/explain ::item item)
  (s/explain ::vote vote))