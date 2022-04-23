
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
    owner { id name }
    sorted(attribute: $attribute) {...itemInfo}
    unsorted(attribute: $attribute) {...itemInfo}
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
}
")


(def vote
  (str "mutation Vote($tagid: ID!, $left_item: ID!, $right_item: ID!, $attribute: String!, $magnitude: Int!)  {
   vote(tagid: $tagid, left_item: $left_item, right_item: $right_item,
        attribute: $attribute, magnitude: $magnitude) { ...appDb }
} 
"
       fragments))

(def app-db
  (str fragments
       "query starting_data($tagid: ID, $attribute: String)  {
  tag_by_id(id: $tagid) {
     ...appDb
  }
}
   
"))
