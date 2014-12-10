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
        mavenRepo 'http://dl.bintray.com/bertramlabs/karman/'
    }
    dependencies {
        // Latest httpcore and httpmime for Coveralls plugin
        build 'org.apache.httpcomponents:httpcore:4.3.2'
        build 'org.apache.httpcomponents:httpclient:4.3.2'
        build 'org.apache.httpcomponents:httpmime:4.3.3'
        // Latest karman-aws-groovy
        runtime 'com.bertramlabs.plugins:karman-aws-groovy:0.6.0'
    }
    plugins {
        runtime(':karman-aws:0.5.1') {
            excludes 'com.bertramlabs.plugins:karman-aws-groovy'
        }
        runtime ':asset-pipeline:2.0.17'
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
