import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import groovy.io.FileType

import java.text.DateFormat
import java.text.SimpleDateFormat

includeTargets << new File("${s3AssetPipelinePluginDir}/scripts/_AssetS3.groovy")

USAGE = """
    asset-s3-push [--bucket=BUCKET] [--expires=EXPIRES] [--region=REGION] [--access-key=ACCESS_KEY] [--secret-key=SECRET_KEY] [--prefix=PREFIX]

where
    BUCKET          = S3 bucket name.
                    (default: grails.assets.s3.bucket
                        or grails.plugin.awssdk.s3.bucket
                        or grails.plugin.awssdk.bucket)

    REGION          = S3 bucket region.
                    (default: grails.assets.s3.region
                        or grails.plugin.awssdk.s3.region
                        or grails.plugin.awssdk.region)

    ACCESS_KEY      = AWS access key.
                    (default: grails.assets.s3.accessKey
                        or grails.plugin.awssdk.s3.accessKey
                        or grails.plugin.awssdk.accessKey)

    SECRET_KEY      = AWS secret key.
                    (default: grails.assets.s3.secretKey
                        or grails.plugin.awssdk.s3.secretKey
                        or grails.plugin.awssdk.secretKey)

    EXPIRES         = Expires value in days (to add 'Cache-Control' and 'Expires' metadata).
                    (default: grails.assets.s3.expires)

    PREFIX          = S3 key prefix
                    (default: grails.assets.s3.prefix)
"""

target(assetS3Push: "Upload static assets to an AWS S3 bucket") {
    loadConfig() // Load config and parse arguments

    if (!accessKey) {
        event("StatusError", ["Access key is required, use 'grails help asset-s3-push' to show usage."])
        exit 1
    }
    if (!secretKey) {
        event("StatusError", ["Secret key is required, use 'grails help asset-s3-push' to show usage."])
        exit 1
    }
    if (!bucket) {
        event("StatusError", ["Bucket is required, use 'grails help asset-s3-push' to show usage."])
        exit 1
    }

    recursive = true
    update = argsMap['update'] ? true : false

    if (expirationDate) {
        event("StatusUpdate", ["Expiration date set to $expirationDate"])
    }

    loadS3Client() // Load s3Client
    assetCompile() // Compile assets

    int uploadCount = 0

    // Push resources to bucket
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
            String s3Key = prefix + file.path.replace("${assetPath}/", '')
            event("StatusUpdate", ["Uploading File ${index} of ${total} -  $s3Key"])
            s3Client.putObject(
                    new PutObjectRequest(bucket, s3Key, file)
                            .withCannedAcl(CannedAccessControlList.PublicRead)
                            .withMetadata(buildMetaData(file.name.tokenize('.').last(), expirationDate))
            )
            uploadCount++
        }
    }

    event("StatusFinal", ["Assets push to S3 complete: $uploadCount assets uploaded to bucket '$bucket'"])
}

setDefaultTarget(assetS3Push)

// PRIVATE

private ObjectMetadata buildMetaData(String extension, Date expirationDate) {
    def metaData = new ObjectMetadata()
    if (expirationDate) {
        DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        metaData.setHeader("Cache-Control", "PUBLIC, max-age=${(expirationDate.time / 1000).toInteger()}, must-revalidate")
        metaData.setHeader("Expires", httpDateFormat.format(expirationDate))
        println httpDateFormat.format(expirationDate)
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
}