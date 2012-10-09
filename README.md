# dynamic-reverse-proxy

A reverse proxy built on [http-proxy](https://github.com/nodejitsu/node-http-proxy) that is configured by REST.

## Starting the server

```javascript
var dynamicProxy = require("dynamic-reverse-proxy");

dynamicProxy().listen(port, function () {
   console.log("Reverse Proxy started, listening on port " + port);
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
Host: localhost:80
Content-Length: 26
Content-Type: application/x-www-form-urlencoded
{"path": "/", "port":1234}
```

Now, any request made to `http://localhost:80/` will be sent to `http://localhost:1234/`.

To register another host:

```HTTP
POST /register HTTP/1.1
Host: localhost:80
Content-Length: 30
Content-Type: application/x-www-form-urlencoded
{"path": "/test", "port":4321}
```

Now, any request made to `http://localhost:80/test` will be sent to `http://localhost:4321/test`.