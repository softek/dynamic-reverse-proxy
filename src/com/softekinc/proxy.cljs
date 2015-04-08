(ns com.softekinc.proxy
  (:require [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def http-proxy (nodejs/require "http-proxy"))

(defn create-proxy [opts]
  (let [options (clj->js (or opts {}))]
    (.createProxyServer http-proxy options)))
