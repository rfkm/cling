(ns cling.middleware
  (:require [cling.command :as cmd]
            [cling.context :as ctx]
            [cling.help :as help]
            [cling.parser :as parser]
            [cling.process :as proc]
            [cling.response :as res]
            [cling.router :as router]
            [io.aviso.exception :refer [format-exception]]
            [plumbing.core :as p]
            [slingshot.slingshot :refer [try+]]))

(defn wrap-base [handler env]
  (fn [args]
    (handler (merge env {:arguments args}))))

(defn handle-response [res]
  (when (map? res)
    (let [{:keys [body status]} res]
      (when body
        (println body))
      (when status
        (proc/exit status))))
  res)

(defn wrap-responder [handler]
  (fn [env]
    (handle-response (handler env))))

(defn wrap-router [handler route-spec]
  (fn [env]
    (let [{path :path routed-handler :handler}
          (router/match-route route-spec (:arguments env))]
      (-> env
          (assoc :route-spec route-spec
                 :path path
                 :handler routed-handler)
          (merge (router/compile-context route-spec path))
          handler))))

(defn wrap-dispatcher [handler]
  (fn [env]
    (if-let [handler' (:handler env)]
      (handler' env)
      (handler env))))

(defn wrap-options-parser [handler]
  (fn [env]
    (let [option-specs (:option-specs env)
          parsed (parser/parse-opts (:arguments env) option-specs)]
      (-> env
          (assoc :raw-arguments (:arguments env))
          (merge parsed)
          (update-in [:arguments] #(drop (count (:path env)) %))
          handler))))

(defn wrap-arguments-parser [handler]
  (fn [env]
    (let [argument-specs (:argument-specs env)
          parsed (parser/parse-args (:arguments env) argument-specs)]
      (-> env
          (assoc :arguments (:arguments parsed))
          (update-in [:errors] concat (:errors parsed))
          handler))))

(defn wrap-help-command [handler]
  (fn [env]
    (-> env
        (assoc :help (help/create-help-fn env))
        handler)))

(defn wrap-help-option-handler [handler]
  (fn [{:keys [options help] :as env}]
    (if (:help options)
      (do (assert (fn? help))
          (help (dissoc env :errors)))
      (handler env))))

(defn wrap-error-raiser [handler]
  (fn [env]
    (if (seq (:errors env))
      (res/fail!)
      (handler env))))

(defn wrap-error-handler
  "A middleware that catches an exception thrown by
  cling.response/fail! and invoke specified error handler"
  [handler err-handler]
  (fn [env]
    (try+
     (handler env)
     (catch [:type ::res/fail] e
       (err-handler env e)))))

(defn wrap-help-fallback [handler]
  (wrap-error-handler
   handler
   (fn [{:keys [help] :as env} {:keys [message status]}]
     (assert help)
     (-> env
         (p/?> message (update-in [:errors] conj message))
         help
         (assoc :status status)))))

(defn wrap-exception-handler [handler exception-handler]
  (fn [env]
    (try
      (handler env)
      (catch Throwable e
        (exception-handler env e)))))

(defn wrap-exception-formatter [handler]
  (wrap-exception-handler handler
                          (fn [env e]
                            (res/ng (format-exception e)))))

(defn- subcommands [spec env]
  (when (map? spec)
    (map (fn [[k v]]
           (-> (ctx/get-context v)
               (assoc :name (name k))))
         spec)))

(defn- inject-subcommands [env]
  (assoc env
         :commands
         (subcommands
          (get-in (:route-spec env) (:path env))
          env)))

(defn wrap-subcommands-info [handler]
  (fn [env]
    (-> env
        inject-subcommands
        handler)))
