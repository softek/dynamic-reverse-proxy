var httpProxy = require("http-proxy");

var DynamicProxy = module.exports = function (options) {
   this.logger = options.logger;
   this.server = options.server;
   this.routes = options.routes;

   this.proxy = new httpProxy.RoutingProxy();
   this.proxy.on("proxyError", this.proxyError.bind(this));
   this.server.on("request", this.handleRequest.bind(this));
};

DynamicProxy.prototype.proxyError = function (error, req, res) {
   this.logger.error("Failed to proxy to http://" + req.host.host + ":" + req.host.port + "/" + req.host.path + ":", error.toString());
   res.statusCode = 500;
   return res.end();
};

DynamicProxy.prototype.handleRequest = function (req, res) {
   if (req.url.match(/^\/register/i)) {
      var requestBody = "";

      req.setEncoding("utf-8");
      req.on("data", function (data) { requestBody += data; });
      req.on("end", function () {
         req.body = requestBody;
         this.registerProxyRequest(req, res);
      }.bind(this));
   }
   else {
      this.proxyRequest(req, res);
   }
};

DynamicProxy.prototype.registerProxyRequest = function (req, res) {
   var config, host, message;

   if (req.connection.remoteAddress !== "127.0.0.1") {
      this.logger.info("Failed to register, machine must be localhost:", req.connection.remoteAddress);
      res.statusCode = 403; // Forbidden
      res.write(JSON.stringify({ message: "Cannot register with proxy from remote machines." }));
      return res.end();
   }

   if (req.method.toUpperCase() !== "POST") {
      this.logger.info("Failed to register, method wasn't POST:", req.method);
      res.statusCode = 405; // Method Not Allowed
      res.write(JSON.stringify({ message: "Registration must occur on a POST request." }));
      return res.end();
   }

   try {
      config = JSON.parse(req.body);
   } 
   catch (e) {
      this.logger.info("Failed to register, invalid JSON:", e.toString());
      res.statusCode = 400; // Bad Request
      res.write(JSON.stringify({ message: "Could not parse body as JSON: " + e.toString() }));
      return res.end();
   }

   if (!(config && config.path && config.port)) {
      this.logger.info("Failed to register, incomplete data:", config);
      res.statusCode = 400; // Bad Request
      res.write(JSON.stringify({ message: "Path and port are required." }));
      return res.end();
   }

   host = {
      host: "localhost",
      port: config.port,
      path: (config.path[0] === "/" ? config.path.substring(1) : config.path).toLowerCase()
   };

   this.routes[host.path] = host;

   message = "Registered your http://" + host.host + ":" + host.port + "/" + host.path + " at my /" + host.path;
   this.logger.info(message);

   res.statusCode = 200;
   res.write(JSON.stringify({
      message: message,
      host: host.host,
      port: host.port,
      path: host.path
   }));

   return res.end();
};

DynamicProxy.prototype.proxyRequest = function (req, res) {
   var parts = req.url.split("/"),
       root = parts[1].toLowerCase(),
       host = this.routes[root] || this.routes[""];

   if (!host) {
      this.logger.error("Failed to proxy, no matching host:", req.url);
      res.statusCode = 501;
      return res.end();
   }

   req.host = host;
   this.proxy.proxyRequest(req, res, host);
};