(ns cling.context-test
  (:require [cling.context :as c]
            [midje.sweet :refer :all]))

(facts "inherit-context"
  (fact "no inheritance"
    (c/inherit-context {:desc "foo"} {}) => {}
    (c/inherit-context {:desc "foo"} {:desc "bar"}) => {:desc "bar"})
  (fact "concat"
    (c/inherit-context {:option-specs [:a]} {:option-specs [:b]}) => {:option-specs [:a :b]})
  (fact "remove unknown keys"
    (c/inherit-context {:a :b} {:c :d}) => {}))

(facts "merge-context"
  (fact "override"
    (c/merge-context {:desc "foo"} {}) => {:desc "foo"}
    (c/merge-context {:desc "foo"} {:desc "bar"}) => {:desc "bar"})
  (fact "concat"
    (c/merge-context {:option-specs [:a]} {:option-specs [:b]}) => {:option-specs [:a :b]})
  (fact "remove unknown keys"
    (c/merge-context {:a :b} {:c :d}) => {}))
