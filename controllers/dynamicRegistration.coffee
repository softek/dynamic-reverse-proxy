express = require "express"
app = module.exports = express.createServer()
parseBody = express.bodyParser()
app.proxyRoutes = {}

app.post "/register", (req, res, next) ->
   return next "Cannot register with proxy from remote servers." if isntLocal req

   parseBody req, res, () ->
      application = req.body
      return next "Path, host, and port are required to register." if missingConfig application

      app.proxyRoutes[application.path] = application
      console.log "Registered: " + JSON.stringify application
      res.send "Registered at /#{application.path}."

isntLocal = (req) ->
   req.connection.remoteAddress isnt "127.0.0.1"

missingConfig = (application) ->
   not (application? and application.path? and application.host? and application.port?)