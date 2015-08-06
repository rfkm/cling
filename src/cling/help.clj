(ns cling.help
  (:require [cling.command :as cmd]
            [cling.parser :as parser]
            [cling.response :as res]
            [cling.util.ansi :as ansi]
            [cling.util.string :as ustr]
            [clojure.string :as str]))

(defn help-header [title & {:keys [version hash date]}]
  (when title
    (str (ansi/cyan title)
         (when version
           (str " version "
                (ansi/yellow version)
                (when hash
                  (str " (" hash ")"))
                (when date
                  (str " " date)))))))

(defn- arg-spec->synopsis [{:keys [id optional? variadic?] :as spec}]
  (str
   (when optional? "[")
   "<" (name id) ">"
   (when (and optional? variadic?)
     " ...")
   (when optional? "]")
   (when (and (not optional?) variadic?)
     (str " "
          (arg-spec->synopsis (assoc spec
                                     :variadic? true
                                     :optional? true))))))

(defn arg-specs->synopsis [specs]
  (->> specs
       parser/compile-argument-specs
       (map arg-spec->synopsis)
       (str/join " ")))

(defn help-signature [root-command path has-subcmds? has-options? argument-specs]
  (let [root-command (or root-command "(lein run [--] | java -jar <jar>)")]
    (ustr/labeled-content
     (ansi/yellow "Usage:")
     (str (when (seq root-command)
            (str root-command " "))
          (when (seq path)
            (str (str/join " " (map name path))
                 " "))
          (when has-subcmds?
            (str "<command> "))
          (when has-options?
            (str "[<options>] "))
          (if has-subcmds?
            "[<arguments>]"
            (arg-specs->synopsis argument-specs))))))

(defn- opt-left-col [{:keys [short-opt long-opt required desc default]}]
  (str
   (if (and short-opt
            long-opt)
     (str long-opt " (" short-opt ")")
     (or long-opt short-opt))
   (when required (str " " required))))

(defn- opt-right-col [{:keys [required desc default default-desc]}]
  (str
   desc
   (when (and required default)
     (str " (default: " (or default-desc (pr-str default)) ")"))))

(defn help-options [option-specs]
  (when (seq option-specs)
    (ustr/labeled-content
     (ansi/yellow "Options:")
     (->> (parser/compile-option-specs
           option-specs)
          (map (juxt (comp ansi/green opt-left-col) opt-right-col))
          (ustr/format-grid "  ")))))

(defn help-commands [commands]
  (when (seq commands)
    (ustr/labeled-content
     (ansi/yellow "Commands:")
     (->> commands
          (map (juxt (comp ansi/green :name) :desc))
          (ustr/format-grid "  ")))))

(defn help-errors [errors]
  (when (seq errors)
    (ansi/red-box (str/join "\n" errors))))

(defn help-description [desc]
  (when (seq desc)
    (ustr/labeled-content
     (ansi/yellow "Description:")
     desc)))

(defn help-format [{:keys [project-name version hash date option-specs desc long-desc
                           argument-specs commands root-command path errors]}]
  (->> [(help-header project-name :version version :hash hash :date date)
        (help-errors errors)
        (help-signature root-command path (seq commands) (seq option-specs) argument-specs)
        (help-description (or long-desc desc))
        (help-options option-specs)
        (help-commands commands)]
       (remove nil?)
       (str/join "\n\n")))

;;; --

(cmd/defcmd help-cmd
  "Display help information"
  []
  []
  [env]
  (-> env
      help-format
      res/ok))

(defn create-help-fn [env]
  ;; currently ignore `env` and just return `help-cmd`
  help-cmd)
