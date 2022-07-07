(ns dtorter.clean-data
  (:require [dtorter.http :refer [reset]]
            [dtorter.data :as d]
            [martian.core :as martian]
            [martian.clj-http :as martian-http]
            [clojure.set :as se]))

(reset)
(def m (martian-http/bootstrap-openapi "http://localhost:8080/api/swagger.json"))

(def title->id (into {} (map (juxt :title :id) d/tags)))
(def tagid->tag (into {} (map (juxt :id identity) d/tags)))

(def oldtommy (-> (first (->> d/users
                           (filter (comp (partial = "tommy") :username))))
                  :id))

(-> (martian/response-for m :user/list-all)
    :body)


(def tommy (:body (martian/response-for m
                                        :user/new
                                        {:user/name "tommy"
                                         :user/password "tommy1"})))
(def fruits (title->id "Fruits"))

(martian/response-for m :tag/new {:tag/name "Fruits"
                                  :tag/description "epic"
                                  :owner "tommy"})
