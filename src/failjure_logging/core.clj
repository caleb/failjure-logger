(ns failjure-logging.core
  (:require [clojure.algo.monads :refer :all]
            [clojure.test :refer [deftest is run-tests]]
            [failjure.core :refer [error-m fail failed? message]]))

(defprotocol IErrorLogger
  (log [self error] "Logs the passed in error"))
(defprotocol IVectorErrorLogger
  (history [self] "Returns the vector containing logged entries"))

(defrecord StandardOutputErrorLogger []
  IErrorLogger
  (log [self error]
    (println (message error))))
(defn standard-output-error-logger []
  (->StandardOutputErrorLogger))

(defrecord VectorErrorLogger [history]
  IErrorLogger
  (log [_self error] (swap! history conj error))
  IVectorErrorLogger
  (history [_self] @history))

(defn vector-error-logger []
  (map->VectorErrorLogger {:history (atom [])}))

(def ^:dynamic *default-logger* (->StandardOutputErrorLogger))
(defn set-default-logger [logger]
  (alter-var-root (var *default-logger*) (constantly logger)))

(defn logging-error-m
  ([] (logging-error-m *default-logger*))
  ([logger]
   (monad-transformer error-m :m-plus-default
                      [m-bind (with-monad error-m
                                (fn [mv f]
                                  (when (failed? mv)
                                    (log logger mv))
                                  (m-bind mv f)))
                       m-result (with-monad error-m m-result)])))

(defn log-attempt->* [logger]
  (fn
    ([start] `(domonad (logging-error-m logger) [x# ~start] x#))
    ([start form] `(domonad (logging-error-m logger) [x# (-> ~start ~form)] x#))
    ([start form & forms]
     (println start form forms)
     `(let [new-start# (recur ~start ~form)]
        (if (failed? new-start#)
          new-start#
          (recur new-start# ~@forms))))))

(defmacro log-attempt->
  ([& args]
   (let [start-or-logger (first args)
         logger-supplied? (if (satisfies? IErrorLogger start-or-logger)
                            start-or-logger
                            nil)
         logger (if logger-supplied?
                  start-or-logger
                  *default-logger*)
         forms (if logger-supplied?
                 (nthrest args 1)
                 args)]
     `((log-attempt->* (logging-error-m ~logger)) ~forms))))

(deftest can-change-default-logger
  (set-default-logger (standard-output-error-logger))
  (is (instance? StandardOutputErrorLogger *default-logger*))
  (set-default-logger (vector-error-logger))
  (is (instance? VectorErrorLogger *default-logger*)))

(deftest logs-errors-to-the-vector-logger
  (let [logger (vector-error-logger)
        result (domonad (logging-error-m logger) [a 1
                                                  b (fail "B Failed")
                                                  c 3]
                        (+ a b c))]
    (is (failed? result))
    (is (= 1 (count (history logger))))
    (is (= result (-> logger history first)))))

(deftest doesnt-log-on-success
  (let [logger (vector-error-logger)
        result (domonad (logging-error-m logger) [a 1
                                                  b 2
                                                  c 3]
                        (+ a b c))]
    (is (not (failed? result)))
    (is (= 0 (count (history logger))))
    (is (= result 6))))

(deftest log-attempt->-macro-works
  (let [logger (vector-error-logger)
        result (log-attempt-> logger
                              1
                              (+ 2)
                              (+ 3))
        result-failed (log-attempt-> logger
                                     1
                                     (fail "Failure")
                                     (+ 3))]
    (is (not (failed? result)))
    (is (= 0 (count (history logger))))
    (is (= result 6))

    (is (failed? result-failed))
    (is (= 1 (count (history logger))))))

(comment
  (do
    (defmacro bwah [& args]
      `(println ~args))
    (macroexpand (bwah 1 2 3 4)))

  (do
    (let [logger (vector-error-logger)]
      ((log-attempt->* logger) 1 (+ 2) (- 3) (* 2))))

  (do
    (let [a [1 2 3]]
      (nthrest a 1)))

  (macroexpand-1 (log-attempt-> (vector-error-logger)
                                1
                                (fail "bwah")
                                (+ 2)
                                (+ 3)))

  (run-tests)

  (set-default-logger (->StandardOutputErrorLogger))

  (set-default-logger (->FancyStandardOutputErrorLogger))

  (do
    (def logger (vector-error-logger))
    (domonad (logging-error-m logger) [a "One"
                                       b (fail "I'm a failure")
                                       c "Three"]
             (str a ", " b ", " c))
    (domonad (logging-error-m logger) [a "One"
                                       b (fail "I'm *another* failure")
                                       c "Three"]
             (str a ", " b ", " c))
    (history logger))

  (domonad (logging-error-m) [a "One"
                              b (fail "I'm a failure")
                              c "Three"]
           (str a ", " b ", " c)))
