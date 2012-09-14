;; Contains logic to capture the calls to the Callback in an atom
(ns  swankject.capture
  (:use
   [clojure
    [repl                :only [doc find-doc            ]]
    [pprint              :only [pp pprint               ]]]
   [clojure.tools.trace  :only [trace deftrace trace-ns ]]
   [clojure.java.javadoc :only [javadoc                 ]])
  (:require
   [clojure
    [data            :as d   ]
    [inspector       :as ins ]
    [string          :as s   ]
    [test            :as t   ]
    [walk            :as w   ]
    [xml             :as xml ]
    [zip             :as z   ]]
   [clojure.data.xml :as x   ]
   [clojure.java
    [shell           :as sh  ]
    [io              :as io  ]])
  (:import
   [com.thoughtworks.xstream XStream]))

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
  [m] (do (trace m)
          (w/postwalk #(if (trace :test (map? %))
                         (into (sorted-map) %)
                         %)
                      m)))

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

;;-----------------------------------------------------------------------------
;; Implements a callback to record the method calls
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

(declare ini bef aft thr)

(t/deftest itest-after-before
  (t/is (.equals (z/root
                  (reduce (fn [r [fun meth arg]] (fun r nil "" meth arg))
                          (ini)
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
                 {:tag :capture,
                  :attrs {},
                  :content
                  [{:tag :.m,
                    :attrs {:ret "<-m", :args "m->"},
                    :content
                    [{:tag :.a,
                      :attrs {:ret "<-a", :args "a->"},
                      :content
                      [{:tag :.s, :attrs {:ret "<-s", :args "s->"}, :content ()}]}
                     {:tag :.a,
                      :attrs {:throw "!a", :args "a2->"},
                      :content
                      [{:tag :.s, :attrs {:throw "!s", :args "s2->"}, :content ()}]}
                     {:tag :.b, :attrs {:ret "<-b", :args "b->"}, :content ()}]}]})))

;;-----------------------------------------------------------------------------
;; public functions
;;-----------------------------------------------------------------------------

(defn ini
  "Return the initial state of a capture"
  [] (z/xml-zip (x/element :capture)))

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
      (z/edit assoc-in [:attrs :throw] (str throwable))
      z/up))



