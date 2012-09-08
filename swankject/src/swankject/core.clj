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
   [sample Graph]]
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

(defn- to-xml-and-back
  "Convert an object:
    - to xml using XStream
    - parse it with clojure.xml
    - emit it with clojure.xml
    - parse it back with XStream

We should have o = (to-xml-and-back o)

Which is not the case when o has Strings"
  [o] (let [xs (XStream.)
            x1 (.toXML xs o)
            o1 (xml/parse (as-input-stream x1))
            x2 (with-out-str
                 (xml/emit o1))]
        (.fromXML xs x2)))

(defn- to-xml-and-back-with-data
  "Convert an object:
    - to xml using XStream
    - parse it with clojure.data.xml
    - emit it with clojure.data.xml
    - parse it back with XStream

We should have o = (to-xml-and-back-with-data o)

It is working with clojure.data.xml"
  [o] (let [xs (XStream.)
            x1 (.toXML xs o)
            o1 (x/parse-str x1)
            x2 (str (x/emit o1 (java.io.StringWriter.)))]
        (.fromXML xs x2)))

(defn- iterate-to-xml-and-back
  "Run to-xml-and-back 10 times and return the xml representation of the result"
  [to-xml-and-back-fn]
  (let [g (Graph/newExample)
        i (iterate to-xml-and-back-fn g)
        gg (nth i 10)]
    (.toXML (XStream.) gg)))

(defn- to-xml "turns any object to xml"
  [o] (.toXML (XStream.) o))

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

(defn bef
  "Takes a datastructure and the params of a `before` AOP interception,
and return a new datastructure representing the new capture state.
The initial value of the capture must be `(z/xml-zip {:tag :capture})`."
[cap t clazz method args]
  (append-child cap
                {:tag (str clazz "." method), :attrs {:args args}}))

(defn aft
  "Same as `bef`, but for the `after` AOP interception."
  [cap t clazz method ret]
  (-> cap
      (z/edit assoc-in [:attrs :ret] ret)
      z/up))

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
  (println (xml/emit (z/root @capture))))

