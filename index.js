var DynamicProxy = require("./lib/dynamicProxy"),
    defaults = {
       logger: require("winston"),
       routes: {}
    };

module.exports = function (options) {
   options = merge(defaults, options);
   return new DynamicProxy(options);
};

var merge = function (base, addition) {
   var result = {};
   for (key in base || {}) { result[key] = base[key]; }
   for (key in addition || {}) { result[key] = addition[key]; }
   return result;
};