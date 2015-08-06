(ns cling.context-test
  (:require [cling.context :as c]
            [midje.sweet :refer :all]))

(facts "merge-context"
  (fact "no inheritance"
    (c/merge-context {:desc "foo"} {}) => {}
    (c/merge-context {:desc "foo"} {:desc "bar"}) => {:desc "bar"})
  (fact "concat"
    (c/merge-context {:option-specs [:a]} {:option-specs [:b]}) => {:option-specs [:a :b]})
  (fact "remove unknown keys"
    (c/merge-context {:a :b} {:c :d}) => {}))
