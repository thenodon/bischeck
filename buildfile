# buildr script bischeck

# Version number for this release
VERSION_NUMBER = "0.3.2A"

# Group identifier for your projects
GROUP = "bischeck"

COPYRIGHT = "Anders Håål, Ingenjörsbyn AB 2011"

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://www.ibiblio.org/maven2/"

desc "The bischeck plugin project"

require 'buildr_jaxb_xjc'

define "bischeck" do

  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = "COPYRIGHT Anders Håål, Ingenjörsbyn 2011"
  
  
  compile.from(_('src/main/java'),_('src/main/generated'))
  
  compile.with Dir['lib/**/*.jar']

#  run.using :main => ["com.ingby.socbox.bischeck.Execute","false"],
#    :classpath => ["target/resources:target/classes:lib/*"]
  
  build do
  filter('src/main/scripts').into('target/scripts').
    using('version'=>version, 'created'=>Time.now).run
  end

  build do
  filter('src/main/migscripts').into('target/migscripts').
    using('version'=>version, 'created'=>Time.now).run
  end


  build do
  filter('src/main/php/pnp4nagios/templates/').into('target/scripts/pnp4nagios/templates/').
    using('version'=>version, 'created'=>Time.now).run
  end


  doc.using(:windowtitle => "bischeck", :private => true)

  
  package :jar
  
  package(:file=>_(:target,"#{id}-#{version}.tgz")).
    include(_(:target,'*.jar'),
        _('target/doc'),
        _('install'), 
        _('migrationpath.txt'),
        _('doc/README'), 
        _('doc/LICENSE'), 
        _('etc'), 
        _('examples'),
        _('target/resources'),
        _('target/scripts'),
        _('target/migscripts'),
        _('target/scripts/pnp4nagios/templates'),
        _('lib'), :path=>"#{id}-#{version}")

end

