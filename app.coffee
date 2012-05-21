httpProxy = require "http-proxy"
express = require "express"

routes = {}
app = express.createServer()
proxy = new httpProxy.RoutingProxy()

register = (path, server) ->
   if routes[path]?
      for other in routes[path]
         return true if other.host is server.host and other.port is server.port
   else
      routes[path] = []

   routes[path].push server

app.post "/register", (req, res) ->
   if req.connection.remoteAddress is "127.0.0.1"
      express.bodyParser() req, res, () ->
         child = req.body

         if child? and child.path? and child.host? and child.port?
            count = register child.path, child
            count = count - 1
            s = if count is 1 then "" else "s"
            res.send "#{count} other server#{s} registered at /#{child.path}"
         else
            res.statusCode = 400
            res.send "Properties path, host, and port are required to register."

app.use (req, res) ->
   urlParts = req.url.split "/"
   hostKey = urlParts[1].toLowerCase()
   hosts = routes[hostKey] or routes[""]
   host = hosts.shift()
   
   console.log "Sending #{req.url} to #{host.host}:#{host.port}..."
   proxy.proxyRequest req, res, host
   hosts.push host

app.listen 8051
