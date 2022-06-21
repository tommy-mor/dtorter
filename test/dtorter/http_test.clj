(ns dtorter.http-test
  (:require [clojure.test :refer :all]
            [dtorter.http :refer :all]
            [martian.core :as martian]
            [martian.clj-http :as martian-http]))

(deftest postget
  (start {})
  (def m (martian-http/bootstrap-openapi "http://localhost:8080/api/swagger.json"))
  
  (testing "can put tags"
    (def t {:tag/name "testing tag"
            :tag/description "epic"
            :owner "092d58c9-d64b-40ab-a8a2-d683c92aa319"})
    (def t-resp (martian/response-for m :tag/new t))
    (is (:status t-resp)
        201)

    (def t-id (-> t-resp :body :xt/id))
    
    (def t-get (martian/response-for m :tag/get {:id t-id}))

    (is (:status t-get) 200)
    
    (is (= t (-> t-get :body (dissoc :xt/id))))

    (is (= 204 (:status
                (martian/response-for m :tag/put
                                      (assoc t
                                             :tag/name "changed"
                                             :id t-id)))))
    
    (def t-get-get (martian/response-for m :tag/get {:id t-id}))
    (is (= (-> t-get-get
               :body :tag/name)
           "changed"))
    
    (is (= 204 (:status
                (martian/response-for m :tag/delete {:id t-id})))))
  (stop))

(comment (run-tests))
