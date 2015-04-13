(ns com.softekinc.dynamic-reverse-proxy-test
  (:require [cljs.nodejs :as nodejs]
            [cljs.test :as t :refer-macros [deftest testing is]]
            [com.softekinc.dynamic-reverse-proxy
              :refer [url-matches-route? normalize-prefix normalize-path
                      route-for-url expand-route longest-string-first]]
            [clojure.string :refer [blank?]]))

(deftest test_normalize-prefix
  (is (= "/" (normalize-prefix "")))
  (is (= "/" (normalize-prefix "/")))
  (is (= "/a" (normalize-prefix "/a")))
  (is (= "/a" (normalize-prefix "/a/")))
  (is (= "/case" (normalize-prefix "/CASE/"))))

(deftest test_normalize-path
  (is (= "" (normalize-path "")))
  (is (= "/" (normalize-path "/")))
  (is (= "/a" (normalize-path "/a")))
  (is (= "/case" (normalize-path "/CASE"))))

(deftest test_longest-string-first
  (is (== 0 (longest-string-first "" "")))
  (is (== 0 (longest-string-first "1" "1")))
  (is (== 0 (longest-string-first "22" "22")))
  (is (< 0 (longest-string-first "1" "22")))
  (is (> 0 (longest-string-first "22" "1")))
  ; same-length strings are compared alphabetically
  (is (> 0 (longest-string-first "a" "b")))
  (is (< 0 (longest-string-first "b" "a")))
  ; put it all together
  (is (= ["4444" "22" "bb" "A" "a" "b" "c" ""]
         (sort-by identity longest-string-first
          ["" "a" "b" "c" "A" "22" "bb" "4444"]))))

(defn route
  ([prefix]
    (clj->js
      {:prefix (normalize-prefix prefix)}))
  ([prefix & kvs]
  (clj->js
    (apply assoc {:prefix (normalize-prefix prefix)} kvs))))

(defn http-url [path & kvs]
  (apply
    assoc
    {:encrypted false
     :host      "unit.test"}
    :path (normalize-path path)
    kvs))

(deftest test_url-matches-route?
  (testing "Route matches if prefix matches at start of uri"
    (is (not (url-matches-route? (http-url "/") (route "/a"))))
    (is (url-matches-route? (http-url "/") (route "/")))
    (is (url-matches-route? (http-url "/a") (route "/")))
    ;; normalization trims the trailing / from route, making the / optional
    (is (url-matches-route? (http-url "/some/page") (route "/some/page/")))
    (is (url-matches-route? (http-url "/SOme/page") (route "/soME/page/")))
    (is (url-matches-route? (http-url "/some/page?p=1") (route "/some/page/")))))

(deftest test_route-for-url
  (testing "route-for-url returns first matching route. (The sequence of routes matters!)"
    (is (= nil (route-for-url [] (http-url "/"))))
    (is (= nil (route-for-url [(route "/deeper")] (http-url "/"))))
    (is (let [root-route (route "/")]
          (= root-route
             (route-for-url [(route "/deeper/still")
                             root-route
                             (route "/deeper")]
                            (http-url "/deeper")))))))

(deftest test_expand-routes
  (testing "When host-names aren't specified, yield 1 route that applies to any host"
    (let [js-route #js {:prefix "/", :host "localhost"}]
      (is (= [{:prefix "/", :req-host-name :any, :route js-route}]
             (expand-route js-route)))))
  (testing "When host-names are specified, yield a route for each"
    (let [js-route #js {:prefix "/", :host "localhost", :reqHostNames #js ["api.example.com" "v1.api.example.com"]}]
      (is (= [{:prefix "/", :req-host-name "api.example.com", :route js-route}
              {:prefix "/", :req-host-name "v1.api.example.com", :route js-route}]
             (expand-route js-route))))))


(t/run-tests)
