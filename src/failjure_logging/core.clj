(ns failjure-logging.core
  (:require [failjure.core :as f]))

(defprotocol IErrorLogger
  (log [self error] "Logs the passed in error"))
(defprotocol IVectorErrorLogger
  (history [self] "Returns the vector containing logged entries"))

(defrecord StandardOutputErrorLogger []
  IErrorLogger
  (log [self error]
    (println (f/message error))))
(defn standard-output-error-logger []
  (->StandardOutputErrorLogger))

(defrecord VectorErrorLogger [history]
  IErrorLogger
  (log [_self error] (swap! history conj error))
  IVectorErrorLogger
  (history [_self] @history))

(defn vector-error-logger []
  (map->VectorErrorLogger {:history (atom [])}))

(def ^:dynamic *logger* (->StandardOutputErrorLogger))
(defn set-logger! [logger]
  (alter-var-root (var *logger*) (constantly logger)))

(defmacro with-logger [logger & exprs]
  `(binding [*logger* ~logger]
     ~@exprs))

(defmacro try* [& body]
  `(try
     ~@body
     (catch Exception e#
       (log *logger* e#)
       e#)))

(defmacro attempt-all
  ([bindings return else]
   `(let [else# (f/if-failed [e#]
                             (log *logger* e#)
                             (f/else* ~else e#))]
      (f/attempt-all ~bindings ~return else#)))
  ([bindings return]
   `(let [else# (f/if-failed [e#]
                             (log *logger* e#)
                             e#)]
      (f/attempt-all ~bindings ~return else#))))

(defmacro attempt->> [& forms]
  `(let [result# (f/attempt->> ~@forms)]
     (when (f/failed? result#) (log *logger* result#))
     result#))

(defmacro attempt-> [& forms]
  `(let [result# (f/attempt-> ~@forms)]
     (when (f/failed? result#) (log *logger* result#))
     result#))
