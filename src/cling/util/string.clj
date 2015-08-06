(ns cling.util.string
  (:require [clojure.string :as str]))

(defn pad [s width]
  (apply str s (repeat (- width (count s)) " ")))

(defn indent [s n]
  (->> (str/split s #"\n" -1)
       (map #(str (when (seq %) (apply str (repeat n " "))) %))
       (str/join "\n")))

(defn labeled-content [label content]
  (str label "\n" (indent content 2)))

(defn- max-length [& xs]
  (apply max (map count xs)))

(defn- calc-max-widths [rows]
  (apply map max-length rows))

(defn- format-row [widths sep cols]
  (->> (if (> (count cols) 1)
         (conj (mapv pad (butlast cols) widths)
               (last cols))
         (mapv pad cols widths))
       (str/join sep)))

(defn format-grid [sep rows]
  (->> rows
       (map (partial format-row (calc-max-widths rows) sep))
       (str/join "\n")))
