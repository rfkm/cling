(ns cling.command-test
  (:require [cling.command :as c]
            [cling.context :as ctx]
            [midje.sweet :refer :all]))


(c/defcmd foo
  "description"
  [["-f" "--foo FOO" "foo option"]
   ["-b" "--ba" "bar flag"]]
  [["arg1"]
   ["arg2" :optional? true :variadic? true]]
  [env]
  env)


(c/defcmd long-desc-cmd
  "Heading.

Long long description.
Long long description."
  [["-f" "--foo FOO" "foo option"]
   ["-b" "--ba" "bar flag"]]
  [["arg1"]
   ["arg2" :optional? true :variadic? true]]
  [env]
  env)

(facts "defcmd"
  (fact "doc"
    (:doc (meta #'foo)) => "description")
  (fact "ctx"
    (ctx/get-context foo) => {:option-specs [["-f" "--foo FOO" "foo option"]
                                             ["-b" "--ba" "bar flag"]]
                              :argument-specs [["arg1"]
                                               ["arg2" :optional? true :variadic? true]]
                              :desc "description"})

  (fact "Regard first line of multiline description as heading"
    (ctx/get-context long-desc-cmd)
    =>
    (contains {:desc "Heading."
               :long-desc "Heading.

Long long description.
Long long description."})))

(c/defcmd migrate
  "Migrate DB"
  []
  []
  [env])

(c/defcmd rollback
  "Rollback DB"
  []
  []
  [env])

(c/defcontainer db-cmds
  "a collection of db utility commands"
  [["-c" "--config CONFIG_FILE" "DB config file"]]
  {:migrate  migrate
   :rollback rollback})

(facts "defcontainer"
  db-cmds => {:migrate migrate
              :rollback rollback}
  (ctx/get-context db-cmds) => {:desc "a collection of db utility commands"
                                :option-specs [["-c" "--config CONFIG_FILE" "DB config file"]]})
