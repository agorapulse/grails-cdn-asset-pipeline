import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.model.ObjectListing

includeTargets << grailsScript("_GrailsInit")
includeTargets << new File(assetPipelinePluginDir, "scripts/_AssetCompile.groovy")

target(loadConfig: "Load S3 resources config") {
    depends(compile, parseArguments)

    if (argsMap['help']) {
        println USAGE
        exit 0
    }

    loadApp()
    configureApp()

    awsConfig = grailsApp.config.grails.plugin?.awssdk
    s3AssetsConfig = grailsApp.config.grails.assets?.s3

    // Parse arguments
    bucket = argsMap['bucket'] ?: s3AssetsConfig?.bucket ?: awsConfig?.s3?.bucket ?: awsConfig?.bucket
    accessKey = argsMap['access-key'] ?: s3AssetsConfig?.accessKey ?: awsConfig?.s3?.accessKey ?: awsConfig?.accessKey
    secretKey = argsMap['secret-key'] ?: s3AssetsConfig?.secretKey ?: awsConfig?.s3?.secretKey ?: awsConfig?.secretKey
    recursive = argsMap['all'] ? true : false // Only used in status (forced to true in push)
    region = argsMap['region'] ?: s3AssetsConfig?.region ?: awsConfig?.s3?.region ?: awsConfig?.region ?: ''

    expirationDate = null
    def expires = argsMap['expires'] ?: s3AssetsConfig?.expires ?: awsConfig?.s3?.expires ?: 0
    if (expires) {
        if (expires instanceof Date) {
            expirationDate = expires
        } else if (expires instanceof Integer) {
            expirationDate = new Date() + expires
        }
    }

    prefix = argsMap['prefix'] ?: s3AssetsConfig.prefix ?: ''
    if (!prefix.endsWith('/')) prefix = "$prefix/"
    if (prefix.startsWith('/')) prefix = prefix.replaceFirst('/', '')

}

target(loadS3Client: "Load S3 Amazon Web Service") {
    depends(loadConfig)

    def amazonWebService = appCtx.getBean('amazonWebService')
    if (accessKey && secretKey) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey)
        s3Client = new AmazonS3Client(credentials)
        if (region == 'us' || region == 'us-east-1') {
            s3Client.endpoint = "s3.amazonaws.com"
        } else {
            s3Client.endpoint = "s3-${region}.amazonaws.com"
        }
    } else {
        s3Client = region ? amazonWebService.getS3(region) : amazonWebService.s3
    }
}