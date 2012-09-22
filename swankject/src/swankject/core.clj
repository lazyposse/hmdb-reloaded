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
(ns  swankject.core
  [:use
   [clojure
    [repl                :only [doc find-doc            ]]
    [pprint              :only [pp pprint               ]]]
   [clojure.tools.trace  :only [trace deftrace trace-ns ]]
   [clojure.java.javadoc :only [javadoc                 ]]]
  [:require
   [clojure
    [data                :as d   ]
    [inspector           :as ins ]
    [string              :as s   ]
    [test                :as t   ]
    [walk                :as w   ]
    [xml                 :as xml ]
    [zip                 :as z   ]]
   [clojure.data.xml     :as x   ]
   [clojure.java.shell   :as sh  ]
   [clojure.java.io      :as io  ]
   [swankject.capture    :as cap ]]
  [:import
   [com.thoughtworks.xstream XStream]
   [java.io PushbackReader]
   [swankject SwankjectAspect Callback CallbackImpl]
   #_[sample Graph]]
  #_[:import
   [sample    Main]
   [sample.a  A]
   [sample.b  B]])

;;-----------------------------------------------------------------------------
;; Checking that we can manipulate the app from the repl
;;-----------------------------------------------------------------------------

;; removing the callback
(SwankjectAspect/setCallback nil)

;; and runing the prog => no sign of interception should be seen
#_(Main/main nil)

;; now put the callback again
(SwankjectAspect/setCallback (CallbackImpl.))

;; and runing the prog => now you should see something
#_(Main/main nil)

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
#_(Main/main nil)

;;-----------------------------------------------------------------------------
;; plug the capture
;;-----------------------------------------------------------------------------

(def capture (atom (cap/ini)))

(def capture-callback
  (proxy [Callback] []
    (before         [t clazz method args     ] (swap! capture cap/bef t clazz method args) )
    (afterReturning [t clazz method ret      ] (swap! capture cap/aft t clazz method ret))
    (afterThrowing  [t clazz method throwable] (swap! capture cap/thr t clazz method throwable))))

;; set it
(SwankjectAspect/setCallback capture-callback)

;;-----------------------------------------------------------------------------
;; worker functions to periodiaclly write the content of the atom to disk
;;-----------------------------------------------------------------------------

(defn write-clj-to-disk!
  "Write the content of capture to disk, in clj format"
  [] (spit "/home/denis/t.clj"
           (with-out-str (pprint (z/root @capture)))))

(defn to-disk!
  "Persist the given object to disk"
  [obj] (with-open [w (io/writer "/home/denis/p.clj")]
          (binding [*out*       w
                    *print-dup* true]
            (pprint obj))))

(defn from-disk!
  "Read an object from the disk"
  [filename]
  (with-open [r (PushbackReader. (io/reader filename))]
    (read r)))

(defn from-disk!-
  "Read an object from the disk"
  []
  (from-disk!  "/home/denis/p.clj"))

(defn read-clj
  "Read a clj file from disk to memory"
  [] (read (io/input-stream "/home/denis/t.clj")))

(defn write-xml-to-disk!
  "Write the content of capture to disk, in xml format"
  [] (x/indent (z/root @capture)
               (io/writer "/home/denis/t.xml")))

(defn exec "Takes one or more message/function, and return a fn that will print the message and exec the fn with timings"
  [& msg-fn]
  (fn []
    (doseq [[m f] (partition 2 msg-fn)]
      (println m)
      (time (f)))))

(defn worker
  "Takes a fn and a delay and indefinitly run the fn then wait the given delay"
  [f delay-ms]
  (while true
    (f)
    (Thread/sleep delay-ms)))

(defn write-to-disk-forever!
  "Return a future that execute forever the write clj and xml to disk"
  []
  (future (worker (exec "  Writing clj to disk ..." write-clj-to-disk!
                        "  Writing xml to disk ..." write-xml-to-disk!)
                  1000)))

(comment
  ;; run it
  (Main/main nil)
  ;; display the content of the atom:
  (pprint @capture)
  ;; display as XML:
  (println (xml/emit (z/root @capture)))

  (swankject.SwankjectAspect/start)

  (def f (write-xml-to-disk-forever!))

  ;; write atom to file (fast, ugly (all on one line))
  (x/emit (z/root @capture)
          (io/writer "/home/wenis/t.xml"))

  ;; write atom to file (slow, pretty)
  (x/indent (z/root @capture)
            (io/writer "/home/wenis/t.xml")))

