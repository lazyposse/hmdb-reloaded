;;
;; This file contains clojure code to be evaluated when connected to the
;; sample app.
;;
;;
;; * run the sample with the agent: `make run-load-time-weaving`
;; * then connect to the swank server with emacs `M-x slime-connect`
;; * then you can use the forms below to test it
;;
(ns swankject
  [:require
   [clojure.repl         :as r]
   [clojure.java.javadoc :as jd]
   [clojure.pprint       :as pp]]
  [:import
   [swankject SwankjectAspect Callback CallbackImpl]
   [sample    Main]
   [sample.a  A]
   [sample.b  B]])

(in-ns 'swankject)

;; removing the callback
(SwankjectAspect/setCallback nil)

;; and runing the prog => no sign of interception should be seen
(Main/main nil)

;; now put the callback again
(SwankjectAspect/setCallback (CallbackImpl.))

;; and runing the prog => now you should see something
(Main/main nil)

;; implement a callback in clojure

(def p (proxy [Callback] []
         (before [t class method args]
           (prn "before"))
         (afterReturning [t class method ret]
           (prn "afterReturning"))
         (afterThrowing [t class method throwable]
           (prn "afterThrowing"))))

;; set it

(SwankjectAspect/setCallback p)

;; see it in action
(Main/main nil)
