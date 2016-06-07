(ns failjure-logger.core-test
  (:require [clojure.test :refer :all]
            [failjure-logger.core :refer :all]
            [failjure.core :as f]))

(deftest can-change-default-logger
  (let [stdout-logger (standard-output-error-logger)
        vector-logger (vector-error-logger)]
    (set-logger! stdout-logger)
    (is (= stdout-logger *logger*))
    (set-logger! vector-logger)
    (is (= vector-logger *logger*))))

;;
;; attempt->
;;
(deftest attempt->-works-on-success
  (let [logger (vector-error-logger)
        result (with-logger logger
                 (attempt-> 1 (+ 2) (+ 3)))]
    (is (not (f/failed? result)))
    (is (= 0 (count (history logger))))
    (is (= result 6))))

(deftest attempt->-works-on-failure
  (let [logger (vector-error-logger)
        fail-fn #(f/fail "Failed %s" %)
        result (with-logger logger
                 (attempt-> 1 fail-fn (+ 3)))]
    (is (f/failed? result))
    (is (= 1 (count (history logger))))))

(deftest attempt->-works-on-failure-with-default-logger
  (let [logger (vector-error-logger)
        _ (set-logger! logger)
        fail-fn #(f/fail "Failed %s" %)
        result (attempt-> 1 fail-fn (+ 3))]
    (is (f/failed? result))
    (is (= 1 (count (history logger))))))

(deftest attempt->-works-on-success-with-default-logger
  (let [logger (vector-error-logger)
        _ (set-logger! logger)
        result (attempt-> 1 (+ 2) (+ 3))]
    (is (not (f/failed? result)))
    (is (= 0 (count (history logger))))
    (is (= 6 result))))

;;
;; attempt->>
;;
(deftest attempt->>-works-on-success
  (let [logger (vector-error-logger)
        result (with-logger logger
                 (attempt->> 1 (+ 2) (+ 3)))]
    (is (not (f/failed? result)))
    (is (= 0 (count (history logger))))
    (is (= result 6))))

(deftest attempt->>-works-on-failure
  (let [logger (vector-error-logger)
        fail-fn #(f/fail "Failed %s" %)
        result (with-logger logger
                 (attempt->> 1 fail-fn (+ 3)))]
    (is (f/failed? result))
    (is (= 1 (count (history logger))))))

(deftest attempt->>-works-on-failure-with-default-logger
  (let [logger (vector-error-logger)
        _ (set-logger! logger)
        fail-fn #(f/fail "Failed %s" %)
        result (attempt->> 1 fail-fn (+ 3))]
    (is (f/failed? result))
    (is (= 1 (count (history logger))))))

(deftest attempt->>-works-on-success-with-default-logger
  (let [logger (vector-error-logger)
        _ (set-logger! logger)
        result (attempt->> 1 (+ 2) (+ 3))]
    (is (not (f/failed? result)))
    (is (= 0 (count (history logger))))
    (is (= 6 result))))

;;
;; attempt-all
;;
(deftest attempt-all-attempt-all-works-on-success
  (let [logger (vector-error-logger)
        result (with-logger logger
                 (attempt-all [a 1
                               b (+ a 2)
                               c (+ b 3)]
                              c))]
    (is (not (f/failed? result)))
    (is (= 0 (count (history logger))))
    (is (= result 6))))

(deftest attempt-all-works-on-failure
  (let [logger (vector-error-logger)
        fail-fn #(f/fail "Failed %s" %)
        result (with-logger logger
                 (attempt-all [a 1
                               b (fail-fn a)
                               c (+ b 3)]
                              c))]
    (is (f/failed? result))
    (is (= 1 (count (history logger))))))

(deftest attempt-all-works-on-success-with-default-logger
  (let [logger (vector-error-logger)
        _ (set-logger! logger)
        result (attempt-all [a 1
                             b (+ a 2)
                             c (+ b 3)]
                            c)]
    (is (not (f/failed? result)))
    (is (= 0 (count (history logger))))
    (is (= result 6))))

(deftest attempt-all-works-on-failure-with-default-logger
  (let [logger (vector-error-logger)
        _ (set-logger! logger)
        fail-fn #(f/fail "Failed %s" %)
        result (attempt-all [a 1
                             b (fail-fn a)
                             c (+ b 3)]
                            c)]
    (is (f/failed? result))
    (is (= 1 (count (history logger))))))

(deftest attempt-all-works-on-success-with-if-failed-function
  (let [logger (vector-error-logger)
        counter (atom 0)
        _ (set-logger! logger)
        result (attempt-all [a 1
                             b (+ a 2)
                             c (+ b 3)]
                            c
                            (f/if-failed [e]
                                         (swap! counter inc)
                                         e))]
    (is (not (f/failed? result)))
    (is (= result 6))
    (is (= 0 (count (history logger))))
    (is (= 0 @counter))))

(deftest attempt-all-works-on-failure-with-if-failed-function
  (let [logger (vector-error-logger)
        counter (atom 0)
        fail-fn #(f/fail "Failed %s" %)
        _ (set-logger! logger)
        result (attempt-all [a 1
                             b (fail-fn a)
                             c (+ b 3)]
                            c
                            (f/if-failed [e]
                                         (swap! counter inc)
                                         e))]
    (is (f/failed? result))
    (is (= 1 (count (history logger))))
    (is (= 1 @counter))))

;;
;; try*
;;
(deftest try*-attempt-all-works-on-success
  (let [logger (vector-error-logger)
        result (with-logger logger
                 (try*
                  6))]
    (is (not (f/failed? result)))
    (is (= 0 (count (history logger))))
    (is (= result 6))))

(deftest attempt-all-works-on-failure
  (let [logger (vector-error-logger)
        result (with-logger logger
                 (try*
                  (throw (new RuntimeException "My Exception"))))]
    (is (f/failed? result))
    (is (= 1 (count (history logger))))))

(deftest attempt-all-works-on-success-with-default-logger
  (let [logger (vector-error-logger)
        _ (set-logger! logger)
        result (try*
                6)]
    (is (not (f/failed? result)))
    (is (= 0 (count (history logger))))
    (is (= result 6))))

(deftest attempt-all-works-on-failure-with-default-logger
  (let [logger (vector-error-logger)
        _ (set-logger! logger)
        result (try*
                (throw (new RuntimeException "My Exception")))]
    (is (f/failed? result))
    (is (= 1 (count (history logger))))))
