(ns cling.help-test
  (:require [cling.help :as c]
            [cling.util.ansi :as ansi]
            [clojure.string :as str]
            [midje.sweet :refer :all]))


(facts "arg-specs->synopsis"
  (fact "simple"
    (c/arg-specs->synopsis [["foo"] ["bar"]])
    =>
    "<foo> <bar>")

  (fact "optional"
    (c/arg-specs->synopsis [["foo"] ["bar" :optional? true]])
    =>
    "<foo> [<bar>]")

  (fact "optional + variadic"
    (c/arg-specs->synopsis [["foo"] ["bar" :optional? true :variadic? true]])
    =>
    "<foo> [<bar> ...]")

  (fact "variadic"
    (c/arg-specs->synopsis [["foo"] ["bar" :variadic? true]])
    =>
    "<foo> <bar> [<bar> ...]"))

(def spec {:project-name "<project>"
           :version "<version>"
           :hash "<sha1>"
           :date "<date>"
           :root-command "<self>"
           :path ["foo" "bar"]
           :option-specs [["-h" "--help" "Display this help message"]
                          ["-f" "--foo VAL" "Do foo" :default "foo"]
                          [nil "--bar VAL" "Do bar"]
                          [nil "--baz" "Do baz"]
                          ["-a" nil "Do a" :id :a]]
           :commands [{:name "foo" :desc "Do foo"}
                      {:name "foobar" :desc "Do foobar"}]})

(def subcmd-spec (-> spec
                     (assoc :argument-specs [["arg1"] ["arg2" :variadic? true]])
                     (dissoc :commands)))

(binding [ansi/*enable-ansi?* false]
  (facts "help-format"
    (fact "help for command that has children"
      (c/help-format spec)
      =>
      "<project> version <version> (<sha1>) <date>

Usage:
  <self> foo bar <command> [<options>] [<arguments>]

Options:
  --help (-h)     Display this help message
  --foo (-f) VAL  Do foo (default: \"foo\")
  --bar VAL       Do bar
  --baz           Do baz
  -a              Do a

Commands:
  foo     Do foo
  foobar  Do foobar")

    (fact "help for subcommand"
      (c/help-format subcmd-spec)
      =>
      "<project> version <version> (<sha1>) <date>

Usage:
  <self> foo bar [<options>] <arg1> <arg2> [<arg2> ...]

Options:
  --help (-h)     Display this help message
  --foo (-f) VAL  Do foo (default: \"foo\")
  --bar VAL       Do bar
  --baz           Do baz
  -a              Do a")

    (fact "error"
      (->> (assoc spec :errors ["error1" "error2"])
           c/help-format
           str/split-lines
           (drop 2)
           (take 4))
      => ["          "
          "  error1  "
          "  error2  "
          "          "])))
