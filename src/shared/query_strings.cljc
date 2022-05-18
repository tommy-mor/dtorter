(ns shared.query-strings)

(def fragments
  "
fragment itemInfo on Item { id name }
fragment appDb on Tag {
    name
    description
    votecount
    usercount
    itemcount

    attributes
    attributecounts

    owner { id name }
    sorted(info: $info) {...itemInfo elo}
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
(def vote
  (str "mutation Vote ($vote_info: VoteInputs!, $info: Tagrefreshinfo!) {
            vote(vote_info: $vote_info) { id } 
"
       refresh-db
       "}"
       fragments))

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
