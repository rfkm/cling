(ns cling.command
  (:require [cling.context :as ctx]
            [clojure.string :as str]
            [plumbing.core :as p]))

(defmacro defcmd
  {:arglists '([name description? option-specs argument-specs args & body])}
  [name & specs]
  (let [[desc] (take-while string? specs)
        lines (when desc (str/split-lines desc))
        short-desc (first lines)
        long-desc (when (> (count lines) 1) desc)
        [opt-specs arg-specs args & body] (drop-while string? specs)]
    `(let [f# (ctx/with-context (fn ~args ~@body)
                (-> {:option-specs ~opt-specs
                     :argument-specs ~arg-specs}
                    (p/assoc-when :desc ~short-desc
                                  :long-desc ~long-desc)))]
       (def ~(vary-meta name merge
                        {:doc desc
                         :arglists [(list 'quote args)]})
         f#))))

(defmacro defcontainer {:arglists '([name description? option-specs routes])}
  [name & specs]
  (let [[desc] (take-while string? specs)
        [opt-specs routes] (drop-while string? specs)]
    `(def ~name (ctx/with-context ~routes
                  {:option-specs ~opt-specs
                   :desc ~desc}))))
