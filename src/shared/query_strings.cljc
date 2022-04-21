
(ns shared.query-strings)

(def starting-data-query
  "fragment itemInfo on Item { id name }

query  starting_data($tagid: ID, $attribute: String)  {
  tag_by_id(id: $tagid) {
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
}
   
")
