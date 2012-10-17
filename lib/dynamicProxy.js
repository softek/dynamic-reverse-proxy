var httpProxy = require("http-proxy"),
    events = require("events");

var DynamicProxy = module.exports = function (server) {
   server.on("request", _onRequest.bind(this));

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

var _onProxyError = function (error, req, res) {
   this.emit("proxyError", error, req.host, req, res);

   if (res.writable) {
      res.statusCode = 500;
      return res.end();
   }
};

var _onRequest = function (req, res) {
   if (req.url.match(/^\/register/i)) {
      var requestBody = "";

      req.setEncoding("utf-8");
      req.on("data", function (data) { requestBody += data; });
      req.on("end", function () {
         req.body = requestBody;
         _register.bind(this)(req, res);
      }.bind(this));
   }
   else {
      _proxyRequest.bind(this)(req, res);
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

   var config;

   try {
      config = JSON.parse(req.body);
   } 
   catch (e) {
      this.emit("registerError", new Error("BAD_REQUEST"), req, res);

      if (res.writable) {
         res.statusCode = 400;
         return res.end();
      }
   }

   if (!(config && config.path && config.port)) {
      this.emit("registerError", new Error("INCOMPLETE_REQUEST"), req, res);

      if (res.writable) {
         res.statusCode = 400;
         return res.end();
      }
   }

   var host = {
      host: "localhost",
      port: config.port,
      path: (config.path[0] === "/" ? config.path.substring(1) : config.path).toLowerCase()
   };

   this.routes[host.path] = host;

   res.statusCode = 200;
   res.write(JSON.stringify({
      message: "Registered your http://" + host.host + ":" + host.port + "/" + host.path + " at my /" + host.path,
      host: host.host,
      port: host.port,
      path: host.path
   }));

   res.end();
   this.emit("routeRegistered", host);
};

var _proxyRequest = function (req, res) {
   var parts = req.url.split("/"),
       root = parts[1].toLowerCase(),
       host = this.routes[root] || this.routes[""];

   if (!host) {
      this.emit("proxyError", new Error("NOT_FOUND"), req, res);

      if (res.writable) {
         res.statusCode = 501;
         return res.end();
      }

      return;
   }

   req.host = Object.create(host);
   this.proxy.proxyRequest(req, res, host);
};