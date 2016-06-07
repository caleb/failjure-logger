(ns failjure-logger.core
  (:require [failjure.core :as f]))

(defprotocol IErrorLogger
  (log [self error opts] "Logs the passed in error"))
(defprotocol IVectorErrorLogger
  (history [self] "Returns the vector containing logged entries"))

(defrecord StandardOutputErrorLogger []
  IErrorLogger
  (log [self error opts]
    (println (f/message error))))
(defn standard-output-error-logger []
  (->StandardOutputErrorLogger))

(defrecord VectorErrorLogger [history]
  IErrorLogger
  (log [_self error opts] (swap! history conj error))
  IVectorErrorLogger
  (history [_self] @history))

(defn vector-error-logger []
  (map->VectorErrorLogger {:history (atom [])}))

(def ^:dynamic *logger* (->StandardOutputErrorLogger))
(defn set-logger! [logger]
  (alter-var-root (var *logger*) (constantly logger)))

(defn- fline [and-form]
  (:line (meta and-form)))

(defn- fcolumn [and-form]
  (:column (meta and-form)))

(defmacro with-logger [logger & exprs]
  `(binding [*logger* ~logger]
     ~@exprs))

(defmacro try* [& body]
  `(try
     ~@body
     (catch Exception e#
       (log *logger* e# {:line ~(fline &form) :column ~(fcolumn &form) :file *file* :ns-str (str *ns*)})
       e#)))

(defmacro attempt-all
  ([bindings return else]
   `(let [else# (f/if-failed [e#]
                             (log *logger* e# {:line ~(fline &form) :column ~(fcolumn &form) :file *file* :ns-str (str *ns*)})
                             (f/else* ~else e#))]
      (f/attempt-all ~bindings ~return else#)))
  ([bindings return]
   `(let [else# (f/if-failed [e#]
                             (log *logger* e# {:line ~(fline &form) :column ~(fcolumn &form) :file *file* :ns-str (str *ns*)})
                             e#)]
      (f/attempt-all ~bindings ~return else#))))

(defmacro attempt->> [& forms]
  `(let [result# (f/attempt->> ~@forms)]
     (when (f/failed? result#) (log *logger* result# {:line ~(fline &form) :column ~(fcolumn &form) :file *file* :ns-str (str *ns*)}))
     result#))

(defmacro attempt-> [& forms]
  `(let [result# (f/attempt-> ~@forms)]
     (when (f/failed? result#) (log *logger* result# {:line ~(fline &form) :column ~(fcolumn &form) :file *file* :ns-str (str *ns*)}))
     result#))
