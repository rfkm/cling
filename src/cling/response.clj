(ns cling.response
  (:require [slingshot.slingshot :refer [throw+]]))

(defn response
  ([status]
   (response status nil))
  ([status body]
   {:status status
    :body body}))

(defn ok
  ([] (response 0))
  ([body] (response 0 body)))

(defn ng
  ([] (response 1))
  ([body] (response 1 body)))

(defn keep-alive []
  (response nil))

(defn fail!
  ([]
   (fail! nil))
  ([message]
   (fail! message 1))
  ([message status]
   (throw+ {:type ::fail :message message :status status})))
