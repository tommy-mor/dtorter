(ns shared.specs
  (:require [clojure.spec.alpha :as s]
            #?@(:clj
                [[dtorter.queries :as q]
                 [dtorter.util :refer [strip]]])))



(def uuid-regex #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

(def uuid-str #(re-matches uuid-regex %))
(s/def :xt/id uuid-str)

(s/def ::user (s/keys :req-un [:xt/id :user/name]))

(s/def ::tag (s/keys :req-un [:xt/id :tag/name :tag/description :tag/owner]))
(s/def :tag/name string?)
(s/def :tag/description string?)
(s/def :tag/votecount int?)
(s/def :tag/usercount int?)
(s/def :tag/itemcount int?)
(s/def :tag/owner ::user)

;; add elo
;; TODO make sure that :un is correct
(s/def ::item (s/keys :req-un [:xt/id :item/name]
                      :opt-un [:item/url :item/tags :item/owner]))

(s/def :item/name string?)
(s/def :item/owner uuid-str)
(s/def :item/tags (s/coll-of uuid-str))
(s/def :item/url string?)

(s/def ::vote (s/keys :req-un [:xt/id
                               :vote/left_item
                               :vote/right_item
                               :vote/magnitude
                               :vote/attribute]
                      :opt-un [:vote/owner :vote/tag]))
(s/def :vote/left_item ::item)
(s/def :vote/right_item ::item)
(s/def :vote/tag uuid-str)
(s/def :vote/attribute string?)
(s/def :vote/magnitude (s/and int? #(and (>= % 0) (<= % 100))))

(s/def ::show (s/keys :opt-un [::add_items ::vote_panel ::vote_edit ::edit_tag]))

(s/def ::votes (s/coll-of ::vote))
(s/def ::sorted (s/coll-of ::item))
(s/def ::unsorted (s/coll-of ::item))
(s/def ::left ::item)
(s/def ::right ::item)


(s/def ::pair (s/nilable (s/keys :req-un [::left ::right])))
;; eventually this will be (s/coll-of [attribute count])
(s/def ::attributes (s/coll-of string?))
(s/def ::current-attribute string?)
(s/def ::percent :vote/magnitude)

(s/def ::db (s/keys :req-un [:tag/name :tag/description :tag/owner :tag/votecount :tag/usercount
                             ::attributes ::votes ::show ::sorted ::unsorted]
                    :opt-un [::percent
                             ::current-attribute])) ;; transient state of webapp

;; TODO add format map to this system (unless its useless cause we want to handle on server)

(comment
  (def tag (first (q/all-tags dtorter.http/db)))
  (def item (first (q/items-for-tag dtorter.http/db (:xt/id tag))))
  (def vote (first (q/votes-for-tag dtorter.http/db (:xt/id tag))))
  (s/explain ::tag (strip tag))
  (s/explain ::item item)
  (s/explain ::vote vote))
