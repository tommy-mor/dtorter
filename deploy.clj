(require '[babashka.pods :as pods])
(require '[babashka.fs :as fs])

(pods/load-pod 'epiccastle/spire "0.1.0-alpha.17")

(require '[pod.epiccastle.spire.transport :refer [ssh]]
         '[pod.epiccastle.spire.module.shell :refer [shell]]
         '[pod.epiccastle.spire.module.upload :refer [upload]])

(shell {:cmd "lein uberjar"})

(def jar (first (fs/glob "./target" "*standalone.jar")))

;; https://epiccastle.io/spire/module/upload.html 
(ssh "root@sorter.isnt.online"
     (shell {:cmd "java -version"})
     (shell {:cmd "ls /root/"})
     (shell {:cmd "rm -f /root/*.jar"}))

#_(shell {:cmd (str "scp " (.getAbsolutePath (fs/file jar)) " root@sorter.isnt.online:/root/")})

(ssh "root@sorter.isnt.online"
     (shell {:cmd "ls /root/"}))










