(ns cling.entrypoint
  (:require [cling.context :as ctx]
            [cling.middleware :as m]
            [cling.process :as proc]
            [cling.response :as res]
            [cling.util.project :as up]
            [clojure.string :as str])
  (:import java.text.SimpleDateFormat
           java.util.Date))

(defn format-date [ts]
  (when ts
    (.format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss Z")
             (Date. ts))))

(defn default-handler [env]
  (res/fail!))

(defn create-handler [route-spec & [config]]
  (-> default-handler
      m/wrap-dispatcher
      m/wrap-error-raiser
      m/wrap-help-fallback
      m/wrap-help-option-handler
      m/wrap-help-command
      m/wrap-subcommands-info
      m/wrap-arguments-parser
      m/wrap-options-parser
      (m/wrap-router route-spec)
      m/wrap-exception-formatter
      m/wrap-responder
      (m/wrap-base config)))

(defmacro gen-cli-options [prj-id]
  `{:project-name ~(-> (name prj-id)
                       (str/split #"[-_]")
                       (->> (map str/capitalize)
                            (str/join " ")))
    :root-command nil
    :version      (up/version ~(str prj-id ".version"))
    :hash         (up/git-hash)
    :date         (format-date (up/git-timestamp))
    :option-specs
    [["-h" "--help" "Display help information"]]})

(defmacro defentrypoint [name spec & [config]]
  `(defn ~name [& args#]
     (let [ctx#  (merge (gen-cli-options ~(:project-id config (up/guess-project-id))) ~config)
           spec# (ctx/with-context ~spec (ctx/merge-context ctx# (ctx/get-context ~spec)))
           ep#   (create-handler spec# ctx#)]
       (binding [proc/*exit-process?* ~(:exit-process? config true)]
         (ep# args#)))))
