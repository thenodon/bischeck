# buildr script bischeck

# Version number for this release
VERSION_NUMBER = "0.1.0"

# Group identifier for your projects
GROUP = "bischeck"

COPYRIGHT = "Anders Håål, Ingenjörsbyn AB 2011"

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://www.ibiblio.org/maven2/"

desc "The bischeck plugin project"


define "bischeck" do

  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT
  compile.with Dir['lib/**/*.jar']
  run.using :main => ["com.ingby.socbox.bischeck.Execute","false"],
    :classpath => ["target/resources:target/classes:lib/*"]
  
  
  build do
  filter('src/main/scripts').into('target/scripts').
    using('version'=>version, 'created'=>Time.now).run
  end

  
  package :jar
  
  package(:file=>_(:target,"#{id}-#{version}.tgz")).
    include(_(:target,'*.jar'),
        _('install'), 
        _('doc'), 
        _('target/resources'),
        _('target/scripts'),
        _('lib'), :path=>"#{id}-#{version}")

end

