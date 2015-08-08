(ns cling.router
  (:require [cling.context :as ctx]
            [cling.parser :as parser]))

(defn- result [path handler]
  {:path path :handler handler})

(defn has-child? [spec]
  (map? spec))

(defn- match-route* [spec args matched-args context]
  (cond
    (nil? spec)
    (result (vec (butlast matched-args)) nil)

    (not (has-child? spec))
    (result matched-args spec)

    (empty? args)
    (result matched-args nil)

    :else
    (let [context' (ctx/inherit-context context (ctx/get-context spec))
          parsed   (parser/parse-opts args (:option-specs context') :in-order true)
          [x & xs] (:arguments parsed)
          k        (keyword x)]
      (if (seq (:errors parsed))
        (result matched-args nil)
        (match-route* (get spec k) xs (conj matched-args k) context')))))

(defn- initial-context []
  {:option-specs []})

(defn match-route [spec args]
  (->> (initial-context)
       (match-route* spec args [])))

(defn compile-context [spec path]
  (reduce (fn [acc ks]
            (ctx/inherit-context acc (ctx/get-context (get-in spec ks))))
          {}
          (reductions conj [] path)))
