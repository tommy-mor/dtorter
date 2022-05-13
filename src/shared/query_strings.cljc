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
    sorted(attribute: $attribute, user: $user) {...itemInfo elo}
    unsorted(attribute: $attribute, user: $user) {...itemInfo}
    pair {
      left {...itemInfo}
      right {...itemInfo}
    }
    votes(attribute: $attribute) {
      id
      left_item {...itemInfo}
      right_item {...itemInfo}
      attribute
      magnitude
    }
    users { id name }
}
")


(def vote
  (str "mutation Vote($tagid: ID!, $user: ID, $left_item: ID!, $right_item: ID!, $attribute: String!, $magnitude: Int!)  {
   vote(tagid: $tagid, left_item: $left_item, right_item: $right_item,
        attribute: $attribute, magnitude: $magnitude) { ...appDb }
} 
"
       fragments))

(def add-item
  (str "mutation AddItem($tagid: ID!, $attribute: String!, $user: ID, $name: String!, $url: String, $description: String)  {
   additem(tagid: $tagid, name: $name, url: $url, description: $description) { ...appDb }
} 
"
       fragments))

(def del-vote
  (str "mutation DelVote($voteid: ID!, $attribute: String!)  {
   delvote(voteid: $voteid, attribute: $attribute) { ...appDb }
} 
"
       fragments))

(def app-db
  (str "query starting_data($tagid: ID!, $attribute: String, $user: ID)  {
  tag_by_id(id: $tagid) {
     ...appDb
  }
}
   
"
       fragments))
