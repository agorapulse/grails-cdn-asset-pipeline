import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration
import com.amazonaws.services.s3.model.CORSRule

import static com.amazonaws.services.s3.model.CORSRule.AllowedMethods.*

includeTargets << new File("${cdnAssetPipelinePluginDir}/scripts/_AssetCdn.groovy")

USAGE = """
    asset-cdn-cors --origin=ORIGIN [--rule-id=RULE_ID] [--directory=DIRECTORY] [--region=REGION] [--access-key=ACCESS_KEY] [--secret-key=SECRET_KEY]

where
    ORIGIN          = Origin to allow.
                    Ex.: *.mydomain.com
                    (REQUIRED)

    RULE_ID         = Rule id
                    (default: GetRule)

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
"""

target(main: "Add a CORS GET rule for a given origin and a bucket") {
    loadConfig() // Load config and parse arguments

    // Parse parameter
    String origin = argsMap['origin'] ?: ''
    String ruleId = argsMap['rule-id'] ?: 'GetRule'
    if (!origin) {
        event("StatusError", ["Origin is a required argument, use 'grails help asset-cdn-cors' to show usage."])
        exit 1
    }
    if (!providerName) {
        event("StatusError", ["Provider is required, use 'grails help asset-cdn-cors' to show usage."])
        exit 1
    } else if (providerName != 'S3') {
        event("StatusError", ["Sorry, only S3 provider is supported."])
        exit 1
    }
    if (!directory) {
        event("StatusError", ["Directory is required, use 'grails help asset-cdn-cors' to show usage."])
        exit 1
    }
    if (!accessKey) {
        event("StatusError", ["Access key is required, use 'grails help asset-cdn-cors' to show usage."])
        exit 1
    }
    if (!secretKey) {
        event("StatusError", ["Secret key is required, use 'grails help asset-cdn-cors' to show usage."])
        exit 1
    }

    event("StatusUpdate", ["Checking existing CORS rules for directory ($directory)....."])
    loadProvider() // Load provider

    CORSRule rule
    List rules = []
    BucketCrossOriginConfiguration bucketConfig = provider.s3Client.getBucketCrossOriginConfiguration(directory)
    if (bucketConfig) {
        bucketConfig.rules.each {
            println "-- Rule ID: $it.id"
            //println "MaxAgeSeconds: $it.maxAgeSeconds"
            println "AllowedMethod: $it.allowedMethods"
            println "AllowedOrigins: $it.allowedOrigins"
            //println "AllowedHeaders: $it.allowedHeaders"
            //println "ExposeHeader: $it.exposedHeaders"
        }
        rules = bucketConfig.rules
        rule = rules.find { it.id == ruleId } //{ origin in it.allowedOrigins && GET in it.allowedMethods }
    } else {
        bucketConfig = new BucketCrossOriginConfiguration()
    }
    if (!rule) {
        event("StatusUpdate", ["Adding CORS GET rule....."])
        rules.add(new CORSRule()
                .withId(ruleId)
                .withAllowedMethods([GET])
                .withAllowedOrigins([origin]))
        bucketConfig.setRules(rules)
        provider.s3Client.setBucketCrossOriginConfiguration(directory, bucketConfig)
        event("StatusFinal", ["CORS GET rule successfully added for origin '$origin' (rule ID: $ruleId)"])
    } else {
        event("StatusFinal", ["CORS GET rule already exists for origin '$origin' (rule ID: $rule.id)"])
    }
}

setDefaultTarget(main)