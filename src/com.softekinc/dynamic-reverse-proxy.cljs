(ns com.softekinc.dynamic-reverse-proxy
  (:require [cljs.nodejs :as nodejs]
            [clojure.string :refer [blank?]]))

(def http-proxy (nodejs/require "http-proxy"))
(def events (nodejs/require "events"))

(defn route-for-uri [routes uri]
  (letfn [(route-matches [route]
            (let [pfx (.-prefix route)]
              (when (== 0 (.indexOf uri pfx))
                route)))]
    (some route-matches routes)))

(defn- set-prototype!
  ([f prototype]
    (aset f "prototype" prototype))
  ([f prototype-key value]
    (-> (aget f "prototype")
        (aset prototype-key value))))

(defn normalize-prefix [p]
  (as-> p prefix
        (.replace prefix #"/+$" "")     ; trim trailing / slashes
        (if (= "" prefix) p prefix) ; undo if string is empty
        (if (.test #"^/" prefix) prefix (str "/" prefix))
        (.toLowerCase prefix)))

(defn parse-json-or-exception
  "returns {:parsed (.parse js/JSON json)} or {:exception ex}"
  [json]
  (try
    {:parsed (.parse js/JSON json)}
    (catch :default ex
      {:exception ex})))

(defn complete-route? [route]
  (and route
    (not (blank? (.-prefix route)))
    (integer? (.-port route))
    (not= 0 (.-port route))))

(defn get-routes [dproxy]
  (let [routes-atom (-> dproxy (aget "routes-atom"))]
    (-> @routes-atom vals)))

(defn add-routes! [dproxy rs]
  (let [routes-atom (-> dproxy (aget "routes-atom"))
        new-routes-kvs (mapcat #(vector (aget % "prefix") %) rs)]
    (swap! routes-atom #(apply assoc % new-routes-kvs))))

(defn end-response
  ([res status-code]
    (end-response res status-code nil))
  ([res status-code data]
    (when (.-writable res) 
      (aset res "statusCode" status-code)
      (when data
        (.write res (.stringify js/JSON data)))
      (.end res))))

(defn register
  ([dproxy req res]
    (letfn [
      (reg-status! [{:keys [code error-msg data]}]
        (when error-msg
          (.emit dproxy "registerError" (js/Error. error-msg) req res))
        (end-response res code data)
        (or error-msg data))
      (success-result [route] #js {
        :message "Registered."
        :host (.-host route)
        :port (.-port route)
        :prefix (.-prefix route)})]
      (let [remote-address (-> req .-connection .-remoteAddress)
            method (-> req .-method .toUpperCase)]
        (or (when (not= "127.0.0.1" remote-address)
              (reg-status! {:code 403, :error-msg "FORBIDDEN"}))
            (when (not= "POST" method)
              (reg-status! {:code 405, :error-msg "METHOD_NOT_ALLOWED"}))
            (let [{route :parsed, ex :exception}
                    (parse-json-or-exception (.-body req))]
              (if ex
                (reg-status! {:code 400, :error-msg "BAD_REQUEST"})
                (if-not (complete-route? route)
                  (reg-status! {:code 400, :error-msg "INCOMPLETE_REQUEST"})
                  (do (.registerRoute dproxy route)
                      (reg-status! {:code 200,
                                    :data (success-result route)}))))))))))

(defn proxy-error [dproxy error req res {:keys [host code]}]
  (let [err (if (string? error) (js/Error. error) error)]
    (.emit dproxy "proxyError" err host req res)
    (end-response res code)))

(defn on-proxy-error [dproxy error req res]
  (proxy-error dproxy error req res {:code 500, :host (.-host req)}))

(defn proxy-request [dproxy req res] 
  (let [uri (-> req .-url js/decodeURI .toLowerCase)
        routes (get-routes dproxy)
        route (route-for-uri routes uri)]
    (if-not route
      (proxy-error dproxy "NOT_FOUND" req res {:code 501})
      (do
        (aset req "host" (.create js/Object route))
        (let [target (str "http://" (.-host route) ":" (.-port route))]
          (.setHeader res "x-served-by", (str target (.-prefix route)));
          (-> dproxy
             .-proxy
             (.web req, res, #js { :target target })))))))

(defn longest-string-first [a b]
  (let [al (.-length a)
        bl (.-length b)]
    (if (== al bl)
     (.localeCompare b a)
     (> al bl))))

(defn register-route-request [dproxy req res] 
  (.setEncoding req "utf-8")
  (let [body (atom [])]
   (.on req "data" (fn [data] (swap! body conj data)))
   (.on req "end"
     (fn []
      (aset req "body" (apply str @body))
      (register dproxy req res)))))

;; js interop

(defn DynamicProxy []
  (let [routes (atom (apply sorted-map-by longest-string-first []))
        proxy (.createProxyServer http-proxy #js {"xfwd" true})]
    (this-as dproxy
      (.on proxy "error" (partial on-proxy-error dproxy))
      (doto dproxy
        (aset "routes-atom" routes)
        (aset "proxy" proxy))
      dproxy)))

(set-prototype! DynamicProxy
  (.create js/Object (-> events.EventEmitter .-prototype)))

(set-prototype! DynamicProxy "addRoutes"
  (fn addRoutes [routes]
    (this-as dproxy
      (add-routes! dproxy routes)
      dproxy)))

(set-prototype! DynamicProxy "getRoutes"
  (fn getRoutes []
    (this-as dproxy
      (clj->js (get-routes dproxy)))))

(set-prototype! DynamicProxy "registerRouteRequest"
  (fn registerRouteRequest [req res]
    (this-as dproxy
      (register-route-request dproxy req res))))

(set-prototype! DynamicProxy "proxyRequest"
  (fn registerRouteRequest [req res]
    (this-as dproxy
      (proxy-request dproxy req res))))

(set-prototype! DynamicProxy "registerRoute"
  (fn registerRoute [route]
    (this-as dproxy
      (aset route "prefix" (-> (aget route "prefix") normalize-prefix))
      (aset route "host" "localhost")
      (add-routes! dproxy [route])
      (.emit dproxy "routeRegistered" route))))

(defn -main []
  (set! module.exports DynamicProxy))

(set! *main-cli-fn* -main)
