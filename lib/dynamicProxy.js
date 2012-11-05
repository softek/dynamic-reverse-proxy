var httpProxy = require("http-proxy"),
    events = require("events");

var DynamicProxy = module.exports = function () {
   this.routes = {};
   this.proxy = new httpProxy.RoutingProxy();
   this.proxy.on("proxyError", _onProxyError.bind(this));
};

DynamicProxy.prototype = Object.create(events.EventEmitter.prototype);

DynamicProxy.prototype.addRoutes = function (routes) {
   for (var key in routes) {
      this.routes[key] = routes[key];
   }
};

DynamicProxy.prototype.registerRouteRequest = function (req, res) {
   var requestBody = "";

   req.setEncoding("utf-8");
   req.on("data", function (data) { requestBody += data; });
   req.on("end", function () {
      req.body = requestBody;
      _register.bind(this)(req, res);
   }.bind(this));
};

DynamicProxy.prototype.proxyRequest = function (req, res) {
   _proxyRequest.bind(this)(req, res);
};

var _onProxyError = function (error, req, res) {
   this.emit("proxyError", error, req.host, req, res);

   if (res.writable) {
      res.statusCode = 500;
      return res.end();
   }
};

var _register = function (req, res) {
   if (req.connection.remoteAddress !== "127.0.0.1") {
      this.emit("registerError", new Error("FORBIDDEN"), req, res);

      if (res.writable) {
         res.statusCode = 403;
         return res.end();
      }
   }

   if (req.method.toUpperCase() !== "POST") {
      this.emit("registerError", new Error("METHOD_NOT_ALLOWED"), req, res);

      if (res.writable) {
         res.statusCode = 405;
         return res.end();
      }
   }

   var host;

   try {
      host = JSON.parse(req.body);
   } 
   catch (e) {
      this.emit("registerError", new Error("BAD_REQUEST"), req, res);

      if (res.writable) {
         res.statusCode = 400;
         return res.end();
      }
   }

   if (!(host && host.prefix && host.port)) {
      this.emit("registerError", new Error("INCOMPLETE_REQUEST"), req, res);

      if (res.writable) {
         res.statusCode = 400;
         return res.end();
      }
   }

   host.prefix = (host.prefix[0] === "/" ? host.prefix : "/" + host.prefix).toLowerCase();
   host.host = "localhost";

   this.routes[host.prefix] = host;

   res.statusCode = 200;
   res.write(JSON.stringify({
      message: "Registered.",
      host: host.host,
      port: host.port,
      prefix: host.prefix
   }));

   res.end();
   this.emit("routeRegistered", host);
};

var _proxyRequest = function (req, res) {
   var url = req.url.toLowerCase(),
       specificity = 0,
       host;

   for (var key in this.routes) {
      var route = this.routes[key];

      if (url.indexOf(route.prefix) === 0 && route.prefix.length > specificity) {
         host = route;
         specificity = route.prefix.length;
      }
   }

   if (!host) {
      this.emit("proxyError", new Error("NOT_FOUND"), null, req, res);

      if (res.writable) {
         res.statusCode = 501;
         return res.end();
      }

      return;
   }

   req.host = Object.create(host);
   this.proxy.proxyRequest(req, res, host);
};