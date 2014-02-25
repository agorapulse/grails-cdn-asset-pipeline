import com.amazonaws.services.s3.Headers
import groovy.io.FileType

import java.text.DateFormat
import java.text.SimpleDateFormat

includeTargets << new File("${karmanAssetPipelinePluginDir}/scripts/_AssetKarman.groovy")

USAGE = """
    asset-karman-push [--provider=PROVIDER] [--directory=DIRECTORY] [--access-key=ACCESS_KEY] [--secret-key=SECRET_KEY] [--prefix=PREFIX] [--expires=EXPIRES] [--region=REGION]

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

target(assetKarmanPush: "Upload static assets to Karman directory") {
    loadConfig() // Load config and parse arguments

    if (!providerName) {
        event("StatusError", ["Provider is required, use 'grails help asset-karman-push' to show usage."])
        exit 1
    }
    if (!directory) {
        event("StatusError", ["Directory is required, use 'grails help asset-karman-push' to show usage."])
        exit 1
    }
    if (!accessKey) {
        event("StatusError", ["Access key is required, use 'grails help asset-karman-push' to show usage."])
        exit 1
    }
    if (!secretKey) {
        event("StatusError", ["Secret key is required, use 'grails help asset-karman-push' to show usage."])
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
            println "Uploading File ${index} of ${total} -  $name"
            def cloudFile = provider[directory][name]
            if (expirationDate) {
                DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                cloudFile.setMetaAttribute(Headers.CACHE_CONTROL, "PUBLIC, max-age=${(expirationDate.time / 1000).toInteger()}, must-revalidate")
                cloudFile.setMetaAttribute(Headers.EXPIRES, httpDateFormat.format(expirationDate))
            }
            // Specify content type for web fonts
            Map contentTypes = [
                    eot: 'application/vnd.ms-fontobject',
                    otf: 'font/opentype',
                    svg: 'image/svg+xml',
                    ttf: 'application/x-font-ttf',
                    woff: 'application/x-font-woff'
            ]
            String extension = file.name.tokenize('.').last()
            if (contentTypes[extension]) {
                cloudFile.contentType = contentTypes[extension]
            }
            cloudFile.bytes = file.bytes
            // Upload file
            cloudFile.save('public-read')
            uploadCount++
        }
    }

    event("StatusFinal", ["Assets push complete: $uploadCount assets uploaded to directory '$directory'"])
}

setDefaultTarget(assetKarmanPush)

// PRIVATE

/*private ObjectMetadata buildMetaData(String extension, Date expirationDate) {
    def metaData = new ObjectMetadata()
    if (expirationDate) {
        DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        metaData.setHeader("Cache-Control", "PUBLIC, max-age=${(expirationDate.time / 1000).toInteger()}, must-revalidate")
        metaData.setHeader("Expires", httpDateFormat.format(expirationDate))
    }
    // Specify content type for web fonts
    switch(extension) {
        case 'eot':
            metaData.setContentType('application/vnd.ms-fontobject')
            break
        case 'otf':
            metaData.setContentType('font/opentype')
            break
        case 'svg':
            metaData.setContentType('image/svg+xml')
            break
        case 'ttf':
            metaData.setContentType('application/x-font-ttf')
            break
        case 'woff':
            metaData.setContentType('application/x-font-woff')
            break
    }
    return metaData
}*/