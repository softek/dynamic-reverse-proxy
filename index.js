var DynamicProxy = require("./lib/dynamicProxy");

module.exports = function (server) {
   if (!server) { throw new Error("HTTP or HTTPS server must be supplied."); }
   return new DynamicProxy(server);
};