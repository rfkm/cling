(ns cling.context)

(defn- override-if-exists [a b k]
  (if (contains? b k)
    (assoc a k (get b k))
    a))

(defn- dont-inherit [a b k]
  (override-if-exists (dissoc a k) b k))

(defn- concat' [a b k]
  (if (contains? b k)
    (update-in a [k] concat (get b k))
    a))

(defmulti merge-context-item (fn [parent child k] k))

(defmethod merge-context-item :option-specs [& args]
  (apply concat' args))

(defmethod merge-context-item :argument-specs [& args]
  (apply override-if-exists args))

(defmethod merge-context-item :desc [& args]
  (apply override-if-exists args))

(defmethod merge-context-item :long-desc [& args]
  (apply override-if-exists args))

(defmethod merge-context-item :default [parent child k]
  (dissoc parent k))

(defmulti inherit-context-item (fn [parent child k] k))

(defmethod inherit-context-item :option-specs [& args]
  (apply concat' args))

(defmethod inherit-context-item :argument-specs [& args]
  (apply dont-inherit args))

(defmethod inherit-context-item :desc [& args]
  (apply dont-inherit args))

(defmethod inherit-context-item :long-desc [& args]
  (apply dont-inherit args))

(defmethod inherit-context-item :default [parent child k]
  (dissoc parent k))

(defn get-context [spec]
  (:cling/cli (meta spec)))

(defn wrap-context [m]
  {:cling/cli m})

(defn with-context [spec m]
  (with-meta spec (wrap-context m)))

(defn merge-context [a b]
  (reduce (fn [acc k]
            (merge-context-item acc b k))
          a
          (into (set (keys a)) (keys b))))

(defn inherit-context [a b]
  (reduce (fn [acc k]
            (inherit-context-item acc b k))
          a
          (into (set (keys a)) (keys b))))
