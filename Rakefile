def inWindows?
   ENV['OS'] == 'Windows_NT'
end

task :default do
   puts `rake -D`
end

desc "Installs npm, upstart"
task :install do
   Rake::Task['npm:install'].execute
   Rake::Task['upstart:install'].execute
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
      if inWindows? then $stderr.puts "Skipping upstart install in Windows..."
      else `ln -s config/upstart.conf /etc/init/illuminate-reverse-proxy.conf` end
   end

   desc "Uninstalls upstart service hooks."
   task :uninstall do
      if inWindows? then $stderr.puts "Skipping upstart uninstall in Windows..."
      else `rm /etc/init/illuminate-reverse-proxy.conf` end
   end
end