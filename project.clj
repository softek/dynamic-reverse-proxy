(defproject dynamic-reverse-proxy "0.7.0-alpha3"
  :description "Dynamic reverse proxy"
  :url "http://github.com/softek/dynamic-reverse-proxy"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3126"]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-3126"]
                                  [lein-set-version "0.4.1"]]}}
  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-simpleton "1.3.0"]]
  :set-version
    {:updates [{:path "resources/version.js"}
               {:path "README.md"
                :search-regex #"Latest unstable release[^`]+`[^`]+"}
               {:path "package.json"
                :search-regex #"\"version\"\s*:\s*\"(\\\"|[^\"])*\""}]} ;"

  :clean-targets ["out" "lib"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src" "test"]
              :notify-command ["npm.cmd" "test"]
              :compiler {
                :output-to "out/dev/dynamic-proxy.js"
                :output-dir "out/dev/"
                :optimizations :simple
                :source-map "out/dev/dynamic-proxy.js.map"
                :pretty-print true
                :preamble ["version.js"]
                :target :nodejs
                :main "com.softekinc.dynamic-reverse-proxy"
                :externs []}}
             {:id "debug"
              :source-paths ["src"]
              :compiler {
                :output-to "lib/debug/dynamic-proxy.js"
                :output-dir "lib/debug"
                :optimizations :simple
                :source-map "lib/debug/dynamic-proxy.js.map"
                :pretty-print true
                :preamble ["version.js"]
                :target :nodejs}}
             {:id "release"
              :source-paths ["src"]
              :compiler {
                :output-to "lib/release/dynamic-proxy.js"
                :output-dir "lib/release"
                :optimizations :advanced
                :source-map "lib/release/dynamic-proxy.js.map"
                :pretty-print false
                :preamble ["version.js"]
                :target :nodejs
                :externs ["node_modules/closurecompiler-externs/events.js"
                          "node_modules/closurecompiler-externs/stream.js"
                          "node_modules/closurecompiler-externs/net.js"
                          "node_modules/closurecompiler-externs/http.js"
                          "node_modules/closurecompiler-externs/https.js"
                          "externs/http-proxy.js"]}}]}

  :aliases {
    "build-release" ["do"
                      "clean"
                      ["cljsbuild" "once" "debug"]
                      ["cljsbuild" "once" "release"]]
    })
