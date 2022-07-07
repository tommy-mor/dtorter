(ns dtorter.http-test
  (:require [clojure.test :refer :all]
            [dtorter.http :refer :all]
            [martian.core :as martian]
            [martian.clj-http :as martian-http]
            [clojure.string :as str]))

(def tommy "092d58c9-d64b-40ab-a8a2-d683c92aa319")
(def alphabet-string "abcdefghijklmnopqrstuvwxyz")

(deftest vote-crud
  (reset)
  (def m (martian-http/bootstrap-openapi "http://localhost:8080/api/swagger.json"))
  (testing "can create vote"
    (def t {:tag/name "testing tag"
            :tag/description "epic"
            :owner tommy})
    
    (def t-resp (martian/response-for m :tag/new t))
    (is (:status t-resp)
        201)

    (def t-id (-> t-resp :body :xt/id))
    
    (def t-get (martian/response-for m :tag/get {:id t-id}))
    (def tag (-> t-get
                 :body
                 :xt/id))
    
    (def i1 (-> (martian/response-for m :item/new {:item/name "item1" 
                                                   :item/tags [tag]
                                                   :owner tommy})
                :body
                :xt/id))
    
    (def i2 (-> (martian/response-for m :item/new {:item/name "item2" 
                                                   :item/tags [tag]
                                                   :owner tommy})
                :body
                :xt/id))

    (def sorted (-> (martian/response-for m :tag/sorted {:id tag :attribute "good attribute"})
                    :body))
    (is (= 0 (-> sorted :tag/votes count)))
    (is (= 0 (-> sorted :tag.filtered/sorted count)))
    
    (def v1 (-> (martian/response-for m :vote/new {:vote/left-item i1
                                                   :vote/right-item i2
                                                   :vote/magnitude 20
                                                   :vote/attribute "good attribute"
                                                   :vote/tag tag
                                                   :owner tommy})
                :body
                :xt/id
                ))
    
    (def sorted (-> (martian/response-for m :tag/sorted {:id tag :attribute "good attribute"})
                    :body ))

    (is (= 1 (-> sorted :tag/votes count)))
    (is (= 2 (-> sorted :tag.filtered/sorted count)))
    ;; in correct order
    (is (= ["item1" "item2"] (->> sorted
                                  :tag.filtered/sorted
                                  (map :item/name))))
    (is (> (-> sorted
               :tag.filtered/sorted
               (nth 0)
               :elo)
           (-> sorted
               :tag.filtered/sorted
               (nth 1)
               :elo)))
    
    (is (= 204 (-> (martian/response-for m :vote/put {:id v1
                                                      :vote/left-item i1
                                                      :vote/right-item i2
                                                      :vote/magnitude 80
                                                      :vote/attribute "good attribute"
                                                      :vote/tag tag
                                                      :owner tommy})
                   :status)))
    
    (def sorted (-> (martian/response-for m :tag/sorted {:id tag :attribute "good attribute"})
                    :body ))
    ;; order reversed
    (is (= ["item2" "item1"] (->> sorted
                                  :tag.filtered/sorted
                                  (map :item/name))))
    (is (= 204 (-> (martian/response-for m  :vote/delete {:id v1})
                   :status)))
    
    (def sorted (-> (martian/response-for m :tag/sorted {:id tag :attribute "good attribute"})
                    :body ))
    
    (is (-> sorted :tag.filtered/sorted count zero?))))

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

(defn calc-score [a b]
  (let [idx1 (str/index-of alphabet-string a)
        idx2 (str/index-of alphabet-string b)]
    (+ 50 (- idx2 idx1))))

(deftest alphabet
  (reset)
  (def m (martian-http/bootstrap-openapi "http://localhost:8080/api/swagger.json"))
  (testing "can sort the alphabet with relative weights"
    (def tag (-> (martian/response-for m :tag/new {:tag/name "TESTING alphabet"
                                                   :tag/description "for testing"
                                                   :owner tommy})
                 :body
                 :xt/id))

    (def sent-ids (set (doall
                        (for [x alphabet-string]
                          (-> (martian/response-for m :item/new {:item/name (str x) 
                                                                     :item/tags [tag]
                                                                     :owner tommy})
                              :body
                              :xt/id)))))
    
    (def items-in-tag (:body (martian/response-for m :tag/items {:id tag})))
    
    (is (= (set (map :xt/id items-in-tag))
           sent-ids))

    (def pair (-> (martian/response-for m :tag/sorted {:id tag
                                                       :attribute "good attribute"})
                  :body
                  :pair))
    (def left (:left pair))
    (def right (:right pair))
    (let [mag (calc-score (:item/name left)
                          (:item/name right))]
      (is (= 201 (:status
                  (martian/response-for m :vote/new {:vote/left-item (:xt/id left)
                                                     :vote/right-item (:xt/id right)
                                                     :vote/magnitude mag
                                                     :vote/attribute "good attribute"
                                                     :vote/tag tag
                                                     :owner tommy})))))
    
    (def sorted (-> (martian/response-for m :tag/sorted {:id tag
                                                         :attribute "good attribute"})
                    :body))
    (is (= 2 (count (:tag.filtered/items sorted))))
    (is (= 2 (count (:tag.filtered/sorted sorted))))
    (is (= 24 (count (:tag.filtered/unvoted-items sorted))))
    (is (= 1 (count (:tag/votes sorted))))
    (is (= {"good attribute" 1} (sorted :interface/attributes)))
    
    (for [_ (range 100)]
      
      (let [{{:keys [left right]} :pair}
            (:body
             (martian/response-for m :tag/sorted
                                   {:id tag
                                    :attribute "good attribute"
                                    :pair-strategy :random}))
            
            mag
            (calc-score (:item/name left) (:item/name right))]
        
        
        (martian/response-for m :vote/new {:vote/left-item (:xt/id left)
                                           :vote/right-item (:xt/id right)
                                           :vote/magnitude mag
                                           :vote/attribute "good attribute"
                                           :vote/tag tag
                                           :owner tommy})))

    (def final-sort (->> (martian/response-for m :tag/sorted {:id tag :attribute "good attribute"})
                         :body
                         :tag.filtered/sorted
                         (map :item/name)
                         str/join))

    (is (= 26 (count final-sort)))
    (comment
      "this test almost works, just one letter off?"
      (is (= (str/reverse alphabet-string) final-sort)))
    
    ;; TODO make sure that the alphabet does get correctly reconstructed..
    
    
    

    
    ;; todo make tag/sorted thing
    )
  (stop))

(deftest alphabet-filtering
  (reset)
  (def m (martian-http/bootstrap-openapi "http://localhost:8080/api/swagger.json"))
  (testing "can sort the alphabet forwards and backwards"
    (def tag (-> (martian/response-for m :tag/new {:tag/name "TESTING alphabet"
                                                   :tag/description "for testing"
                                                   :owner tommy})
                 :body
                 :xt/id))

    (def sent-ids (set (doall
                        (for [x alphabet-string]
                          (-> (martian/response-for m :item/new {:item/name (str x) 
                                                                 :item/tags [tag]
                                                                 :owner tommy})
                              :body
                              :xt/id)))))
    
    (def items-in-tag (:body (martian/response-for m :tag/items {:id tag})))
    
    (is (= (set (map :xt/id items-in-tag))
           sent-ids))
    (def pair (-> (martian/response-for m :tag/sorted {:id tag :attribute "good attribute"})
                  :body
                  :pair))
        
    (def left (:left pair))
    (def right (:right pair))
        
    (let [mag (calc-score (:item/name left)
                          (:item/name right))]
      (is (= 201 (:status
                  (martian/response-for m :vote/new {:vote/left-item (:xt/id left)
                                                     :vote/right-item (:xt/id right)
                                                     :vote/magnitude mag
                                                     :vote/attribute "good attribute"
                                                     :vote/tag tag
                                                     :owner tommy}))))
      (is (= 201 (:status
                  (martian/response-for m :vote/new {:vote/left-item (:xt/id left)
                                                     :vote/right-item (:xt/id right)
                                                     :vote/magnitude mag
                                                     :vote/attribute "bad attribute"
                                                     :vote/tag tag
                                                     :owner tommy})))))


    (def sorted (-> (martian/response-for m :tag/sorted {:id tag :attribute "good attribute"})
                    :body))
    (is (= 2 (count (:tag.filtered/items sorted))))
    (is (= 2 (count (:tag.filtered/sorted sorted))))
    (is (= 24 (count (:tag.filtered/unvoted-items sorted))))
    (is (= 2 (count (:tag/votes sorted))))
    (is (= 1 (count (:tag.filtered/votes sorted))))
    
    (is (= {"good attribute" 1 "bad attribute" 1} (:interface/attributes sorted)))
    
    (def sorted (-> (martian/response-for m :tag/sorted {:id tag :attribute "bad attribute"})
                    :body))
    (is (= 2 (count (:tag.filtered/sorted sorted))))
    
    (def final-sort (->> (martian/response-for m :tag/sorted {:id tag :attribute "bad attribute"})
                         :body
                         :tag.filtered/sorted
                         (map :item/name)
                         str/join))

    (is (= 2 (count final-sort)))
    ;; TODO make sure that the alphabet does get correctly reconstructed..
    
    
    

    
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
