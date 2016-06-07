# Failjure Logger

Failjure Logger includes macros and protocols to automatically log errors that
occur in application code.

This library includes macros with the same names as those in the base
[Failjure][failjure] library.

By default Failjure Logger uses `println` to print errors to standard output.
You can configure Failjure Logger to use a different logger:

```clojure
(defrecord TimbreErrorLogger []
 IErrorLogger
 (log [self error] (timbre/error (f/message error))))

(fl/set-logger! (->TimbreErrorLogger))
```

In this example we create a new logger record and implement the `IErrorLogger`
protocol. The single `log` function receives the logger itself, and the Failjure
error (in this case, we simply log the error's message)

Then we call `set-logger!` to change the default logger. That's all that's needed.

Of course, if you want to get fancier in your Logger you can. Let's say that you
have your own custom `HasFailed` record (see [Failjure][failjure] for more
information), and you want to log at different levels depending on the kind of
error:

```clojure
(defrecord FancyError [level message data]
  f/HasFailed
  (failed? [self] true)
  (message [self] message))

(defn fancy-error [level message data]
  (map->FancyError {:level level :message message :data data}))

(defrecord TimbreErrorLogger []
 fl/IErrorLogger
 (log [self error]
  (timbre/log (or (:level error) :error)
              (f/message error))))

(fl/set-logger! (->TimbreErrorLogger))

(defn save-user [user]
  ;; Try to save the user... oh no! It failed.
  (fancy-error :fatal "Could not save user!" user))

;; This will fail and log to the TimbreErrorLogger at the :fatal level
(fl/attempt-> {:name "Ada Lovelace"}
              (save-user))
```

## Usage

Using this library is as simple as setting up your logger and replacing the
`attempt-*` and `try*` macro calls from [Failjure][failjure] with those from the
Failjure Logger library.

```clojure
(ns my-namespace.core
  (:require [failjure-logger.core :as fl]
            [failjure.core :refer [fail]]))

(fl/attempt-> {:name "Ada Lovelace"}
              (validate-user)
              (save-user))

(fl/attempt->> {:name "Ada Lovelace"}
               (validate-user)
               (save-user))

(fl/attempt-all [user {:name "Ada Lovelace"}
                 user (validate-user user)
                 user (save-user user)]
  user)

(fl/attempt-all [user {:name "Ada Lovelace"}
                 user (validate-user user)
                 user (save-user user)]
  user
  (f/if-failed [e]
    ;; Do something with the failure
    e))

(fl/try*
  (let [user (some-throwing-function {:name "Ada Lovelace"})]
    user))
```

## License

Copyright © 2016 Caleb Land

Distributed under the MIT License.

[failjure]: https://github.com/adambard/failjure
