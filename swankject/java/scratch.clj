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
    [inspector           :as ins]]
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
  (def design [["before M" {:curr [:main  ]  :stack {:main nil}}]
               ["before A" {:curr [:main.a]  :stack {:main {:calls [{:a nil}]}}}]
               ["after  A" {:curr [:main  ]  :stack {:main {:calls [{:a {:ret nil}}]}}}]
               ["before B" {:curr [:main.b]  :stack {:main {:calls [{:a {:ret nil}}
                                                                    {:b nil}]}}}]
               ["after  B" {:curr [:main  ]  :stack {:main {:calls [{:a {:ret nil}}
                                                                    {:b {:ret nil}}]}}}]
               ["after  M" {:curr nil        :stack {:main {:calls [{:a {:ret nil}}
                                                                    {:b {:ret nil}}]
                                                            :ret    nil}}}]]))


(def stack (atom {}))

(defn before [t class method args]
  (println (str "=>before " class "." method)))

(def p (proxy [Callback] []
         (before [t class method args]
           (before t class method args))
         (afterReturning [t class method ret]
           (println "<=afterReturning " class "." method))
         (afterThrowing [t class method throwable]
           (println "<=afterThrowing " class "." method))))

;; set it

(SwankjectAspect/setCallback p)

;; see it in action
(Main/main nil)
