(defproject fm.caleb/failjure-logger "0.1.5-SNAPSHOT"
  :description "Some utilities for automatically logging failures in the Failjure library"
  :url "http://github.com/caleb/failjure-logger"
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [failjure "0.1.2"]]
  :plugins [[s3-wagon-private "1.2.0"]]
  :repositories [["s3" {:url "http://caleb-maven2.s3.amazonaws.com/repo"}]
                 ["s3-releases" {:url "s3p://caleb-maven2/repo" :creds :gpg}]])
