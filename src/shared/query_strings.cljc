
(ns shared.query-strings)

(def starting-data-query
  "query starting_data($tagid: ID, $attribute: String)  {
     tag_by_id(id: $tagid) { 
       name description sorted(attribute: $attribute) {name elo}
     }
   }")
