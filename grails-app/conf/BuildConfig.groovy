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
    }
    dependencies {
    }
    plugins {
        build(":release:3.0.1") {
            export = false
        }
        compile ':aws-sdk:1.7.1'
        compile ':asset-pipeline:1.5.7'
    }
}