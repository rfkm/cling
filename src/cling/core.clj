(ns cling.core
  (:require [cling
             command
             entrypoint
             response]
            [potemkin :refer [import-vars]]))

(import-vars
 [cling.entrypoint
  create-handler
  defentrypoint]
 [cling.command
  defcmd
  defcontainer]
 [cling.response
  response
  ok
  ng
  keep-alive
  fail!])
