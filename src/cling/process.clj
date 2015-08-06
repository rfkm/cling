(ns cling.process)

(def ^:dynamic *exit-process?* false)

(defn exit
  [status]
  (if *exit-process?*
    (do
      (shutdown-agents)
      (System/exit status))
    (println "[DRY RUN] Finished with exit code" status)))
