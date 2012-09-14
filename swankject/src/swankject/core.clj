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
    [repl                :only [doc find-doc]]
    [pprint              :only [pp pprint   ]]]
   [clojure.tools.trace  :only [trace deftrace trace-ns]]
   [clojure.java.javadoc :only [javadoc]]]
  [:require
   [clojure
    [data                :as d]
    [inspector           :as ins]
    [string              :as s]
    [test                :as t]
    [walk                :as w]
    [xml                 :as xml]
    [zip                 :as z]]
   [clojure.data.xml     :as x]
   [clojure.java.shell   :as sh]
   [clojure.java.io      :as io]]
  [:import
   [swankject SwankjectAspect Callback CallbackImpl]
   [com.thoughtworks.xstream XStream]
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
;; Some dev functions
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
        (println (str "(" (get-fn-name f)
                      (z/node (first args))
                      ", method="
                      (nth args 3)
                      "...)"))
        (let [r (apply f args)]
          (println (str "  =>  " (with-out-str (pprint (z/root (first args))))))
          (println)
          r)))

(defn- as-input-stream "Convert input to InputStream"
  [str] (java.io.ByteArrayInputStream. (.getBytes str "UTF-8")))

(defmulti to-xml "elements to XML using different implems"
  :serializer)

(defmulti parse-xml "XML to elements using different implems"
  :serializer)

(defmethod parse-xml :clojure.xml
  [{xml :xml}] (xml/parse (as-input-stream xml)))

(defmethod parse-xml :clojure.data.xml
  [{xml :xml}] (x/parse-str xml))

(defmethod to-xml :clojure.xml
  [{elements :elements}] (with-out-str (xml/emit elements)))

(defmethod to-xml :clojure.data.xml
  [{elements :elements}] (x/emit-str elements))

(defn- to-xml-and-back
  "Convert an object:
    - to xml using XStream
    - parse it with the :serializer (:clojure.xml or :clojure.data.xml) passed in the opts
    - emit it with the serializer
    - parse it back with XStream

We should have o = (to-xml-and-back o)

Which is not the case when o has Strings"
  [o & opts] (let [xs (XStream.)
                   x1 (.toXML xs o)
                   o1 (parse-xml (merge {:xml      x1} opts))
                   x2 (to-xml    (merge {:elements o1} opts))]
               (.fromXML xs x2)))

(defn- to-xml-and-back-clojure.xml
  "We should have o = (to-xml-and-back o)
Which is not the case when o has Strings"
  [o] (to-xml-and-back o :serializer :clojure.xml))

(defn- to-xml-and-back-clojure.data.xml
  "We should have o = (to-xml-and-back o)
Which is not the case even if o has Strings"
  [o] (to-xml-and-back o :serializer :clojure.data.xml))

(comment (defn- iterate-to-xml-and-back
   "Run to-xml-and-back 10 times and return the xml representation of the result"
   [to-xml-and-back-fn]
   (let [g (Graph/newExample)
         i (iterate to-xml-and-back-fn g)
         gg (nth i 10)]
     (.toXML (XStream.) gg))))

;;-----------------------------------------------------------------------------
;; Implements a callback to records the method calls
;;-----------------------------------------------------------------------------

(defn- insert-child-and-move
  "Like insert-child, but move to the child"
  [n c] (-> n
            (z/insert-child c)
            z/down))

(defn- append-to-children-and-move
  "Add the child at the right of the children list, and move to it. "
  [n c] (let [right-child (-> n
                              z/down
                              z/rightmost)]
          (-> right-child
              (z/insert-right c)
              z/right)))

(defn- append-child
  "Append a child to the right of the list of childs (or create one if not exists).
And move to it"
  [n c] (if (z/children n)
          (append-to-children-and-move n c)
          (insert-child-and-move       n c)))

(defn- method-to-tag
  "Take a couple class / method and return a string suitable for an XML tag"
  [clazz method] (let [clazz-str  (if clazz  (str clazz)  "_unknown-class_")
                       method-str (if method (str method) "_unknown-method_")]
                   (keyword (str clazz-str \. method-str))))

(defn bef
  "Takes a datastructure and the params of a `before` AOP interception,
and return a new datastructure representing the new capture state.
The initial value of the capture must be `(z/xml-zip {:tag :capture})`."
[cap t clazz method args]
(append-child cap
              (x/element (method-to-tag clazz method)
                         {:args args}) ))

(defn aft
  "Same as `bef`, but for the `after` AOP interception."
  [cap t clazz method ret]
  (-> cap
      (z/edit assoc-in [:attrs :ret] ret)
      z/up))

(defn thr
  "Same as `aft`, but for the `afterThrowing` aspectJ pointcut."
  [cap t clazz method throwable]
  (-> cap
      (z/edit assoc-in [:attrs :throw] throwable)
      z/up))

(t/deftest itest-after-before
  (t/is (= (z/root
            (reduce (fn [r [fun meth arg]] (fun r nil "" meth arg))
                    (z/xml-zip (x/element :capture))
                    [[bef "m" "m->"     ]
                     [bef   "a" "a->"   ]
                     [bef     "s" "s->" ]
                     [aft     "s" "<-s" ]
                     [aft   "a" "<-a"   ]

                     ;; with an exception throwed by s, then a, then
                     ;; catched by m
                     [bef   "a" "a2->"   ]
                     [bef     "s" "s2->" ]
                     [thr     "s" "!s"]
                     [thr   "a" "!a"   ]

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
                                 {:tag     ".a"
                                  :attrs   {:args "a2->"
                                            :throw  "!a"}
                                  :content [{:tag ".s"
                                             :attrs {:args "s2->"
                                                     :throw "!s"}}]}
                                 {:tag     ".b"
                                  :attrs   {:args "b->"
                                            :ret  "<-b"}}]}]})))

(def e (z/root
        (reduce (fn [r [fun meth arg]] (fun r nil "" meth arg))
                (z/xml-zip (x/element :capture))
                [[bef "m" "m->"     ]])))

(def capture (atom (z/xml-zip (x/element :capture))))

(def capture-callback
  (proxy [Callback] []
    (before         [t clazz method args     ] (swap! capture bef t clazz method args) )
    (afterReturning [t clazz method ret      ] (swap! capture aft t clazz method ret))
    (afterThrowing  [t clazz method throwable] (swap! capture thr t clazz method throwable))))

;; set it
(SwankjectAspect/setCallback capture-callback)

;;-----------------------------------------------------------------------------
;; worker functions to periodiaclly write the content of the atom to disk
;;-----------------------------------------------------------------------------

(defn write-clj-to-disk!
  "Write the content of capture to disk, in clj format"
  [] (spit "/home/wenis/t.clj"
           (with-out-str (pprint @capture))))

(defn write-xml-to-disk!
  "Write the content of capture to disk, in xml format"
  [] (x/indent (z/root @capture)
               (io/writer "/home/wenis/t.xml")))

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

