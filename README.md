# dynamic-reverse-proxy

A reverse proxy built on [http-proxy](https://github.com/nodejitsu/node-http-proxy) that is configured by REST.

[Dynamic-reverse-proxy](http://github.com/softek/dynamic-reverse-proxy) exposes several web apps on a single port so you can:
* **Use the right language for the job.** Maybe you want to use the best parts of [Clojure](http://clojure.org/), [Node.js](https://nodejs.org/about/), [Erlang](http://www.erlang.org/), [Ruby](https://www.ruby-lang.org/).  Put each project on its own port and use dynamic-reverse-proxy to expose a unified front to the world.
* **Partition parts of the web app for stability**.  Put experimental features in their own process and relay the traffic.
* **Only bother with HTTPS in one place**.  You can expose HTTPS to the world, but your "behind the proxy" apps don't need to worry about HTTPS.

##### Latest stable release: 0.6.0 [Doc](https://github.com/softek/dynamic-reverse-proxy/blob/0b770e23c59818fe514e41897f7bf609efff474b/README.md)
`npm install dynamic-reverse-proxy`

##### Latest unstable release: 0.7.0-alpha2
`npm install dynamic-reverse-proxy@0.7.0-alpha2`

## Starting the server

#### Stand-alone (available starting 0.7.0)
```dos
npm install dynamic-reverse-proxy
cd node_modules\dynamic-reverse-proxy
SET port=3000
npm start
```

#### With code
```javascript
var http = require("http"),
    server = http.createServer(),
    dynamicProxy = require("dynamic-reverse-proxy")();

server.on("request", function (req, res) {
  if (req.url.match(/^\/register/i)) {
    dynamicProxy.registerRouteRequest(req, res);
  }
  else {
    dynamicProxy.proxyRequest(req, res);
  }
});

server.listen(3000, function () {
  console.log("Reverse Proxy started, listening on port 3000");
});
```

## Configuring the proxy

The reverse proxy is configured to route based on the first segment of the path. For example:
 - `/` would route to the host registered at `/`
 - `/application1` would route to the host registered at `/application1`
 - `/application2/test/index.html` would route to the host registered at `/application2`

To register the a host with the proxy:

```HTTP
POST /register HTTP/1.1
Host: localhost:3000
Content-Length: 28
Content-Type: application/json

{"prefix": "/", "port":1234}
```

Now, any request made to `http://localhost:3000/` will be sent to `http://localhost:1234/`.

To register another host:

```HTTP
POST /register HTTP/1.1
Host: localhost:3000
Content-Length: 32
Content-Type: application/json

{"prefix": "/test", "port":4321}
```

Now, any request made to `http://localhost:3000/test` will be sent to `http://localhost:4321/test`.

### Wait, what about security? 

Well, it's pretty lame (but functional) at the moment. Only requests originating from the same machine as the proxy are allowed to register.

## Events

The dynamic proxy object that is returned is an EventEmitter with the following events:

 - `proxyError` is passed `(error, host, request, response)` and is emitted when:
     - A request is sent to a known host but the request could not be proxied (likely the host was unreachable). If no handler ends the response back to the original client, `500 Internal Server Error` will be returned.
     - No host could be found to handle the request. In this case, the `error` will be `NOT_FOUND`. If no handler ends the response back to the original client, `501 Not Implemented` will be returned.

 - `registerError` is passed `(error, request, response)` and is emitted when a request is sent to `/register` but it could not be handled correctly. Error will be one of the following:
     - `FORBIDDEN` (not allowed)
     - `METHOD_NOT_ALLOWED` (must be a POST)
     - `BAD_REQUEST` (not parsable as JSON)
     - `INCOMPLETE_REQUEST` (path and port were not supplied)

 - `routeRegistered` is passed `(host)` and is emitted when a request is sent to `/register` and it was successful.

## Methods

 - `dynamicProxy.addRoutes(routes)` adds an object of routes in the following format:

```JSON
{
   "/": {
      "prefix": "",
      "port": 1234
   },
   "/test": {
      "prefix": "test",
      "port": 4321
   }
}
 ```

## Troubleshooting
This package comes with both an optimized/minified "release" version, and a more-readable "debug" version.  To use the debug version, set `debug: true` in ./config.js.

## Development

### Roadmap
* Allowing HOST-specific routes (`http://example.com/` gets a different route than `http://subdomain.example.com/` depending on the host header)
* Requiring encryption for some routes (for example, force the `/login` route to use HTTPS)
* Performance improvements for proxies with many routes. Before v0.7.0, the complexity was o(n) and O(n) because it uses the longest prefix that works.  This may become more important when certain areas of sites force HTTPS - that may use more routes, depending on your URL scheme.

### Scripts
* To set the version (in package.json, project.clj, resources/version.js): `lein set-version 0.x.x-alphaX`.  You can also `:dry-run true` to [see what changes would be made](https://github.com/pallet/lein-set-version#dry-run-mode).
