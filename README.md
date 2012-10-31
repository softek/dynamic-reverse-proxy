# dynamic-reverse-proxy

A reverse proxy built on [http-proxy](https://github.com/nodejitsu/node-http-proxy) that is configured by REST.

## Starting the server

```javascript
var http = require("http"),
    server = http.createServer(),
    dynamicProxy = require("dynamic-reverse-proxy")();

server.on("request", function (req, res) {
  if (req.url.match(/^\/register/i)) {
    proxy.registerRouteRequest(req, res);
  }
  else {
    proxy.proxyRequest(req, res);
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