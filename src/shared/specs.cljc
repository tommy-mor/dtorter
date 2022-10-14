(ns shared.specs
  (:require [clojure.spec.alpha :as s]))



(def uuid-str (s/and string? #(< (count %) 50) #(> (count %) 4)))

(s/def :xt/id uuid-str)

(s/def :user/name string?)
(s/def :user/password string?)
(s/def :user.parameters/new (s/keys :req [:user/name :user/password]))

(s/def ::user (s/keys :req [:xt/id :user/name]))
(s/def ::owner uuid-str)

(defn collapsible [spec]
  (s/or :id uuid-str :full spec))

(s/def ::tag (s/keys :req [:tag/name :tag/description]
                     :opt [:xt/id]
                     :req-un [::owner]))
(s/def :tag/name string?)
(s/def :tag/description string?)
(s/def :tag/votecount int?)
(s/def :tag/usercount int?)
(s/def :tag/itemcount int?)
;; add elo
;; TODO make sure that :un is correct
(s/def ::item (s/keys :req [:item/name]
                      :req-un [::owner]
                      :opt [:xt/id :item/url :item/paragraph :item/tags]))

(s/def :item/name string?)
(s/def :item/tags (s/coll-of uuid-str))
(s/def :item/url string?)
(s/def :item/paragraph string?)

(s/def ::vote (s/keys :req [:vote/left-item
                            :vote/right-item
                            :vote/magnitude
                            :vote/attribute
                            :vote/tag]
                      :opt [:xt/id]
                      :req-un [::owner]))

(s/def :vote/left-item uuid-str)
(s/def :vote/right-item uuid-str)
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
(s/def :interface.filter/attribute (s/or :specified string?
                                         :empty #{:interface.filter/no-attribute}))
(s/def :interface.filter/user (s/or :all #{:interface.filter/all-users} :specified uuid-str))
(s/def :pair/percent :vote/magnitude)

(s/def :interface/owner ::user)

(s/def :tag/votes (s/coll-of ::vote))
(s/def :tag/items (s/coll-of ::item))
(s/def :tag.filtered/votes (s/coll-of ::vote))
(s/def :tag.filtered/items (s/coll-of ::item))
(s/def :tag.filtered/unvoted-items (s/coll-of ::item))
(s/def :tag.filtered/sorted (s/coll-of ::item))
(s/def :tag/item-vote-counts (s/map-of uuid-str int?))
(s/def :interface/attributes (s/map-of string? int?))
(s/def :interface/users (s/coll-of ::user))

;; todo maybe have different specs (mostly same)
;; that are for the api call, and one for the frontend database
(s/def :page/tag (s/keys :req [:tag/name :tag/description
                               :tag/votecount :tag/usercount
                               :tag/votes :tag/items

                               :tag.filtered/votes
                               :tag.filtered/items
                               :tag.filtered/unvoted-items
                               :tag.filtered/sorted

                               :tag/item-vote-counts
                               :interface/attributes
                               :interface/owner
                               :interface/users]
                         :opt [:interface.filter/user ; because they stored in the url
                               :interface.filter/attribute]
                         :req-un [::owner]))

(s/def :page/tags (s/coll-of ::tag))

(s/def ::db (s/keys :opt [:page/tags :page/tag]))

;; the query that describes a tag view..
(s/def :tag.query/attribute string?)
(s/def :tag.query/user string?)
(s/def :tag.query/itemid string?)
(s/def ::tag-query
  (s/keys :opt-un [:tag.query/attribute
                   :tag.query/user
                   :tag.query/itemid]))
;; transient state of webapp

;; TODO add format map to this system (unless its useless cause we want to handle on server)


