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
        runtime 'com.amazonaws:aws-java-sdk:1.10.0'
    }
    plugins {
        runtime(':karman-aws:0.6.1') {
            excludes 'com.amazonaws:aws-java-sdk'
        }
        runtime ':asset-pipeline:2.2.3'
        build(':release:3.0.1',
                ':rest-client-builder:1.0.3',
                ':coveralls:0.1.3') {
            export = false
        }
        test(':code-coverage:2.0.3-3') {
            export = false
        }

    }
}
