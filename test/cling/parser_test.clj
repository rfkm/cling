(ns cling.parser-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [cling.parser :refer [parse-opts] :as c]
            [midje.sweet :refer :all]))

(defn parse-int [x]
  (Integer/parseInt x))

(facts "compile-argument-spec"
  (let [ret-base {:id :foo :name "foo" :parse-fn identity :validate-fn nil :validate-msg nil}]
    (fact "simple"
      (c/compile-argument-spec ["foo"]) => ret-base)
    (fact "name"
      (c/compile-argument-spec ["foo" "foobar"]) => (assoc ret-base :name "foobar")
      (c/compile-argument-spec ["foo" "foobar" :name "baz"]) => (assoc ret-base :name "baz"))
    (fact "trailing key-value pairs"
      (c/compile-argument-spec ["foo" :parse-fn parse-int]) => (assoc ret-base :parse-fn parse-int))
    (fact "unknown key will be ignored"
      (c/compile-argument-spec ["foo" :unknown-key :foo]) => ret-base)
    (fact "validate"
      (c/compile-argument-spec ["foo" :validate [integer? "Must be an integer"
                                                 string? "Must be a string"]])
      =>
      (assoc ret-base
             :validate-fn [integer? string?]
             :validate-msg ["Must be an integer"
                            "Must be a string"]))))

(facts "compile-argument-specs"
  (background
   (c/compile-argument-spec ..spec..) => {:id :foo}
   (c/compile-argument-spec ..spec2..) => {:id nil}
   (c/compile-argument-spec ..spec3..) => {:id :bar :optional? true}
   (c/compile-argument-spec ..spec4..) => {:id :baz :optional? true}
   (c/compile-argument-spec ..spec5..) => {:id :qux :variadic? true})

  (fact
    (c/compile-argument-specs []) => []
    (c/compile-argument-specs [..spec..]) => [{:id :foo}])

  (fact "Must have id"
    (c/compile-argument-specs [..spec2..]) => (throws java.lang.AssertionError))
  (fact "Must have unqiue id"
    (c/compile-argument-specs [..spec.. ..spec..]) => (throws java.lang.AssertionError))
  (fact "`optional?` option is allowed only with trailing specs"
    (c/compile-argument-specs [..spec.. ..spec3.. ..spec4..]) => [{:id :foo}
                                                                  {:id :bar :optional? true}
                                                                  {:id :baz :optional? true}]
    (c/compile-argument-specs [..spec3.. ..spec..]) => (throws java.lang.AssertionError))
  (fact "`variadic?` option is allowed only in a last spec"
    (c/compile-argument-specs [..spec5..]) =not=> (throws java.lang.AssertionError)
    (c/compile-argument-specs [..spec.. ..spec5..]) =not=> (throws java.lang.AssertionError)
    (c/compile-argument-specs [..spec5.. ..spec..]) => (throws java.lang.AssertionError)))

(facts "parse-argument-specs"
  (fact "simple"
    (c/parse-argument-specs ["1" "2"]
                            [{:id :a :parse-fn parse-int}
                             {:id :b :parse-fn parse-int}])
    =>
    {:errors []
     :arguments {:a 1 :b 2}})

  (fact "too many arguments"
    (c/parse-argument-specs ["1" "2" "3"]
                            [{:id :a :parse-fn parse-int}
                             {:id :b :parse-fn parse-int}])
    =>
    {:errors ["Failed to validate : Too many arguments"]
     :arguments {:a 1 :b 2}})

  (fact "too few arguments"
    (c/parse-argument-specs ["1"]
                            [{:id :a :parse-fn parse-int}
                             {:id :b :parse-fn parse-int}])
    =>
    {:errors ["Failed to validate : Too few arguments"]
     :arguments {:a 1}})

  (fact "optional"
    (c/parse-argument-specs ["1" "2"]
                            [{:id :a :parse-fn parse-int}
                             {:id :b :parse-fn parse-int :optional? true}
                             {:id :c :parse-fn parse-int :optional? true}
                             {:id :d :parse-fn parse-int :optional? true}])
    =>
    {:errors []
     :arguments {:a 1 :b 2}})

  (fact "default"
    (c/parse-argument-specs []
                            [{:id :a :default 1 :optional? true}])
    =>
    {:errors []
     :arguments {:a 1}})

  (fact "variadic+"
    (c/parse-argument-specs ["1" "2" "3" "4"]
                            [{:id :a :parse-fn parse-int}
                             {:id :b :parse-fn parse-int :variadic? true}])
    =>
    {:errors []
     :arguments {:a 1 :b [2 3 4]}}

    (c/parse-argument-specs ["1"]
                            [{:id :a :parse-fn parse-int}
                             {:id :b :parse-fn parse-int :variadic? true}])
    =>
    {:errors ["Failed to validate : Too few arguments"]
     :arguments {:a 1}})

  (fact "variadic*"
    (c/parse-argument-specs ["1"]
                            [{:id :a :parse-fn parse-int}
                             {:id :b :parse-fn parse-int :variadic? true :optional? true}])
    =>
    {:errors []
     :arguments {:a 1}})

  (fact "parsing error"
    (c/parse-argument-specs ["foo"]
                            [{:id :a :parse-fn parse-int}])
    =>
    {:errors ["Error while parsing option : java.lang.NumberFormatException: For input string: \"foo\""]
     :arguments nil})

  (fact "validation error"
    (c/parse-argument-specs ["100"]
                            [{:id :a :parse-fn parse-int
                              :validate-fn [#(< 200 %) odd?]
                              :validate-msg ["too small" "must be an odd number"]}])
    =>
    {:errors ["Failed to validate : too small"]
     :arguments nil}

    (c/parse-argument-specs ["202"]
                            [{:id :a :parse-fn parse-int
                              :validate-fn [#(< 200 %) odd?]
                              :validate-msg ["too small" "must be an odd number"]}])
    =>
    {:errors ["Failed to validate : must be an odd number"]
     :arguments nil}))

(facts "parse-args"
  (fact "can parse"
    (c/parse-args ..args.. ..specs..) => ..parsed..
    (provided
      (c/compile-argument-specs ...specs...) => ..compiled-specs..
      (c/parse-argument-specs ..args.. ..compiled-specs..) => ..parsed..)))


;;; Copied from clojure.tools.cli's test --

(defn has-error? [re coll]
  (seq (filter (partial re-seq re) coll)))

(deftest test-parse-opts
  (testing "parses options to :options"
    (is (= (:options (parse-opts ["-abp80"] [["-a" "--alpha"]
                                             ["-b" "--beta"]
                                             ["-p" "--port PORT"
                                              :parse-fn parse-int]]))
           {:alpha true :beta true :port (int 80)})))
  (testing "collects error messages into :errors"
    (let [specs [["-f" "--file PATH"
                  :validate [#(not= \/ (first %)) "Must be a relative path"]]
                 ["-p" "--port PORT"
                  :parse-fn parse-int
                  :validate [#(< 0 % 0x10000) "Must be between 0 and 65536"]]]
          errors (:errors (parse-opts ["-f" "/foo/bar" "-p0"] specs))]
      (is (has-error? #"Must be a relative path" errors))
      (is (has-error? #"Must be between 0 and 65536" errors))))
  (testing "collects unprocessed arguments into :arguments"
    (is (= (:arguments (parse-opts ["foo" "-a" "bar" "--" "-b" "baz"]
                                   [["-a" "--alpha"] ["-b" "--beta"]]))
           ["foo" "bar" "-b" "baz"])))
  (testing "provides an option summary at :summary"
    (is (re-seq #"-a\W+--alpha" (:summary (parse-opts [] [["-a" "--alpha"]])))))
  (testing "processes arguments in order when :in-order is true"
    (is (= (:arguments (parse-opts ["-a" "foo" "-b"]
                                   [["-a" "--alpha"] ["-b" "--beta"]]
                                   :in-order true))
           ["foo" "-b"])))
  (testing "does not merge over default values when :no-defaults is true"
    (let [option-specs [["-p" "--port PORT" :default 80]
                        ["-H" "--host HOST" :default "example.com"]
                        ["-q" "--quiet" :default true]
                        ["-n" "--noop"]]]
      (is (= (:options (parse-opts ["-n"] option-specs))
             {:port 80 :host "example.com" :quiet true :noop true}))
      (is (= (:options (parse-opts ["-n"] option-specs :no-defaults true))
             {:noop true}))))
  (testing "accepts optional summary-fn for generating options summary"
    (is (= (:summary (parse-opts [] [["-a" "--alpha"] ["-b" "--beta"]]
                                 :summary-fn (fn [specs]
                                               (str "Usage: myprog ["
                                                    (str/join \| (map :long-opt specs))
                                                    "] arg1 arg2"))))
           "Usage: myprog [--alpha|--beta] arg1 arg2"))))
