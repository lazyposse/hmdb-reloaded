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
   [clojure
    [repl                :as r]
    [pprint              :as pp]
    [inspector           :as ins]
    [xml                 :as x]
    [zip                 :as z]]
   [clojure.java.javadoc :as jd]]
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

;; this datastructure shows the evolution of the atom during the calls
;; of the callback

(comment
  (def design [["initial state"] {:curr [        ]}
               ["before M"       {:curr [:main   ] :stack {:tag "Main.main"}}]
               ["before A"       {:curr [:main :a] :stack {:tag "Main.main" :content [{:tag "A.a"}]}}]
               ["after  A"       {:curr [:main   ] :stack {:tag "Main.main" :content [{:tag "A.a" :attrs {:ret nil}}]}}]
               ["before B"       {:curr [:main :b] :stack {:tag "Main.main" :content [{:tag "A.a" :attrs {:ret nil}}
                                                                                      {:tag "B.b"}]}}]
               ["after  B"       {:curr [:main   ] :stack {:tag "Main.main" :content [{:tag "A.a" :attrs {:ret nil}}
                                                                                      {:tag "B.b" :attrs {:ret nil}}]}}]
               ["after  M"       {:curr nil        :stack {:tag "Main.main" :content [{:tag "A.a" :attrs {:ret nil}}
                                                                                      {:tag "B.b" :attrs {:ret nil}}]
                                                           :attrs {:ret nil}}}]]))


(def stack (atom {}))

(defn before [t clazz method args]
  (println (str "=>before " clazz "." method)))

(defn bef [cap t clazz method args]
  (if (z/down cap)
    (z/rightmost
     (z/insert-right (z/rightmost (z/down cap))
                     {:tag   (str clazz "." method)
                      :attrs {:args args}}))
    (z/down
     (z/insert-child cap
                     {:tag   (str clazz "." method)
                      :attrs {:args args}}))))

(defn aft [cap t clazz method ret      ]
  (z/up
   (z/edit cap assoc-in [:attrs :ret] ret)))


(defn pz [loc]
  (println "current node:")
  (pp/pprint (z/node loc))
  (println "whole: ")
  (pp/pprint (z/root loc)))

(comment
  (def r (z/down (z/xml-zip {:tag "capture" :content [{:tag "mefirst"}]})))
  (def r1 (aft r nil "mefirst" "" "first-ret"))
  (def r2 (bef r1 nil "Main" "main" "main-args"))
  (def r3 (aft r2 nil "main" "main" "main-ret"))
  (def r4 (bef r3 nil "m2"   ""     "arg4"))
  (def r5 (aft r4 nil "m2"   ""     "ret2"))
  (def r6 (bef r5 nil "m3"   ""     "arg3"))
  (def r6a (bef r6 nil "m3a"   ""     "arg3a"))
  (def r6b (aft r6a nil "m3a"   ""     "ret2"))
  (def r7 (aft r6b nil "m3"   ""     "ret3")))



(def p
  (proxy [Callback] []
    (before         [t clazz method args     ] (before t clazz method args))
    (afterReturning [t clazz method ret      ] (println (str "<=afterReturning " clazz "." method)))
    (afterThrowing  [t clazz method throwable] (println (str "<=afterThrowing " clazz "." method)))))

;; set it

(SwankjectAspect/setCallback p)

;; see it in action
(Main/main nil)

