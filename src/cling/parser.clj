(ns cling.parser
  (:require [clojure.tools.cli :as cli]))

;;; TODO: Extend syntax
(defn compile-option-specs [option-specs]
  (#'cli/compile-option-specs option-specs))

(defn parse-opts [args option-specs & options]
  (apply cli/parse-opts args option-specs options))

(def ^:private arg-spec-keys
  [:id :name :parse-fn :validate :optional? :variadic? :default])

(defn- distinct?* [coll]
  (if (seq coll)
    (apply distinct? coll)
    true))

(defn compile-argument-spec [spec]
  (let [id-name                    (take-while #(or (string? %) (nil? %)) spec)
        spec-map                   (apply hash-map (drop (count id-name) spec))
        [id arg-name]              id-name
        validate                   (:validate spec-map)
        [validate-fn validate-msg] (when (seq validate)
                                     (->> (partition 2 2 (repeat nil) validate)
                                          (apply map vector)))]
    (merge {:parse-fn identity}
           {:id (keyword id) :name (or arg-name id)
            :validate-fn validate-fn
            :validate-msg validate-msg}
           (select-keys (dissoc spec-map :validate) arg-spec-keys))))

(defn compile-argument-specs [argument-specs]
  {:post [(every? :id %)
          (distinct?* (map :id %))
          (every? :optional? (drop-while (complement :optional?) %))
          (> 2 (count (drop-while (complement :variadic?) %)))]}
  (map compile-argument-spec argument-specs))

(defn- parse-error [arg-name msg]
  (str "Error while parsing option " arg-name ": " msg))

(defn- validation-error [arg-name msg]
  (str "Failed to validate " arg-name
       (if msg (str ": " msg) "")))

(defn- validate-argument [value spec]
  (let [{:keys [validate-fn validate-msg]} spec]
    (or (loop [[vfn & vfns] validate-fn [msg & msgs] validate-msg]
          (when vfn
            (if (try (vfn value) (catch Throwable e))
              (recur vfns msgs)
              [nil (validation-error (:name spec) msg)])))
        [value nil])))

(defn parse-argument-spec [arg spec]
  {:pre [(fn? (:parse-fn spec))]}
  (try
    (let [val ((:parse-fn spec) arg)
          [val err] (validate-argument val spec)
          val (if (:variadic? spec) [val] val)]
      [val err])
    (catch Throwable e
      [nil (parse-error (:name spec) (str e))])))

(defn parse-argument-specs [args specs]
  (loop [args   args
         specs  specs
         parsed []
         errors []]
    (let [arg (first args)
          spec (first specs)]
      (cond
        (and (empty? args) (empty? specs))
        {:errors errors
         :arguments (apply merge-with concat parsed)}

        (and (seq args) (empty? specs))
        (recur [] [] parsed (conj errors (validation-error nil "Too many arguments")))

        (and (empty? args) (seq specs))
        (if (:optional? spec)
          (if (contains? spec :default)
            (recur [] [] (conj parsed {(:id spec) (:default spec)}) errors)
            (recur [] [] parsed errors))
          (recur [] [] parsed (conj errors (validation-error nil "Too few arguments"))))

        :else
        (let [[val err] (parse-argument-spec arg spec)]
          (recur (rest args)
                 (if (:variadic? spec)
                   [(assoc spec :optional? true)]
                   (rest specs))
                 (if err parsed (conj parsed {(:id spec) val}))
                 (if err (conj errors err) errors)))))))

(defn- omit-leading-seps [args]
  (drop-while (partial = "--") args))

(defn parse-args [args argument-specs]
  (let [compiled-specs (compile-argument-specs argument-specs)]
    (parse-argument-specs (omit-leading-seps args) compiled-specs)))
