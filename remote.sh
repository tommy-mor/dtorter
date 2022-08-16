# https://stackoverflow.com/questions/52459671/clojure-how-to-connect-to-running-repl-process-remotely
# cider-connect localhost:7888

ssh -NL 7888:localhost:7888 root@sorter.isnt.online
