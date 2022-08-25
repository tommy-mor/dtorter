(ns tdsl.show
  (:require [tdsl.parse :as parse]
            [cheshire.core :as json]
            [io.pedestal.interceptor.chain :refer [terminate]]
            [io.pedestal.http.route :refer [url-for]]
            [ring.util.response :as ring-resp]
            [clojure.string :as str]
            [dtorter.views.common :refer [html-interceptor]]
            [reitit.core :as r]

            [clj-jgit.porcelain :as g]))

(defn display [dir]
  (def files (parse/parse-files dir))
  (def thoughts (sort-by :name files))

  (->> thoughts
       (filter (comp (complement nil?) :body))
       (filter (comp not #{(keyword "(ns")} :name))))

(defn only-users [users]
  {:name :filter :enter
   #(if (-> % :request :session :user-name users)
      %
      (terminate
       (assoc % :response {:status 404})))})

(defn todopage [req]
  {:status 200
   :headers {"X-Frame-Options" "SAMEORIGIN"}
   :html
   [:html
    [:head
     [:link {:href "/css/site.css"
             :rel "stylesheet"
             :type "text/css"}]
     [:script {:src "/js/shared.js"
               :type "text/javascript"}]
     [:script {:src "/js/tdsl.js"
               :type "text/javascript"}]
     [:script {:src "/js/tdsl-todo.js"
               :type "text/javascript"}]
     [:script (str "const dir = '" "tdsl" "';")]
     [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
     ]
    [:div#app]
    [:script "frontdsl.todopage.run(" (json/generate-string (first
                                                             (filter #(= (:name %) :todo/concurrent)
                                                                     (display "tdsl")))) ")"]]
   })

(defn page [req]
  (def req req)
  (let [dir (-> req
                :path-params
                :base)
        username (-> req
                     :session
                     :user-name)
        access (or (and (= dir "tdsl")
                        (#{"tommy"} username))
                   (and (= dir "egregore")
                        (#{"tommy" "blobbed"} username)))]

    (def dir dir)
    (def username username)
    (def access access)
    (if-not access (throw (ex-info "access denied" req)))
    
    {:status 200
     :html [:html
            [:head
             [:link {:href "/css/site.css"
                     :rel "stylesheet"
                     :type "text/css"}]
             [:script {:src "/js/shared.js"
                       :type "text/javascript"}]
             [:script {:src "/js/tdsl.js"
                       :type "text/javascript"}]
             [:script (str "const dir = '" dir "';")]
             [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]]
            [:div#app]
            [:script "frontdsl.page.run(" (json/generate-string (display dir)) ")"]]}))

(defn refresh
  [req]
  (def req req)
  (let [page  (-> req
                  :headers
                  (get "referer")
                  (str/split #"/")
                  last)
        query-params (-> req
                         :cookies
                         (get "query")
                         :value)]
    (parse/update-files page)
    (ring-resp/redirect (str (r/match->path (r/match-by-name (::r/router req) :tdsl-page {:base page})) "#" query-params))))

(defn rewrite [req]
  (def req req)
  (def dir (-> req
               :parameters
               :path
               :base))
  (def thoughts (-> req :body-params))
  (def files (parse/parse-files dir))
  (-> req :body-params)
  (parse/rewrite files (-> req
                           :body-params))
  {:status 200 :body ""})

(defn routes []
  [["/todo.concurrent"
    {:get {:handler todopage}
     :name :todo-page
     :interceptors [html-interceptor]}]
   ["/b/:base"
    {:get {:handler page}
     :name :tdsl-page
     :parameters {:path {:base string?}}
     :interceptors [html-interceptor (only-users #{"tommy"})]}]
   ["/b/:base/update"
    {:post {:handler rewrite}
     :name :tdsl-write
     :parameters {:path {:base string?}}
     :interceptors [html-interceptor (only-users #{"tommy"})]}]
   ["/refresh"
    {:get {:handler refresh}
     :interceptors [(only-users #{"tommy"})]}]])
