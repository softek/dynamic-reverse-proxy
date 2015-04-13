(ns com.softekinc.dynamic-reverse-proxy
  (:require [cljs.nodejs :as nodejs]
            [com.softekinc.proxy :refer [create-proxy]]
            [clojure.string :refer [blank?]]))

(nodejs/enable-util-print!)

(def require (js* "require"))

(def events (require "events"))

(def allow-registration-from #{"::1" "127.0.0.1"})

#_(defschema Url
  {:encrypted s/Bool
   :host-name s/Str
   :path      s/Str})

(defn longest-string-first [a b]
  (let [al (.-length a)
        bl (.-length b)]
    (if (== al bl)
     (.localeCompare b a)
     (> al bl))))

(def empty-route-map
  (apply sorted-map-by longest-string-first []))

(defn url-matches-route?
  "returns true when
     (.-host route) is nil or is the same as (:host-name url))
     AND (:path url) starts with (.-prefix route)

   In both criteria, the string comparisons ignore case."
  [{:keys [path host] :as url} route]
  (let [pfx (aget route "prefix")]
    (.startsWith path pfx)))

(defn route-for-url
  "Gets the first route that matches the url map."
  [routes url]
  (->> routes
       (filter (partial url-matches-route? url))
       first))

(defn- set-prototype!
  ([f prototype]
    (aset f "prototype" prototype))
  ([f prototype-key value]
    (-> (aget f "prototype")
        (aset prototype-key value))))

(defn normalize-prefix [p]
  (as-> p prefix
        (.replace prefix #"/+$" "") ; trim trailing / slashes
        (if (= "" prefix) p prefix) ; undo if string is empty
        (if (.test #"^/" prefix) prefix (str "/" prefix))
        (.toLowerCase prefix)))

(defn normalize-path [^String path]
  (.toLowerCase path))

(defn complete-route? [route]
  (and route
    (not (blank? (aget route "prefix")))
    (integer? (aget route "port"))
    (not= 0 (aget route "port"))))

(defn get-host-routes [dproxy]
  (let [routes-atom (-> dproxy (aget "routes-atom"))]
    @routes-atom))

(defn routes-for-host-name [dproxy host-name]
  (let [routes (get-host-routes dproxy)]
    (lazy-cat (-> routes (get host-name) vals)
              (-> routes :any vals))))

(defn expand-route [route]
  (let [prefix (aget route "prefix")
        r {:route route, :req-host-name :any, :prefix prefix}
        rhns (-> route (aget "reqHostNames") js->clj)]
    (if rhns
      (map #(assoc r :req-host-name %) rhns)
      [r])))

(defn add-route [host-name->routes r]
  (let [expanded-routes (expand-route r)]
    (reduce
      (fn [routes {:keys [req-host-name prefix route]}]
        (update routes req-host-name #(assoc (or % empty-route-map) prefix route)))
      (update host-name->routes :raw conj r)  
      expanded-routes)))

(defn add-routes! [dproxy rs]
  (let [routes-atom (-> dproxy (aget "routes-atom"))]
    (swap! routes-atom #(add-route % rs))))

(defn register-route! [dproxy route]
  (aset route "prefix" (-> (aget route "prefix") normalize-prefix))
  (when-not (aget route "host")
    (aset route "host" "localhost"))
  (add-routes! dproxy route)
  (.emit dproxy "routeRegistered" route))

(defn end-response
  ([res status-code]
    (end-response res status-code nil))
  ([res status-code data]
    (when (aget res "writable") 
      (aset res "statusCode" status-code)
      (when data
        (.write res (.stringify js/JSON data)))
      (.end res))))

(defn register!
  ([dproxy req res]
    (letfn [
      (reg-status! [{:keys [code error-msg data]}]
        (when error-msg
          (.emit dproxy "registerError" (js/Error. error-msg) req res))
        (end-response res code data)
        (or error-msg data))
      (success-result [route] #js {
        :message "Registered."
        :host (aget route "host")
        :port (aget route "port")
        :prefix (aget route "prefix")})
      (parse-json-or-exception
        [json]
        (try
          {:parsed (.parse js/JSON json)}
          (catch :default ex
            {:exception ex})))]
      (let [remote-address (-> req .-connection .-remoteAddress)
            method (-> req .-method .toUpperCase)]
        (or (when-not (allow-registration-from remote-address)
              (reg-status! {:code 403, :error-msg "FORBIDDEN"}))
            (when (not= "POST" method)
              (reg-status! {:code 405, :error-msg "METHOD_NOT_ALLOWED"}))
            (let [{route :parsed, ex :exception}
                    (parse-json-or-exception (.-body req))]
              (if ex
                (reg-status! {:code 400, :error-msg "BAD_REQUEST"})
                (if-not (complete-route? route)
                  (reg-status! {:code 400, :error-msg "INCOMPLETE_REQUEST"})
                  (do (register-route! dproxy route)
                      (reg-status! {:code 200,
                                    :data (success-result route)}))))))))))

(defn proxy-error [dproxy error req res {:keys [host code]}]
  (let [err (if (string? error) (js/Error. error) error)]
    (.emit dproxy "proxyError" err host req res)
    (end-response res code)))

(defn on-proxy-error [dproxy error req res]
  (proxy-error dproxy error req res {:code 500, :host (.-host req)}))

(defn proxy-request [dproxy req res]
  (let [uri (-> req .-url js/decodeURI normalize-path)
        host (-> req .-headers .-host)
        host-name (or (re-find #"^[^:]+" (or host "")) host)
        url {:path uri
             :host host
             :host-name host-name
             :encrypted (boolean (-> req .-connection .-encrypted))}
        routes (routes-for-host-name dproxy host-name)
        route (route-for-url routes url)]
    (if-not route
      (proxy-error dproxy "NOT_FOUND" req res {:code 501})
      (do
        (aset req "host" (.create js/Object route))
        (let [h (aget route "host")
              target (str "http://" h ":" (aget route "port"))]
          (.setHeader res "x-served-by", (str target (aget route "prefix")))
          (-> dproxy
             (aget "proxy")
             (.web req, res, #js { :target target })))))))

(defn register-route-request [dproxy req res] 
  (.setEncoding req "utf-8")
  (let [body (atom [])]
   (.on req "data" (fn [data] (swap! body conj data)))
   (.on req "end"
     (fn []
      (aset req "body" (apply str @body))
      (register! dproxy req res)))))

;; js interop

(defn ^:export DynamicProxy []
  (let [routes (atom {:any empty-route-map
                      :raw []})
        proxy (create-proxy #js {"xfwd" true})]
    (this-as dproxy
      (.on proxy "error" (partial on-proxy-error dproxy))
      (doto dproxy
        (aset "routes-atom" routes)
        (aset "proxy" proxy))
      dproxy)))

(set-prototype! DynamicProxy
  (.create js/Object (-> events (aget "EventEmitter") (aget "prototype"))))

(set-prototype! DynamicProxy "addRoutes"
  (fn addRoutes [routes]
    (this-as dproxy
      (add-routes! dproxy routes)
      dproxy)))

(set-prototype! DynamicProxy "getRoutes"
  (fn getRoutes []
    (this-as dproxy
      (-> dproxy
          get-host-routes
          :raw
          clj->js))))

(set-prototype! DynamicProxy "registerRouteRequest"
  (fn ^:export registerRouteRequest [req res]
    (this-as dproxy
      (register-route-request dproxy req res))))

(set-prototype! DynamicProxy "proxyRequest"
  (fn registerRouteRequest [req res]
    (this-as dproxy
      (proxy-request dproxy req res))))

(set-prototype! DynamicProxy "registerRoute"
  (fn registerRoute [route]
    (this-as dproxy
      (register-route! dproxy route))))

(defn -main []
  (aset js/module "exports" DynamicProxy))

(set! *main-cli-fn* -main)
