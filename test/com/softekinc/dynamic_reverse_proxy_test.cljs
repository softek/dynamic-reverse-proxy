(ns com.softekinc.dynamic-reverse-proxy-test
  (:require [cljs.nodejs :as nodejs]
            [cljs.test :as t :refer-macros [deftest testing is]]
            [com.softekinc.dynamic-reverse-proxy :refer [url-matches-route? normalize-prefix]]
            [clojure.string :refer [blank?]]))

(defn route
  ([prefix]
    (clj->js
      {:prefix (normalize-prefix prefix)}))
  ([prefix & kvs]
  (clj->js
    (apply assoc {:prefix (normalize-prefix prefix)} kvs))))

(defn any-host-route [prefix]
  (route prefix))

(defn http-url [path & kvs]
  (apply
    assoc
    {:encrypted false
     :host      "unit.test"}
    :path path
    kvs))

(deftest test_url-matches-route?
  (testing "Route matches if prefix matches at start of uri"
    (is (not (url-matches-route? (http-url "/") (any-host-route "/a"))))
    (is (url-matches-route? (http-url "/") (any-host-route "/")))
    (is (url-matches-route? (http-url "/a") (any-host-route "/")))
    ;; normalization trims the trailing / from route, making the / optional
    (is (url-matches-route? (http-url "/some/page") (any-host-route "/some/page/")))
    (is (url-matches-route? (http-url "/some/page?p=1") (any-host-route "/some/page/"))))

  (testing "The url host must match, if specified in the route"
    ;; same host
    (is (url-matches-route? (http-url "/" :host "right") (route "/" :host "right")))
    (is (url-matches-route? (http-url "/a" :host "right") (route "/" :host "right")))
    ;; same host, different case
    (is (url-matches-route? (http-url "/" :host "RIGHT") (route "/" :host "right")))
    (is (url-matches-route? (http-url "/a" :host "RIGHT") (route "/" :host "right")))
    ;; different host
    (is (not (url-matches-route? (http-url "/" :host "wrong") (route "/a" :host "right"))))
    (is (not (url-matches-route? (http-url "/" :host "wrong") (route "/" :host "right"))))
    (is (not (url-matches-route? (http-url "/a" :host "wrong") (route "/" :host "right"))))))

(t/run-tests)
