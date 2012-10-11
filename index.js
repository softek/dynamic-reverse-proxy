var DynamicProxy = require("./lib/dynamicProxy"),
    http = require("http"),
    defaults = {
       logger: require("winston"),
       routes: {},
       server: http.createServer()
    };

module.exports = function (options) {
   options = merge(defaults, options || {});
   return new DynamicProxy(options);
};

var merge = function (base, addition) {
   var result = {};
   for (key in base || {}) { result[key] = base[key]; }
   for (key in addition || {}) { result[key] = addition[key]; }
   return result;
};