

;; TODO get rid of show map, should be calculated on clientside.
;; PROBLEM: we need to chose default attr before running q.
;; (defn conform-throwing [spec x]
;;   (let [conformed (s/conform spec x)]
;;     (if (= conformed ::s/invalid)
;;       (throw (ex-info "invalid data being sent" (s/explain-data spec x)))
;;       conformed)))




