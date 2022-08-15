#!/usr/bin/env -S bb --classpath /home/tommy/programming/dtorter/src/ 
(ns tdsl.parse-cli
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [tdsl.parse :as parse]
            [clojure.java.shell :as shell]))

(println "strst")

(parse/rewrite (parse/parse-files "../programming/tdsl") [])

(shell/sh "git" "add" ".")
(println "formatted tdsl files")
