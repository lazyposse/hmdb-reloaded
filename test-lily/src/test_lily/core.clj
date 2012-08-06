(ns test-lily.core
  (:use [midje.sweet])
  (:import [org.lilyproject.client         LilyClient]
           [org.lilyproject.repository.api Scope QName]
           [org.lilyproject.util.repo      PrintUtil]))

;; ############### Static setup

(def conn {:local "localhost:2181"
           :vm    "192.168.33.10:2181"})

;; In the vm
;; start the lily server
;; ~/start-lily.sh

;; ############### Setup

;; Init the connection to the lily server
(def lily-client (LilyClient. (:local conn) 20000))
(def repository (.. lily-client getRepository))
(def type-manager (.. repository getTypeManager))

;; ############### Create some field types

;; we will work in the namespace 'namespace-test'

(def string-value-type (.. type-manager (getValueType "STRING")))

;; create some field type name for the data we persist
(def qname-title (QName. "namespace-test" "title"))

;; create the field type bean (no persisting yet)
(def field-type-title (.. type-manager (newFieldType string-value-type qname-title Scope/VERSIONED)))

;; persist the bean field-type-title
(.. type-manager (createFieldType field-type-title) )

;; list the field types persisted
(.. type-manager getFieldTypes)

;; oops
;; i forgot to keep the reference the first time
;; so i update it but this time i kept the reference!
( def title (.. type-manager (createOrUpdateFieldType field-type-title)))

;; ############### Now create some record types

;; create some name for the record type
(def qname-book (QName. "namespace-test" "book"))

;; no persistence, just a bean
(def record-type-book (.. type-manager (newRecordType qname-book )))

;; Add the field type we previously created (the true parameter stands for 'mandatory?')
(.. record-type-book (addFieldTypeEntry (. title getId) true)) ;; (beurk, ugly mutating code)

;; Now we will persist the record type book
(def book (.. type-manager (createRecordType record-type-book)))

;; Some pretty print utility
(PrintUtil/print book repository)

;; add a new field type

;; We now add the field type authors
(def qname-authors (QName. "namespace-test" "authors"))
(.. type-manager (createFieldType "LIST<STRING>" qname-authors Scope/VERSIONED))

;; oops again - forgot to keep the reference
(def authors-ft (.. type-manager (getFieldTypeByName qname-authors)))

(def book-retrieved (.. type-manager (getRecordTypeByName qname-book nil)))
;; here, book retrieve and book are the same

(. book-retrieved (addFieldTypeEntry (. authors-ft getId) false));; warning again, mutating code here

(def book-retrieved (.. type-manager (updateRecordType book-retrieved)))

;; ############### Now Create records

;; first create some bean
(def first-record (.. repository newRecord))

;; now set the record type
;; then set the title (which is a string)
;; then the field authors (which is a list of strings)
(doto first-record
  (.setRecordType qname-book)
  (.setField qname-title "Clojure, the first language which gets it right!")
  (.setField qname-authors ["Denis Labaye" "Antoine R. Dumont"]));; (beware, mutation here)

;; now persist the record into the storage
(def persisted-fr (.. repository (create first-record)))

(def persisted-fr-id (.getId persisted-fr))

;; some tool to pretty print
(PrintUtil/print persisted-fr repository)

;; ############### Update record

(doto persisted-fr
  (.setField qname-title "Clojure, the first language that gets it right!!!")
  (.delete qname-authors true ))

(def persisted-fr2 (.. repository (update persisted-fr)))

;; ############### Read records

;;(.. repository (read persisted-fr-id))


;; ############### Delete records



