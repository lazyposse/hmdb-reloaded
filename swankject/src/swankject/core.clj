;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; This file contains clojure code to be evaluated when connected to the
;; sample app.
;;
;;
;; * run the sample with the agent: `make run-load-time-weaving`
;; * then connect to the swank server with emacs `M-x slime-connect`
;; * compile (C-c C-k) this file
;; * and play with it
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(ns swankject.core
  [:use
   [clojure
    [repl               :only [doc find-doc]]
    [pprint             :only [pp pprint   ]]]
   [clojure.tools.trace :only [trace deftrace trace-ns]]]
  [:require
   [clojure
    [data                :as d]
    [inspector           :as ins]
    [string              :as s]
    [test                :as t]
    [walk                :as w]
    [xml                 :as x]
    [zip                 :as z]]
   [clojure.java.javadoc :as jd]]
  [:import
   [swankject SwankjectAspect Callback CallbackImpl]
   [sample    Main]
   [sample.a  A]
   [sample.b  B]])

;;-----------------------------------------------------------------------------
;; Checking that we can manipulate the app from the repl
;;-----------------------------------------------------------------------------

;; removing the callback
(SwankjectAspect/setCallback nil)

;; and runing the prog => no sign of interception should be seen
(Main/main nil)

;; now put the callback again
(SwankjectAspect/setCallback (CallbackImpl.))

;; and runing the prog => now you should see something
(Main/main nil)

;;-----------------------------------------------------------------------------
;; Implements a simple callback in clojure
;;-----------------------------------------------------------------------------

(def simple-log-callback
  (proxy [Callback] []
    (before         [t clazz method args     ] (println (str "before====>      " clazz "." method)))
    (afterReturning [t clazz method ret      ] (println (str "<=afterReturning " clazz "." method)))
    (afterThrowing  [t clazz method throwable] (println (str "<=afterThrowing  " clazz "." method)))))

;; set it
(SwankjectAspect/setCallback simple-log-callback)

;; see it in action
(Main/main nil)

;;-----------------------------------------------------------------------------
;; Implements a callback to records the method calls
;;-----------------------------------------------------------------------------

(defn- get-fn-name
  "For dev: hack to get the name of a fn"
  [f] (second (s/split (clojure.main/demunge (str f))
                       #"[/@]")))

(defn- sorted-map-all
  "For dev: Recursively transforms nested maps into sorted nested maps.
Usefull for comparing two nested datastructures"
  [m] (w/postwalk #(if (map? %)
                     (into (sorted-map) %)
                     %)
                  m))

(defn- loggify
  "For dev: log decorator"
  [f] (fn [& args]
        (println (str "(" (get-fn-name f) (z/node (first args)) ", method=" (nth args 3) "...)"))
        (let [r (apply f args)]
          (println (str "    =>    " (with-out-str (pprint (z/root (first args))))))
          (println)
          r)))

(defn bef
  "Return the new value for the capture, after 'before' has been called.
Must start with the value: (z/xml-zip {:tag :capture})"
  [cap t clazz method args]
  (let [new-call {:tag (str clazz "." method)
                  :attrs {:args args}}]
    (if (z/children cap)
      (z/right (z/insert-right (z/rightmost (z/down cap))
                               new-call))
      (z/down  (z/insert-child cap new-call)))))

#_(def bef (loggify bef))

(defn aft
  "Return the new value for the capture, after 'after' has been called"
  [cap t clazz method ret]
  (z/up (z/edit cap assoc-in [:attrs :ret] ret)))

#_(def aft (loggify aft))

(t/deftest itest-after-before
  (t/is (= (z/root
            (reduce (fn [r [fun meth arg]] (fun r nil "" meth arg))
                    (z/xml-zip {:tag :capture})
                    [[bef "m" "m->"     ]
                     [bef   "a" "a->"   ]
                     [bef     "s" "s->" ]
                     [aft     "s" "<-s" ]
                     [aft   "a" "<-a"   ]
                     [bef   "b" "b->"   ]
                     [aft   "b" "<-b"   ]
                     [aft "m" "<-m"     ]]))
           {:tag     :capture
            :content [{:tag     ".m"
                       :attrs   {:args "m->"
                                 :ret  "<-m"}
                       :content [{:tag     ".a"
                                  :attrs   {:args "a->"
                                            :ret  "<-a"}
                                  :content [{:tag ".s"
                                             :attrs {:args "s->"
                                                     :ret  "<-s"}}]}
                                 {:tag     ".b"
                                  :attrs   {:args "b->"
                                            :ret  "<-b"}}]}]})))

(def capture (atom (z/xml-zip {:tag :capture})))

(def capture-callback
  (proxy [Callback] []
    (before         [t clazz method args     ] (swap! capture bef t clazz method args) )
    (afterReturning [t clazz method ret      ] (swap! capture aft t clazz method ret))
    (afterThrowing  [t clazz method throwable] (println (str "<=afterThrowing  (not implemented)" clazz "." method)))))

;; set it
(SwankjectAspect/setCallback capture-callback)

(comment
  ;; run it
  (Main/main nil)
  ;; display the content of the atom:
  (pprint @capture)
  ;; display as XML:
  (println (x/emit (z/root @capture))))
