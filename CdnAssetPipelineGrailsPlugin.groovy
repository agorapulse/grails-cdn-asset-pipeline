class CdnAssetPipelineGrailsPlugin {

    def version = "0.4.1"
    def grailsVersion = "2.0 > *"
    //def dependsOn = ['asset-pipeline': '1.5.0 > *']
    def loadAfter = ['asset-pipeline','karmanAws']

    def author = "Benoit Hediard"
    def authorEmail = "ben@benorama.com"
    def title = "CDN Asset Pipeline Plugin"
    def description = '''
Provides Gant scripts to automatically upload Grails app static assets to CDNs.
Those scripts can easily be integrated to a build pipeline for continuous delivery/deployment.
It uses Asset Pipeline Grails Plugin to precompile assets and Karman Grails Plugin to upload files to various Cloud Storage Services.
'''

    def documentation = "http://github.com/agorapulse/grails-cdn-asset-pipeline"
    def license = "APACHE"
    def organization = [ name: "AgoraPulse", url: "http://www.agorapulse.com/" ]
    def issueManagement = [ system: "github", url: "https://github.com/agorapulse/grails-cdn-asset-pipeline" ]
    def scm = [  url: "https://github.com/agorapulse/grails-cdn-asset-pipeline" ]
    def developers      = [ [name: 'Benoit Hediard'], [name: 'David Estes']  ]
}
