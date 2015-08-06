(ns cling.middleware-test
  (:require [cling.help :as help]
            [cling.middleware :as c]
            [cling.parser :as parser]
            [cling.response :as res]
            [cling.router :as router]
            [io.aviso.exception :refer [format-exception]]
            [midje.sweet :refer :all]))

(fact "wrap-base"
  ((c/wrap-base identity {:a :b}) ..args..) => {:arguments ..args.. :a :b})

(fact "wrap-router"
  ((c/wrap-router identity ..spec..)
   {:arguments ..args..})
  =>
  {:arguments  ..args..
   :path       ..path..
   :handler    ..handler..
   :route-spec ..spec..
   :foo        :bar}
  (provided
    (router/match-route ..spec.. ..args..) => {:path ..path.. :handler ..handler..}
    (router/compile-context ..spec.. ..path..) => {:foo :bar}))

(facts "wrap-dispatcher"
  (fact "Invoke handler"
    ((c/wrap-dispatcher (constantly :foo))
     {:handler (constantly :bar)})
    =>
    :bar)

  (fact "Invoke default handler if no handler is given"
    ((c/wrap-dispatcher (constantly :foo))
     {:handler nil})
    =>
    :foo))

(fact "wrap-options-parser"
  ((c/wrap-options-parser identity)
   {:arguments ..raw-args..
    :option-specs  ..option-specs..
    :path [:a :b]})
  =>
  {:path          [:a :b]
   :arguments     ["c" "d"]
   :options       ..options..
   :option-specs  ..option-specs..
   :raw-arguments ..raw-args...}
  (provided
    (parser/parse-opts ..raw-args.. ..option-specs..)
    =>
    {:arguments ["a" "b" "c" "d"]
     :options   ..options..}))

(fact "wrap-arguments-parser"
  ((c/wrap-arguments-parser identity)
   {:arguments ..args..
    :argument-specs ..specs..
    :errors ["foo"]})
  =>
  {:arguments {:foo 100}
   :argument-specs ..specs..
   :errors ["foo" "bar"]}
  (provided
    (parser/parse-args ..args.. ..specs..)
    =>
    {:arguments {:foo 100}
     :errors ["bar"]}))

(fact "wrap-help-command"
  ((c/wrap-help-command identity)
   {:arguments ..args..})
  =>
  {:arguments ..args..
   :help      ..help-fn..}
  (provided
    (help/create-help-fn {:arguments ..args..}) => ..help-fn..))

(facts "wrap-help-option-handler"
  (fact "Invoke help command if help option is given"
    ((c/wrap-help-option-handler identity)
     {:help (constantly :help)
      :options {:help true}})
    =>
    :help)

  (fact "Invoke default handler if help option is not given"
    ((c/wrap-help-option-handler (constantly :foo))
     {:help (constantly :help)
      :options {:help false}})
    =>
    :foo)

  (fact "Throw assersion error if there is no help command"
    ((c/wrap-help-option-handler (constantly :foo))
     {:help nil
      :options {:help true}})
    =>
    (throws java.lang.AssertionError #"fn\? help")))

(facts "wrap-error-raiser"
  (fact "Throw an exception when errors exist"
    ((c/wrap-error-raiser identity)
     {:errors ["foo"]})
    =>
    (throws clojure.lang.ExceptionInfo))

  (fact "Do nothing if there is no error"
    ((c/wrap-error-raiser identity)
     {:errors []})
    =>
    {:errors []}))

(facts "wrap-error-handler"
  ((c/wrap-error-handler (fn [_] (res/fail! "error!" 1))
                         vector)
   ..env..)
  =>
  [..env.. {:type ::res/fail :message "error!" :status 1}])

(facts "wrap-help-fallback"
  (fact "w/o error message"
    ((c/wrap-help-fallback (fn [_] (res/fail! nil)))
     {:help identity})
    =>
    {:help identity
     :status 1})

  (fact "w/ error message"
    ((c/wrap-help-fallback (fn [_] (res/fail! "error!" 1)))
     {:help identity})
    =>
    {:help identity
     :errors ["error!"]
     :status 1}))

(facts "wrap-exception-handler"
  (let [ex (Exception. "error!")]
    ((c/wrap-exception-handler (fn [_] (throw ex))
                               vector)
     ..env..)
    =>
    (just [..env.. ex])))

(facts "wrap-exception-formatter"
  (let [ex (Exception. "error!")]
    ((c/wrap-exception-formatter (fn [_] (throw ex)))
     ..env..)
    =>
    {:status 1
     :body ..formated..}
    (provided
      (format-exception ex) => ..formated..)))
