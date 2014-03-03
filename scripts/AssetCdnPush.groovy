import com.amazonaws.services.s3.Headers
import groovy.io.FileType

includeTargets << new File("${cdnAssetPipelinePluginDir}/scripts/_AssetCdn.groovy")

USAGE = """
    asset-cdn-push [--provider=PROVIDER] [--directory=DIRECTORY] [--access-key=ACCESS_KEY] [--secret-key=SECRET_KEY] [--prefix=PREFIX] [--expires=EXPIRES] [--region=REGION]

where
    PROVIDER        = Provider name (ex.: S3).
                    (default: grails.assets.karman.provider)

    DIRECTORY       = Directory name (ex.: S3 bucket name).
                    (default: grails.assets.karman.directory)

    REGION          = Directory region (ex.: S3 bucket region).
                    (default: grails.assets.karman.region)

    ACCESS_KEY      = Provider access key.
                    (default: grails.assets.karman.accessKey)

    SECRET_KEY      = Provider secret key.
                    (default: grails.assets.karman.secretKey)

    EXPIRES         = Expires value in days (to add 'Cache-Control' and 'Expires' metadata).
                    (default: grails.assets.karman.expires)

    PREFIX          = Files key prefix
                    (default: grails.assets.karman.prefix)
"""

target(main: "Upload static assets to CDN") {
    loadConfig() // Load config and parse arguments

    if (!providerName) {
        event("StatusError", ["Provider is required, use 'grails help asset-cdn-push' to show usage."])
        exit 1
    }
    if (!directory) {
        event("StatusError", ["Directory is required, use 'grails help asset-cdn-push' to show usage."])
        exit 1
    }
    if (!accessKey) {
        event("StatusError", ["Access key is required, use 'grails help asset-cdn-push' to show usage."])
        exit 1
    }
    if (!secretKey) {
        event("StatusError", ["Secret key is required, use 'grails help asset-cdn-push' to show usage."])
        exit 1
    }


    if (expirationDate) {
        event("StatusUpdate", ["Expiration date set to $expirationDate"])
    }

    assetCompile() // Compile assets
    loadProvider() // Load provider

    int uploadCount = 0

    // Push resources to directory
    def assetPath = 'target/assets'
    def assetDir = new File(assetPath)
    if (!assetDir.exists()) {
        event("StatusError", ["Could not push assets, target/assets directory not found"])
    } else {
        List list = []
        assetDir.eachFileRecurse (FileType.FILES) { file ->
            list << file
        }

        int total = list.size()
        list.eachWithIndex { File file, index ->
            String name = prefix + file.path.replace("${assetPath}/", '')
            event("StatusUpdate", ["Uploading File ${index} of ${total} -  $name"])
            def cloudFile = provider[directory][name]
            if (expirationDate) {
                cloudFile.setMetaAttribute("Cache-Control", "PUBLIC, max-age=${(expirationDate.time / 1000).toInteger()}, must-revalidate")
                cloudFile.setMetaAttribute("Expires", expirationDate)
            }
            // Specify some content types for extension not handled by URLConnection.guessContentType
            Map contentTypes = [
                    css: 'text/css',
                    gz: 'application/x-compressed',
                    js: 'application/javascript',
                    pdf: 'application/pdf',
                    eot: 'application/vnd.ms-fontobject',
                    otf: 'font/opentype',
                    svg: 'image/svg+xml',
                    ttf: 'application/x-font-ttf',
                    woff: 'application/x-font-woff'
            ]
            String extension = file.name.tokenize('.').last()
            if (contentTypes[extension]) {
                cloudFile.contentType = contentTypes[extension]
            } else {
                cloudFile.contentType = URLConnection.guessContentTypeFromName(file.name)
            }
            cloudFile.bytes = file.bytes
            // Upload file
            cloudFile.save('public-read')
            uploadCount++
        }
    }

    event("StatusFinal", ["Assets push complete: $uploadCount assets uploaded to directory '$directory'"])
}

setDefaultTarget(main)