
(ns shared.query-strings)

(def starting-data-query
  "fragment itemInfo on Item { id name }

query  starting_data($tagid: ID, $attribute: String)  {
      tag_by_id(id: $tagid) {
        name
        attributes
        sorted(attribute: $attribute) {
          ...itemInfo
          id
        }
        pair {
          left {...itemInfo}
          right {...itemInfo}
        }
      }
   }
")
