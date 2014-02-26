includeTargets << grailsScript("_GrailsInit")
includeTargets << new File(assetPipelinePluginDir, "scripts/_AssetCompile.groovy")

target(loadConfig: "Load CDN assets config") {
    depends(compile, parseArguments)

    if (argsMap['help']) {
        println USAGE
        exit 0
    }

    loadApp()
    configureApp()

    def cdnAssetsConfig = grailsApp.config.grails.assets?.cdn

    // Parse arguments
    providerName = argsMap['provider'] ?: cdnAssetsConfig?.provider
    directory = argsMap['directory'] ?: cdnAssetsConfig?.directory
    accessKey = argsMap['access-key'] ?: cdnAssetsConfig?.accessKey
    secretKey = argsMap['secret-key'] ?: cdnAssetsConfig?.secretKey
    region = argsMap['region'] ?: cdnAssetsConfig?.region

    if (providerName == 'S3') {
        def awsConfig = grailsApp.config.grails.plugin?.awssdk
        if (!directory) directory = awsConfig?.s3?.bucket ?: awsConfig?.bucket
        if (!accessKey) accessKey = awsConfig?.s3?.accessKey ?: awsConfig?.accessKey
        if (!secretKey) secretKey = awsConfig?.s3?.secretKey ?: awsConfig?.secretKey
        if (!region) region = awsConfig?.s3?.region ?: awsConfig?.region ?: ''
    }

    // Global expirationDate var
    expirationDate = null
    def expires = argsMap['expires'] ?: cdnAssetsConfig?.expires ?: 0
    if (expires) {
        if (expires instanceof Date) {
            expirationDate = expires
        } else if (expires instanceof Integer) {
            expirationDate = new Date() + expires
        } else if (expires instanceof String && expires.isNumber()) {
            expirationDate = new Date() + expires.toInteger()
        }
    }

    // Global prefix var
    prefix = argsMap['prefix'] ?: cdnAssetsConfig.prefix ?: ''
    if (!prefix.endsWith('/')) prefix = "$prefix/"
    if (prefix.startsWith('/')) prefix = prefix.replaceFirst('/', '')
}

target(loadProvider: "Load Karman provider") {
    depends(loadConfig)

    // Load provider
    try {
        String className = "com.bertramlabs.plugins.karman.${providerName == 'S3' ? 'aws' : providerName.toLowerCase()}.${providerName}StorageProvider"
        provider = Class.forName(className, false, Thread.currentThread().contextClassLoader).newInstance(
                accessKey: accessKey,
                secretKey: secretKey,
                region: region
        )
    } catch (ClassNotFoundException exception) {
        event("StatusError", ["Provider class not found: ${exception.message}."])
        exit 1
    }
}