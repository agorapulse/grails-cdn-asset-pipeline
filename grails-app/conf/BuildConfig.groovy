grails.project.work.dir = 'target'
grails.project.source.level = 1.6

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {
    inherits 'global'
    log 'warn'
    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
        mavenRepo 'http://dl.bintray.com/karman/karman'
    }
    dependencies {
    }
    plugins {
        build ':tomcat:7.0.52.1'
        build(':release:3.0.1') {
            export = false
        }

        runtime ':karman-aws:0.4.2'
        runtime ':asset-pipeline:1.8.7'
    }
}
