(ns dre.pledge.interface
  (:require [dre.pledge.core :as core]))

(defn export-depots [cluster]
  (core/export-depots cluster))

(defn export-pstates [cluster]
  (core/export-pstates cluster))

(defn get-pledge-module []
  (core/get-pledge-module))


