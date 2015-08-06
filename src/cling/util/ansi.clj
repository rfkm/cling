(ns cling.util.ansi
  (:require [clojure.string :as str]
            [io.aviso.ansi]
            [cling.util.string :as ustr]
            [potemkin.namespaces :refer [import-vars]]))

(def ^:dynamic *enable-ansi?* true)

(defmacro ^:private wrap-ansi [colors]
  (let [forms (for [c colors
                    prefix ["" "bold-"]
                    suffix ["" "-bg"]]
                `(defn ~(symbol (str prefix c suffix)) [text#]
                   (if *enable-ansi?*
                     (~(symbol "io.aviso.ansi" (str prefix c suffix)) text#)
                     text#)))]
    `(do
       ~@forms)))

(wrap-ansi ["black" "red" "green" "yellow" "blue" "magenta" "cyan" "white"])

(defn color-box [text ansi-fn]
  (-> (ustr/format-grid
       ""
       `[["  " "" "  "]
         ~@(map #(vector "  " % "  ") (str/split text #"\r?\n" -1))
         ["  " "" "  "]])
      (str/split #"\n" -1)
      (->> (map ansi-fn)
           (str/join "\n"))))

(defn red-box [text]
  (color-box text (comp red-bg white)))

(import-vars [io.aviso.ansi
              strip-ansi
              visual-length])
