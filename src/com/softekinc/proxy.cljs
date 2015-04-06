(ns com.softekinc.proxy
  (:require [cljs.nodejs :as nodejs]))

(def http-proxy (nodejs/require "http-proxy"))

(defn create-proxy [opts]
  (let [options (clj->js (or opts {}))]
    (.createProxyServer http-proxy options)))
