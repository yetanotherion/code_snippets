// https://issues.apache.org/jira/browse/GROOVY-8097
@Grab(group="org.apache.ivy", module="ivy", version="2.2.0")

import groovy.grape.Grape
import groovy.grape.GrapeIvy
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.Ivy
import java.lang.reflect.Field;

class Main {

    private static String home = System.getenv("HOME")

    static class Grabber implements Runnable {
        private int i
        private final GrapeIvy grapeIvy

        static IvySettings createIvySettings(String resolutionPath) {
            // Copy/Paste/Purged from GrapeIvy.groovy
            IvySettings settings = new IvySettings()

            settings.load(new File(home, ".groovy/grapeConfig.xml"))
            // set up the cache dirs
            settings.defaultCache = new File(home, ".groovy/grapes")
            settings.setVariable("ivy.default.configuration.m2compatible", "true")
            settings.setDefaultResolutionCacheBasedir(resolutionPath)
            return settings
        }

        static GrapeIvy ivyWithCustomResolutionPath(String resolutionPath) {
            Class<?> grapeIvyClass = Class.forName("groovy.grape.GrapeIvy");
            Object instance = grapeIvyClass.newInstance()
            Field field = grapeIvyClass.getDeclaredField("ivyInstance");
            field.setAccessible(true);
            field.set(instance, Ivy.newInstance(createIvySettings(resolutionPath)));
            return ((GrapeIvy)instance)
        }

        Grabber(int i) {
            this.i = i
            this.grapeIvy = ivyWithCustomResolutionPath(home + "/.groovy/grapes/.cache/resolution/${i}")
        }

        private void fGrab(doGrab, jVersion, gVersion) {
            try {
                doGrab(group: 'org.jenkins-ci.main',
                       module: 'jenkins-core',
                       version: jVersion,
                       transitive: true,
                       autoDownload: true)
                doGrab(group: 'org.codehaus.groovy',
                       module: 'groovy-all',
                       version: gVersion,
                       transitive: true,
                       autoDownload: true)
            }
            catch (Exception e) {
                System.err.println(e)
                e.printStackTrace()
                System.exit(1)
            }
        }

        private void grabOk(jVersion, gVersion) {
            fGrab({ this.grapeIvy.grab(it) }, jVersion, gVersion)
        }

        private void grabNok(jVersion, gVersion) {
            fGrab({ Grape.grab(it) }, jVersion, gVersion)
        }

        private void grab(jVersion, gVersion) {
            grabOk(jVersion, gVersion)
            //grabNok(jVersion, gVersion) gets
            // java.lang.IllegalStateException: impossible to get artifacts when data has not been loaded.
            // IvyNode = org.apache.ant#ant-launcher;1.7.1
        }

        @Override
        void run() {
            def jenkinsVersions = ["1.396", "1.397", "1.398",
                                   "1.399", "1.400", "1.401",
                                   "1.403", "1.404", "1.405",
                                   "1.406", "1.407", "1.408",
                                   "1.409.1", "1.409.2", "1.409.3",
                                   "1.409"]
            def groovyAllVersions = ["1.6.0", "1.6.9", "1.7.4",
                                     "1.7.5", "1.7.6", "1.8.3",
                                     "1.8.9", "2.0.0", "2.0.1",
                                     "2.1.5", "2.1.6", "2.1.9",
                                     "2.4.3", "2.4.4", "2.4.7",
                                     "2.4.8"]
            [jenkinsVersions, groovyAllVersions].transpose().each {
                grab(it[0], it[1])
            }
        }
    }

    static void main(String[] args) {
        def numThreads = 1
        for (int i in 0..(numThreads - 1)) {
            Runnable grabber = new Grabber(i)
            new Thread(grabber).start()
        }
    }
}
