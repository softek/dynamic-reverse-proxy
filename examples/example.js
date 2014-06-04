var http = require("http"),
    server = http.createServer(),
    dynamicProxy = require("../index")();

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

// Create a server we can proxy to.
var server = http.createServer(function (req, res) {
	res.writeHead(200, { 'Content-Type': 'text/plain' });
	res.write(JSON.stringify(req.headers, true, 2));
	res.end();
});

server.listen(3005, function () {
	console.log("Target http server started, listening on port 3005");
});

// Now register the target server for /foo
dynamicProxy.registerRoute({
	host: 'localhost',
	prefix: '/foo',
	port: '3005'
});