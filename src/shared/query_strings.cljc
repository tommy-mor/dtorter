
(ns shared.query-strings)

(def starting-data-query
  "fragment itemInfo on Item { id name }

query  starting_data($tagid: ID, $attribute: String)  {
  tag_by_id(id: $tagid) {
    name
    description
    attributes
    sorted(attribute: $attribute) {...itemInfo}
    unsorted(attribute: $attribute) {...itemInfo}
    pair {
      left {...itemInfo}
      right {...itemInfo}
    }
    votes {
      id
      left_item {...itemInfo}
      right_item {...itemInfo}
    }
  }
}
   
")
