httpProxy = require "http-proxy"
express = require "express"

app = express.createServer()
proxy = new httpProxy.RoutingProxy()

routeRegistration = require "./dynamicRegistration"
routes = routeRegistration.proxyRoutes
app.use routeRegistration

app.use (req, res, next) ->
   urlParts = req.url.split "/"
   rootPath = urlParts[1].toLowerCase()
   host = routes[rootPath] or routes[""]

   return next "No routes defined! Blargh!" if not host?
   proxy.proxyRequest req, res, host

app.listen 80
