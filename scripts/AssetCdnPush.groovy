includeTargets << new File("${cdnAssetPipelinePluginDir}/scripts/_AssetCdn.groovy")

USAGE = """
    asset-cdn-push [--provider=PROVIDER] [--directory=DIRECTORY] [--region=REGION] [--access-key=ACCESS_KEY] [--secret-key=SECRET_KEY] [--expires=EXPIRES] [--prefix=PREFIX] [--gzip=GZIP]

where
    PROVIDER        = Provider name (ex.: S3).
                    (default: grails.assets.cdn.provider)

    DIRECTORY       = Directory name (ex.: S3 bucket name).
                    (default: grails.assets.cdn.directory)

    REGION          = Directory region (ex.: S3 bucket region).
                    (default: grails.assets.cdn.region)

    ACCESS_KEY      = Provider access key.
                    (default: grails.assets.cdn.accessKey)

    SECRET_KEY      = Provider secret key.
                    (default: grails.assets.cdn.secretKey)

    EXPIRES         = Expires value in days (to add 'Cache-Control' and 'Expires' metadata).
                    (default: grails.assets.cdn.expires)

    PREFIX          = Files key prefix
                    (default: grails.assets.cdn.prefix)

    GZIP           = Files gzip mode (default to 'false', 'true' to upload gzip version or 'both' to keep original file + .gz version)
                    (default: grails.assets.cdn.gzip)
"""

target(main: "Upload static assets to CDN") {
    loadConfig() // Load config and parse arguments

    if (!providers) {
        event("StatusError", ["Provider is required, use 'grails help asset-cdn-push' to show usage."])
        exit 1
    }
    if (!providers.first().provider) {
        event("StatusError", ["Provider is required, use 'grails help asset-cdn-push' to show usage."])
        exit 1
    }
    if (!providers.first().directory) {
        event("StatusError", ["Directory is required, use 'grails help asset-cdn-push' to show usage."])
        exit 1
    }
    if (!providers.first().accessKey) {
        event("StatusError", ["Access key is required, use 'grails help asset-cdn-push' to show usage."])
        exit 1
    }
    if (!providers.first().secretKey) {
        event("StatusError", ["Secret key is required, use 'grails help asset-cdn-push' to show usage."])
        exit 1
    }

    if (expirationDate) {
        event("StatusUpdate", ["Expiration date set to $expirationDate"])
    }

    assetCompile() // Compile assets
    def assetSyncClass = classLoader.loadClass('asset.pipeline.cdn.AssetSync')
    def options = [
            expirationDate: expirationDate,
            gzip: gzip.toString(),
            providers: providers
    ]
    def assetSync = assetSyncClass.newInstance(options, eventListener)

    // Push resources to directory
    assetSync.sync()

    event("StatusFinal", ["Assets push complete!"])
}

setDefaultTarget(main)