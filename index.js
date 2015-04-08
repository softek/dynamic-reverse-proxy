var config = require("./config")
var isDev = config.dev ? true : false;
var isDebug = config.debug ? true : false;
module.exports = function () {
  if (isDev) {
    try {
        require("source-map-support").install();
    } catch(err) {
    }
    var DynamicProxy = require("./out/dev/dynamic-proxy.js");
    var pxy = new DynamicProxy();
    pxy.debug = true;
    return pxy;
  } else if (isDebug) {
    var pxy = new (require("./lib/debug/dynamic-proxy.js"))();
    pxy.debug = true;
    return pxy;
  } else {
   return new (require("./lib/release/dynamic-proxy.js"))();
  }
};
