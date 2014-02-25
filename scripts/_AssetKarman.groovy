import com.bertramlabs.plugins.karman.aws.S3StorageProvider

includeTargets << grailsScript("_GrailsInit")
includeTargets << new File(assetPipelinePluginDir, "scripts/_AssetCompile.groovy")

target(loadConfig: "Load Karman assets config") {
    depends(compile, parseArguments)

    if (argsMap['help']) {
        println USAGE
        exit 0
    }

    loadApp()
    configureApp()

    def awsConfig = grailsApp.config.grails.plugin?.awssdk
    def karmanAssetsConfig = grailsApp.config.grails.assets?.karman

    // Parse arguments
    providerName = argsMap['provider'] ?: karmanAssetsConfig?.provider
    directory = argsMap['directory'] ?: karmanAssetsConfig?.directory ?: awsConfig?.s3?.bucket ?: awsConfig?.bucket
    accessKey = argsMap['access-key'] ?: karmanAssetsConfig?.accessKey ?: awsConfig?.s3?.accessKey ?: awsConfig?.accessKey
    secretKey = argsMap['secret-key'] ?: karmanAssetsConfig?.secretKey ?: awsConfig?.s3?.secretKey ?: awsConfig?.secretKey
    region = argsMap['region'] ?: karmanAssetsConfig?.region ?: awsConfig?.s3?.region ?: awsConfig?.region ?: ''

    // Global expirationDate var
    expirationDate = null
    def expires = argsMap['expires'] ?: karmanAssetsConfig?.expires ?: awsConfig?.s3?.expires ?: 0
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
    prefix = argsMap['prefix'] ?: karmanAssetsConfig.prefix ?: ''
    if (!prefix.endsWith('/')) prefix = "$prefix/"
    if (prefix.startsWith('/')) prefix = prefix.replaceFirst('/', '')
}

target(loadProvider: "Load Karman provider") {
    depends(loadConfig)

    // Load provider
    try {
        provider = Class.forName("com.bertramlabs.plugins.karman.${providerName == 'S3' ? 'aws' : providerName.toLowerCase()}.${providerName}StorageProvider", false, Thread.currentThread().contextClassLoader).newInstance(
                accessKey: accessKey,
                secretKey: secretKey,
                region: region
        )
    } catch (ClassNotFoundException exception) {
        event("StatusError", ["Provider class not found: ${exception.message}."])
        exit 1
    }
}