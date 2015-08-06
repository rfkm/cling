(ns cling.util.project
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]))

(defn guess-project-id []
  (first (str/split (str *ns*) #"\.")))

(defmacro version [key]
  (System/getProperty key))

(defmacro git-hash []
  (try
    (let [ret (sh/sh "git" "rev-parse" "HEAD")]
      (when (zero? (:exit ret)) (when-let [h (:out ret)] (str/trim h))))
    (catch Throwable _)))

(defmacro git-timestamp []
  (try
    (let [ret (sh/sh  "git" "show" "-s" "--format=%ct" "HEAD")]
      (when (zero? (:exit ret))
        (when-let [h (:out ret)]
          (* (Integer/parseInt (str/trim h)) 1000))))
    (catch Throwable _)))
