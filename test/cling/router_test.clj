(ns cling.router-test
  (:require [cling.context :as ctx]
            [cling.router :as c]
            [midje.sweet :refer :all]))


(defn opt [flags obj]
  (ctx/with-context obj
    {:option-specs (map #(vector (str "-" (name %))
                                 nil
                                 :id %)
                        flags)}))

(defn opt-req [flags obj]
  (ctx/with-context obj
    {:option-specs (map #(vector (str "-" (name %))
                                 nil
                                 :id %
                                 :required "VAL")
                        flags)}))

(defn ret [path handler & [full-path]]
  (contains {:path path
             :handler handler
             :full-path (or full-path path)}))

(facts "match-route"
  (fact "simple"
    (c/match-route :a []) => (ret [] :a))

  (fact "map"
    (c/match-route {:a :a} ["a"]) => (ret [:a] :a))

  (fact "nested map"
    (c/match-route {:a {:a :a}} ["a" "a"]) => (ret [:a :a] :a))

  (fact "partial match"
    (c/match-route {:a {:a :a}} ["a"]) => (ret [:a] nil))

  (fact "extra args"
    (c/match-route :a ["a"]) => (ret [] :a)
    (c/match-route {:a :a} ["a" "a"]) => (ret [:a] :a))

  (fact "not found (partial match + extra args)"
    (c/match-route {:a :a} ["b"]) => (ret [] nil)
    (c/match-route {:a {:a :a}} ["a" "b"]) => (ret [:a] nil))

  (fact "catch all"
    (c/match-route {:a :a true :b} ["b"]) => (ret [] :b [true])
    (c/match-route {:a {:a :a true :b}} ["a" "b"]) => (ret [:a] :b [:a true])
    (c/match-route {:a {:a :a true {:b :c}}} ["a" "b"]) => (ret [:a :b] :c [:a true :b]))

  (facts "w/ opt"
    (fact "flag simple"
      (c/match-route (opt [:h] 'a) ["-h"]) => (ret [] 'a)
      (c/match-route (opt [:h] {:a :a}) ["-h" "a"]) => (ret [:a] :a)
      (c/match-route (opt [:h] {:a :a}) ["a" "-h"]) => (ret [:a] :a)
      (c/match-route {:a (opt [:h] 'a)} ["-h" "a"]) => (ret [] nil)
      (c/match-route {:a (opt [:h] 'a)} ["a" "-h"]) => (ret [:a] 'a))

    (fact "required simple"
      (c/match-route {:a (opt-req [:h] {:a :a})} ["a" "-h" "b" "a"]) => (ret [:a :a] :a)
      (c/match-route (opt-req [:h] 'a) ["-h" "h"]) => (ret [] 'a)
      (c/match-route (opt-req [:h] {:a :a}) ["-h" "h" "a"]) => (ret [:a] :a)
      (c/match-route (opt-req [:h] {:a :a}) ["a" "-h" "h"]) => (ret [:a] :a)
      (c/match-route {:a (opt-req [:h] 'a)} ["-h" "h" "a"]) => (ret [] nil)
      (c/match-route {:a (opt-req [:h] 'a)} ["a" "-h" "h"]) => (ret [:a] 'a))

    (fact "inheritance"
      (c/match-route (opt-req [:h] {:a {:a :a}}) ["a" "-h" "b" "a"]) => (ret [:a :a] :a))

    (fact "conflict opt"
      (c/match-route (opt [:h] {:a (opt [:h] {:a :a})}) ["a" "a"]) => (throws java.lang.AssertionError))))

(fact "compile-context"
  (c/compile-context ..spec.. ..path..) => ..compiled..
  (provided
    ..path.. =contains=> [:a :a :a]
    ..spec.. =contains=> {:a {:a {:a :a}}}
    (ctx/get-context ..spec..) => ..ctx1..
    (ctx/get-context {:a {:a :a}}) => ..ctx2..
    (ctx/get-context {:a :a}) => ..ctx3..
    (ctx/get-context :a) => ..ctx4..
    (ctx/inherit-context {} ..ctx1..) => ..ctx1'..
    (ctx/inherit-context ..ctx1'.. ..ctx2..) => ..ctx2'..
    (ctx/inherit-context ..ctx2'.. ..ctx3..) => ..ctx3'..
    (ctx/inherit-context ..ctx3'.. ..ctx4..) => ..compiled..))
