import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration
import com.amazonaws.services.s3.model.CORSRule

import static com.amazonaws.services.s3.model.CORSRule.AllowedMethods.*

includeTargets << new File("${s3AssetPipelinePluginDir}/scripts/_AssetS3.groovy")

USAGE = """
    asset-s3-bucket-allow --origin=ORIGIN --rule-id=RULE_ID [--bucket=BUCKET] [--region=REGION] [--access-key=ACCESS_KEY] [--secret-key=SECRET_KEY]

where
    --origin        = Origin to allow.
                    Ex.: *.mydomain.com
                    (REQUIRED)

    --rule-id       = Rule id
                    (default: GetRule)

    ACCESS_KEY      = AWS access key.
                    (default: grails.assets.s3.accessKey
                        or grails.plugin.awssdk.s3.accessKey
                        or grails.plugin.awssdk.accessKey)

    SECRET_KEY      = AWS secret key.
                    (default: grails.assets.s3.secretKey
                        or grails.plugin.awssdk.s3.secretKey
                        or grails.plugin.awssdk.secretKey)

    BUCKET          = S3 bucket name.
                    (default: grails.assets.s3.bucket
                        or grails.plugin.awssdk.s3.bucket
                        or grails.plugin.awssdk.bucket)

    REGION          = S3 bucket region.
                    (default: grails.assets.s3.region
                        or grails.plugin.awssdk.s3.region
                        or grails.plugin.awssdk.region)
"""

target(main: "Add a CORS GET rule for a given origin and an AWS S3 bucket") {
    loadConfig() // Load config and parse arguments

    // Parse parameter
    String origin = argsMap['origin'] ?: ''
    String ruleId = argsMap['rule-id'] ?: 'GetRule'
    if (!origin) {
        event("StatusError", ["Origin is a required argument, use 'grails help asset-s3-bucket-allow' to show usage."])
        exit 1
    }

    if (!bucket) {
        event("StatusError", ["Bucket is a required argument, use 'grails help asset-s3-bucket-allow' to show usage."])
        exit 1
    }

    event("StatusUpdate", ["Checking existing CORS rules for S3 bucket ($bucket)....."])
    loadS3Client()

    CORSRule rule
    List rules = []
    BucketCrossOriginConfiguration bucketConfig = s3Client.getBucketCrossOriginConfiguration(bucket)
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
        s3Client.setBucketCrossOriginConfiguration(bucket, bucketConfig)
        event("StatusFinal", ["CORS GET rule successfully added for origin '$origin' (rule ID: $rule.id)"])
    } else {
        event("StatusFinal", ["CORS GET rule already exists for origin '$origin' (rule ID: $rule.id)"])
    }
}

setDefaultTarget(main)