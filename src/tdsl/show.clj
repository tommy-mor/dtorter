(ns tdsl.show
  (:require [tdsl.parse :as parse]
            [cheshire.core :as json]
            [io.pedestal.interceptor.chain :refer [terminate]]
            [io.pedestal.http.route :refer [url-for]]
            [ring.util.response :as ring-resp]
            [clojure.string :as str]
            [dtorter.views.common :refer [html-interceptor layout-interceptor]]
            [reitit.core :as r]

            [clj-jgit.porcelain :as g]
            [tick.core :as t]
            [tick.alpha.interval :as t.i]
            [clojure.java.shell :as shell]))

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

(def goals
  [:div
   [:div
    {:style "display:flex; justify-content: space-around"}
    [:div 
     [:a {:href "https://course.ccs.neu.edu/cs3650f22/schedule.html"} "systems"]
     [:br]
     [:a {:href "https://piazza.com/class/l7jbp35d4i9nr"} "piazza"]]
    [:div
     [:a {:href "https://khoury.northeastern.edu/~derakhshan/CS4810-F22/course-info.html"} "algo"]
     [:br]
     [:a {:href  "https://piazza.com/class/l5x2fyu9chz5i7"} "piazza"]]
    [:div
     [:a {:href "https://3700.network/"} "networks"]
     [:br]
     [:a {:href "https://piazza.com/class/l7kwgqgh3pd6vc"} "piazza"]]]
   [:br]
   
   [:b "program"]
   [:pre "
     follow gcal/habits.
     fill out food in gsheet.

     then work through :concurrent/todo
     use linear.app for all school and person"
    ]])

(defn todopage [req]
  ;; https://stackoverflow.com/a/10113231
  
  (shell/sh "git" "add" "." :dir (str "../tdsl"))
  (shell/sh "git" "commit" "-am\"refreshed during 5m window\"" :dir (str "../tdsl"))
  (shell/sh "git" "fetch" "origin" "main" :dir (str "../tdsl"))
  (shell/sh "git" "merge" "-s" "recursive" "-X" "theirs" "origin/main" "-m" "merge" :dir (str "../tdsl"))
  {:status 200
   :headers {"X-Frame-Options" "SAMEORIGIN"}
   :html
   [:html
    [:head
     [:script {:src "/js/tdsl.js"
               :type "text/javascript"}]
     [:script {:src "/js/tdsl-todo.js"
               :type "text/javascript"}]
     [:script (str "const dir = '" "tdsl" "';")]
     [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
     ]
    goals
    [:div#app]
    [:script "frontdsl.todopage.run(" (json/generate-string (first
                                                             (filter #(= (:name %) :todo/concurrent)
                                                                     (display "tdsl")))) ")"]]})

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
             [:script (str "const dir = '" dir "';")]
             [:script {:src "/js/shared.js"
                       :type "text/javascript"}]
             [:script {:src "/js/tdsl.js"
                       :type "text/javascript"}]
             [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]]
            [:div#app]
            [:script "frontdsl.page.run(" (json/generate-string (display dir)) ")"]]}))

(defn rewrite [req]
  (def req req)
  (def dir (-> req
               :parameters
               :path
               :base))
  (def thoughts (-> req :body-params))
  (def files (parse/parse-files dir))
  (parse/rewrite files (-> req :body-params))
  ;; we cant pull, because my ssh key is password protected.
  ;; i don't want to fix that thus this hac

  (def repo (g/load-repo (str "../" dir)))
  (g/git-status repo)
  
  (if (t/> (t/duration (t.i/new-interval (t/instant (:date (:author (first (g/git-log repo :max-count 1)))))
                                         (t/now)))
           (t/new-duration 10 :minutes))
    (shell/sh "git" "commit" "-a" "-m" "change from tdslweb" :dir (str "../tdsl"))
    (shell/sh "git" "commit" "-a" "--amend" "--no-edit" :dir (str "../tdsl")))
  
  (shell/sh "git" "push" "-f" :dir (str "../tdsl"))
  {:status 200 :body (parse/parse-files dir)})

(defn get-todos [req]
  {:status 200
   :body (-> (parse/parse-files "tdsl")
             parse/find-todos
             parse/collapse-li
             )})

(defn routes []
  [["/todo.concurrent"
    {:get {:handler todopage}
     :name :todo-page
     :interceptors [html-interceptor layout-interceptor (only-users #{"tommy"})]}]
   ["/b"
    {:parameters {:path {:base string?}}
     :interceptors [html-interceptor (only-users #{"tommy"})]}
    
    ["/:base"
     {:get {:handler page}
      :name :tdsl-page
      :interceptors [layout-interceptor]}]
    
    ["/:base/update"
     {:post {:handler rewrite}
      :name :tdsl-write}]
    
    ["/:base/todo"
     {:post {:handler get-todos}
      :get {:handler get-todos}
      
      
      :name :tdsl-todo}]]])
