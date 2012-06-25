SERVICE_NAME = 'illuminate-reverse-proxy'

def inWindows?
   ENV['OS'] == 'Windows_NT'
end

def isntRoot? 
   Process.uid != 0
end

def initctl(method)
   if inWindows? then $stderr.puts "Skipping upstart #{method} (in Windows)..."
   elsif isntRoot? then $stderr.puts "Skipping upstart ${method} (must be root)..."
   else `initctl #{method} #{SERVICE_NAME}` end
end

task :default do
   puts `rake -D`
end

desc "Installs npm, upstart"
task :install do
   Rake::Task['npm:install'].execute
   Rake::Task['upstart:install'].execute
end

desc "Uninstalls npm, upstart"
task :uninstall do
   Rake::Task['npm:uninstall'].execute
   Rake::Task['upstart:uninstall'].execute
end

namespace :npm do
   desc "Installs node package dependencies."
   task :install do
      `npm install`
   end

   desc "Installs node package and development dependencies."
   task :installdev do 
      `npm install --dev`
   end

   desc "Uninstalls node package dependencies (including dev dependencies)."
   task :uninstall do
      `rm -r node_modules`
   end
end

namespace :upstart do
   desc "Installs the application as a service using upstart."
   task :install do
      if inWindows? then $stderr.puts "Skipping upstart install (in Windows)..."
      elsif isntRoot? then $stderr.puts "Skipping upstart install (must be root)..."
      else 
         dirname = File.dirname(__FILE__)
         file = dirname + "/config/upstart.conf"

         `ln -s #{file} /etc/init/#{SERVICE_NAME}.conf` 
         initctl "start"
      end
   end

   desc "Uninstalls upstart service hooks."
   task :uninstall do
      if inWindows? then $stderr.puts "Skipping upstart uninstall (in Windows)..."
      elsif isntRoot? then $stderr.puts "Skipping upstart uninstall (must be root)..."
      else 
         `rm /etc/init/#{SERVICE_NAME}.conf` 
      end
   end

   desc "Stops the service."
   task :stop do
      initctl "stop"
   end

   desc "Restarts the service."
   task :restart do
      initctl "restart"
   end

   desc "Starts the service."
   task :start do
      initctl "start"
   end
end
