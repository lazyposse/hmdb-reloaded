(ns test-lily.core
  (:use [midje.sweet])
  (:import org.lilyproject.client.LilyClient))

;; In the vm
;; start the lily server
;; ~/start-lily.sh

;; Init the connection to the lily server
(def lily-client (LilyClient. "localhost:2181" 20000))

(comment (def repository (.. lily-client getRepository))
         (def type-manager (.. lily-client getTypeManager)))

;; This is not yet working.

