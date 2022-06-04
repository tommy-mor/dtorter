(ns shared.query-strings)

(def fragments "fragment itemInfo on Item { id name }
    fragment appDb on Tag {
    name
    description
    votecount
    usercount
    itemcount

    attributes
    attributecounts

    owner { id name }
    sorted(info: $info) {...itemInfo elo votecount }
    unsorted(info: $info) {...itemInfo}
    pair {
      left {...itemInfo}
      right {...itemInfo}
    }
    votes(info: $info) {
      id
      left_item {...itemInfo}
      right_item {...itemInfo}
      attribute
      magnitude
    }
    users { id name }
}
")

(def refresh-db
  "tag_by_id(info: $info) {
     ...appDb
  }")


(def app-db
  (str "mutation starting_data($info: Tagrefreshinfo!)  {"
       refresh-db
       "}"
       fragments))

;; mutations
(defn vote-fn [refresh-db fragments]
  (str "mutation Vote ($vote_info: VoteInputs!, $info: Tagrefreshinfo!) {
            vote(vote_info: $vote_info) { id } 
"
       refresh-db
       "}"
       fragments))

(def vote (vote-fn refresh-db fragments))

(def add-item
  (str "mutation AddItem($item_info: ItemInputs!, $info: Tagrefreshinfo!) {
   additem(item_info: $item_info) { id }
 
"
       refresh-db
       "}"
       fragments))

(def del-vote
  (str "mutation DelVote($voteid: ID!, $info: Tagrefreshinfo)  {
   delvote(voteid: $voteid) { id }
"
       refresh-db
       "}"
       fragments))


;; for item page

(def item-page-fragments
  "

fragment tagAppDb on Tag {
  name
  attributes
  attributecounts
  sorted(info: $info) { id name elo votecount }
  unsorted(info: $info) { id name }
  votes(info: $info) {
    id
    left_item { id name }
    right_item { id name }
    attribute
    magnitude
  }
  users { id name }
}
")

(def item-page-refresh-db "
            tag_by_id(info: $info) { ...tagAppDb }
            item_by_id(info: $info) { id name paragraph url }
")

(def item-app-db
  (str "mutation starting_item_data($info: Tagrefreshinfo!, $itemid: ID!) {"
       item-page-refresh-db
       "}"
       item-page-fragments
       ))

(def vote-item (vote-fn item-page-refresh-db item-page-fragments))
