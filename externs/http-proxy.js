/**
 BEGIN_NODE_INCLUDE
 var httpProxy = require('http-proxy');
 var events = require('events');
 END_NODE_INCLUDE
 */

/**
 * @type {Object.<string,*>}
 */
var httpProxy = {};

/**
 * @param {Object.<string,*>}
 * @return {httpProxy.ProxyServer}
 */
httpProxy.createProxyServer = function createProxyServer(options) {};

/**
 * @param {Object.<string,*>}
 * @return {httpProxy.ProxyServer}
 */
httpProxy.createServer = function createProxyServer(options) {};

/**
 * @param {Object.<string,*>}
 * @return {httpProxy.ProxyServer}
 */
httpProxy.createProxy = function createProxyServer(options) {};


/**
 * @constructor
 * @extends events.EventEmitter
 */
var ProxyServer = {};
ProxyServer.emit = function (){};

ProxyServer.web = function (req, res, data){};
