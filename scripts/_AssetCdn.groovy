includeTargets << grailsScript("_GrailsInit")
includeTargets << new File(karmanPluginDir, "scripts/_InitKarman.groovy")
includeTargets << new File(assetPipelinePluginDir, "scripts/_AssetCompile.groovy")

target(loadConfig: "Load CDN assets config") {
    depends(compile, parseArguments)
    

    if (argsMap['help']) {
        println USAGE
        exit 0
    }

    loadApp()
    configureApp()
    initKarman()
    providers = []

    def cdnAssetsConfig = grailsApp.config.grails.assets?.cdn

    providers = cdnAssetsConfig.providers ?: []

    if(providers.size() == 0) {
        def providerObject = [:]
        providerObject.provider = argsMap['provider'] ?: cdnAssetsConfig?.provider
        providerObject.directory = argsMap['directory'] ?: cdnAssetsConfig?.directory
        providerObject.accessKey = argsMap['access-key'] ?: cdnAssetsConfig?.accessKey
        providerObject.secretKey = argsMap['secret-key'] ?: cdnAssetsConfig?.secretKey
        providerObject.region = argsMap['region'] ?: cdnAssetsConfig?.region
        providers << providerObject
    }

    def prefix = argsMap['prefix'] ?: cdnAssetsConfig.prefix
    providers.each { provider ->
        if(provider.provider?.toLowerCase() == 's3')
        def awsConfig = grailsApp.config.grails.plugin?.awssdk
        if (!provider.directory) provider.directory = awsConfig?.s3?.bucket ?: awsConfig?.bucket
        if (!provider.accessKey) provider.accessKey = awsConfig?.s3?.accessKey ?: awsConfig?.accessKey
        if (!provider.secretKey) provider.secretKey = awsConfig?.s3?.secretKey ?: awsConfig?.secretKey
        if (!provider.region)    provider.region = awsConfig?.s3?.region ?: awsConfig?.region ?: ''
        if(!provider.storagePath) provider.storagePath = prefix
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

}
