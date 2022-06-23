(ns dtorter.http-test
  (:require [clojure.test :refer :all]
            [dtorter.http :refer :all]
            [martian.core :as martian]
            [martian.clj-http :as martian-http]
            [clojure.string :as str]))

(def tommy "092d58c9-d64b-40ab-a8a2-d683c92aa319")
(deftest postget
  (reset)
  (def m (martian-http/bootstrap-openapi "http://localhost:8080/api/swagger.json"))
  
  (testing "can put tags"
    (def t {:tag/name "testing tag"
            :tag/description "epic"
            :owner tommy})
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

(deftest alphabet
  (reset)
  (def m (martian-http/bootstrap-openapi "http://localhost:8080/api/swagger.json"))
  (testing "can sort the alphabet"
    (def tag (-> (martian/response-for m :tag/new {:tag/name "TESTING alphabet"
                                                   :tag/description "for testing"
                                                   :owner tommy})
                 :body
                 :xt/id))

    (def sent-ids (set (doall
                        (for [x "abcdefghijklmnopqrstuvwxyz"]
                          (-> (martian/response-for m :item/new {:item/name (str x) 
                                                                 :item/tags [tag]
                                                                 :owner tommy})
                              :body
                              :xt/id)))))
    
    (def items-in-tag (:body (martian/response-for m :tag/items {:id tag})))
    
    (is (= (set (map :xt/id items-in-tag))
           sent-ids))

    (def pair (-> (martian/response-for m :tag/sorted {:id tag})
                  :body
                  :pair))
    (def left (:left pair))
    (def right (:right pair))
    (let [[winner loser] (sort-by :item/name [left right])]
      (is (= 201 (:status
                  (martian/response-for m :vote/new {:vote/left-item (:xt/id winner)
                                                     :vote/right-item (:xt/id loser)
                                                     :vote/magnitude 20
                                                     :vote/attribute "good attribute"
                                                     :vote/tag tag
                                                     :owner tommy})))))
    
    (def sorted (-> (martian/response-for m :tag/sorted {:id tag})
                  :body))

    (is (= 2 (count (:voteditems sorted))))
    (is (= 2 (count (:sorted sorted))))
    (is (= 24 (count (:unvoteditems sorted))))
    (is (= 1 (count (:allvotes sorted))))
    (is (= (list ["good attribute" 1]) (sorted :frequencies)))
    (for [_ (range 100)]
      (let [{{:keys [left right]} :pair} (:body (martian/response-for m :tag/sorted {:id tag}))
            [winner loser] (sort-by :item/name [left right])]
        
        (martian/response-for m :vote/new {:vote/left-item (:xt/id winner)
                                           :vote/right-item (:xt/id loser)
                                           :vote/magnitude 20
                                           :vote/attribute "good attribute"
                                           :vote/tag tag
                                           :owner tommy})))

    (def final-sort (->> (martian/response-for m :tag/sorted {:id tag})
                         :body
                         :sorted
                         (map :item/name)
                         str/join))
    
    
    

    
    ;; todo make tag/sorted thing
    )
  (stop))

(deftest illegal-item
  (reset)
  (def m (martian-http/bootstrap-openapi "http://localhost:8080/api/swagger.json"))
  (testing "cant add item to nonsense tag"
    
    (def tag (-> (martian/response-for m :tag/new {:tag/name "TESTING alphabet"
                                                   :tag/description "for testing"
                                                   :owner tommy})
                 :body
                 :xt/id))
    (is (= 201 (:status (martian/response-for m :item/new {:item/name "woa" 
                                                           :item/tags [tag]
                                                           :owner tommy}))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (martian/response-for m :item/new {:item/name "wswts"
                                                    :item/tags ["strstr"]
                                                    :owner tommy}))))
  (stop))

(comment (run-tests))
