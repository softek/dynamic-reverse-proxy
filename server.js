var http = require("http"),
    server = http.createServer(),
    dynamicProxy = require("./index")();
 
server.on("request", function (req, res) {
  if (req.url.match(/^\/register/i)) {
    dynamicProxy.registerRouteRequest(req, res);
  }
  else {
    dynamicProxy.proxyRequest(req, res);
  }
});
 
var port = process.env.port || 3000;
server.listen(port, function () {
  console.log("Reverse Proxy started, listening on port " + port +
              (dynamicProxy.debug ? " \x1b[33m(DEBUG build)\x1b[0m" : ""));
});
