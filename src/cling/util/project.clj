(ns cling.util.project
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn guess-project-id-from-project-clj []
  (let [f (io/file "project.clj")]
    (when-let [[_ p] (and (.exists f)
                          (re-find #"defproject\s+(\S+)" (slurp f)))]
      (last (.split p "/")))))

(defn guess-project-id-from-current-ns []
  (first (str/split (str *ns*) #"\.")))

(defn guess-project-id []
  (or (guess-project-id-from-project-clj)
      (guess-project-id-from-current-ns)))

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
