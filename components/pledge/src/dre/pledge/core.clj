(ns dre.pledge.core
  (:require
   [com.rpl.rama :as r]
   [com.rpl.rama.path :as path]
   [dre.session.interface :as session]
   [dre.belt.interface :as belt])
  (:import
   (java.util UUID)))

(defrecord Pledge [id title trigger-count])
(defrecord Signature [pledge-id phone-number name])
;; depots
(def *pledge-depot "*pledge-depot")
(def *signature-depot "*signature-depot")
;; pstates
(def $$pledges "$$pledges")
(def $$pledge->signatures "$$pledge->signatures")
(def $$pledge->numbers "$$pledge->numbers")

#_:clj-kondo/ignore
(r/defmodule PledgeModule [setup topo]
  
  (r/declare-depot setup *pledge-depot (r/hash-by :id))
  (r/declare-depot setup *signature-depot (r/hash-by :pledge-id))
  
  (let [s (r/stream-topology topo "pledge")]
    ;; Quizzes schema
    (r/declare-pstate s $$pledges {String (r/fixed-keys-schema {:title String
                                                                :trigger-count Long
                                                                :id String})})
    (r/declare-pstate s $$pledge->signatures {String ;; pledge-id
                                              (r/vector-schema (r/fixed-keys-schema
                                                                {:name String}))})
    (r/declare-pstate s $$pledge->numbers {String ;; pledge-id
                                           (r/set-schema String ;; phone-numbers
                                                         )})
    
    (r/<<sources s
                 (r/source> *pledge-depot :> {:keys [*id] :as *pledge})
                 (r/|hash *id)
                 (r/local-transform> [(path/keypath *id)
                                      (path/termval (into {} *pledge))]
                                     $$pledges)
                 
                 (r/source> *signature-depot :> {:keys [*pledge-id *phone-number] :as *signature})
                 (r/|hash *pledge-id)
                 (r/local-select> [(path/keypath *pledge-id) *phone-number]
                                  $$pledge->numbers :> *existing-signature)
                 (r/<<if (nil? *existing-signature)
                         (r/local-transform> [(path/keypath *pledge-id)
                                              path/NIL->VECTOR
                                              path/AFTER-ELEM
                                              (path/termval
                                               {:name (:name *signature)})]
                                             $$pledge->signatures)
                         
                         (r/local-transform> [(path/keypath *pledge-id)
                                              path/NIL->SET
                                              path/NONE-ELEM
                                              (path/termval *phone-number)]
                                             $$pledge->numbers)))))

(def pledge-module-name (r/get-module-name PledgeModule))

(def depots [*pledge-depot *signature-depot])
(def pstates [$$pledges $$pledge->signatures $$pledge->numbers])

(defn export-depots [cluster]
  (belt/make-depots-map cluster pledge-module-name depots))

(defn export-pstates [cluster]
  (belt/make-pstates-map cluster pledge-module-name pstates))

(defn get-pledge-module [] PledgeModule)
