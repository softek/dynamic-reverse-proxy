var config = require("./config")
var isDebug = config.debug ? true : false;
module.exports = function () {
  if (isDebug) {
    var pxy = new (require("./lib/debug/dynamic-proxy.js"))();
    pxy.debug = true;
    return pxy;
  } else {
   return new (require("./lib/release/dynamic-proxy.js"))();
  }
};
