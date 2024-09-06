(ns dre.pledge.core
  (:require
   [com.rpl.rama :as r]
   [com.rpl.rama.path :as path]
   [dre.session.interface :as session]
   [dre.belt.interface :as belt])
  (:import
   (java.util UUID)))

(defrecord Pledge [id title trigger-count])
;; depots
(def *pledge-depot "*pledge-depot")
;; pstates
(def $$pledges "$$pledges")

#_:clj-kondo/ignore
(r/defmodule PledgeModule [setup topo]
  (r/declare-depot setup *pledge-depot (r/hash-by :id))
  (let [s (r/stream-topology topo "pledge")]
    ;; Quizzes schema
    (r/declare-pstate s $$pledges {String (r/fixed-keys-schema {:title String
                                                                :trigger-count Long})})
    
    (r/<<sources s
                 ;; Question
                 (r/source> *pledge-depot :> {:keys [*id] :as *pledge})
                 (r/|hash *id)
                 (r/local-transform> [(path/keypath *id)
                                      (path/termval (into {} (dissoc *pledge :id)))]
                                     $$pledges))))

(def pledge-module-name (r/get-module-name PledgeModule))

(def depots [*pledge-depot])
(def pstates [$$pledges])

(defn export-depots [cluster]
  (belt/make-depots-map cluster pledge-module-name depots))

(defn export-pstates [cluster]
  (belt/make-pstates-map cluster pledge-module-name pstates))

(defn get-pledge-module [] PledgeModule)
