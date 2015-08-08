(ns cling.entrypoint-test
  (:require [cling.command :as cmd]
            [cling.entrypoint :as c]
            [cling.util.ansi :refer [*enable-ansi?*]]
            [clojure.string :as str]
            [midje.sweet :refer :all]))

(cmd/defcmd migrate
  "Migrate DB.
aaaaaaaaaaaaaaaaaaaaaaaa.
bbbbbbbbbbbbbbbbbbbbbbbb."
  [["-s" "--schema SCHEMA" "Schema."]]
  [["version" :optional? true]]
  [env]
  :migrate)

(cmd/defcmd rollback
  "Rollback DB.
aaaaaaaaaaaaaaaaaaaaaaaa.
bbbbbbbbbbbbbbbbbbbbbbbb."
  []
  [["count" :optional? true]]
  [env]
  :rollback)

(cmd/defcontainer db
  "DB utilities"
  [["-c" "--config CONFIG" "DB Config file"]]
  {:migrate migrate
   :rollback rollback})

(cmd/defcmd server
  "Run server."
  [["-p" "--port PORT" "Port number."]]
  []
  [env]
  env)

(cmd/defcontainer root
  "Awesome CLI Application!"
  []
  {:db db
   :server server})

(c/defentrypoint ep
  root
  {:project-name  "Cling"
   :version       "1.3.0"
   :date          "2015-10-29 14:20:30"
   :hash          "1a2b3c4d5e"
   :exit-process? false})

(defn remove-last-line [s]
  (str/join "\n" (drop-last 2 (str/split s #"\n" -1))))

(binding [*enable-ansi?* false]
  (facts
    (remove-last-line (with-out-str (ep)))
    =>
    "Cling version 1.3.0 (1a2b3c4d5e) 2015-10-29 14:20:30

Usage:
  (lein run [--] | java -jar <jar>) <command> [<options>] [<arguments>]

Description:
  Awesome CLI Application!

Options:
  --help (-h)  Display help information

Commands:
  db      DB utilities
  server  Run server."

    (remove-last-line (with-out-str (ep "db")))
    =>
    "Cling version 1.3.0 (1a2b3c4d5e) 2015-10-29 14:20:30

Usage:
  (lein run [--] | java -jar <jar>) db <command> [<options>] [<arguments>]

Description:
  DB utilities

Options:
  --help (-h)           Display help information
  --config (-c) CONFIG  DB Config file

Commands:
  migrate   Migrate DB.
  rollback  Rollback DB."

    (ep "db" "migrate") => :migrate

    (remove-last-line (with-out-str (ep "db" "migrate" "-h")))
    =>
    "Cling version 1.3.0 (1a2b3c4d5e) 2015-10-29 14:20:30

Usage:
  (lein run [--] | java -jar <jar>) db migrate [<options>] [<version>]

Description:
  Migrate DB.
  aaaaaaaaaaaaaaaaaaaaaaaa.
  bbbbbbbbbbbbbbbbbbbbbbbb.

Options:
  --help (-h)           Display help information
  --config (-c) CONFIG  DB Config file
  --schema (-s) SCHEMA  Schema."))
