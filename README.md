
S3 Asset Pipeline Grails Plugin
===============================

# Introduction

The S3 Plugin allows you to push [Grails](http://grails.org) app assets to [Amazon S3](aws.amazon.com/s3/), in order to use a CDN to serve all your [Grails](http://grails.org) app static assets:

- *great for your users*: faster browser page rendering thanks to CDN,
- *great for your servers*: less static requests to handle = increased load capabilities.

Undercover, it uses [Asset Pipeline](http://grails.org/plugin/asset-pipeline) Grails Plugin to precompile assets and [AWS SDK](http://grails.org/plugin/aws-sdk) Grails Plugin to upload files to [Amazon S3](aws.amazon.com/s3/).

It adds two new [Grails](http://grails.org) CLI scripts:

- *asset-s3-push* to upload assets to a bucket,
- *asset-s3-bucket-allow* to add a CORS GetRule to a bucket.

# Installation

Declare the plugin dependency in the BuildConfig.groovvy file, as shown here:

```groovy
grails.project.dependency.resolution = {
		inherits("global") { }
		log "info"
		repositories {
                //your repositories
        }
        dependencies {
                //your dependencies
        }
		plugins {
				//here go your plugin dependencies
				runtime ':s3-asset-pipeline:0.1'
		}
}
```


# Config

You can add your config in **Config.groovy** but it is not required, all parameters can be passed as arguments to *asset-s3-push*.

```groovy
def appName = grails.util.Metadata.current.'app.name'
def appVersion = grails.util.Metadata.current.'app.version'

grails {
    assets {
        s3 {
            accessKey = '{MY_S3_ACCESS_KEY}'
            secretKey = '{MY_S3_SECRET_KEY}'
            bucket = 'my-bucket'
            prefix = "assets/${appName}-${appVersion}/" // This is just a prefix example
            expires = 365 // Expires in 1 year (value in days)
        }
    }
}
```

**prefix** config param is not required, but it is useful to version your assets automatically.

You should set a pretty big **expires** value (to add **Cache-Control** and **Expires** metadata), so that browsers cache assets locally.

Note: never use your AWS root user access keys, you should create a specific IAM user with the corresponding S3 bucket permissions.


# Usage

## Pushing your assets to S3 bucket

Add this command to your build process (usually before war generation and deployment).

```groovy
// If all the settings are defined in your Config.groovy
grails asset-s3-push
// Or
grails asset-s3-push --prefix=some-prefix --expires=365 --bucket=my-bucket --region=eu-west-1 --access-key=$MY_S3_ACCESS_KEY --secret-key=$MY_S3_SECRET_KEY
```

Then, in your [Asset Pipeline](http://grails.org/plugin/asset-pipeline) config, define the assets S3 URL or CloudFront CDN URL

```groovy
grails.assets.url = "https://s3.amazonaws.com/my-bucket/assets/${appName}-${appVersion}"
```

## Allowing your domain with a CORS bucket rule

When you create your bucket on S3, you might need to add a [CORS rule](http://docs.aws.amazon.com/AmazonS3/latest/dev/cors.html) (Cross-Origin Resource Sharing).

Here is a command that will automatically do it for you!

```groovy
// If all the settings are defined in your Config.groovy
grails asset-s3-bucket-allow --origin=*.mydomain.com
// Or
grails asset-s3-bucket-allow --origin=*.mydomain.com --bucket=my-bucket --region=eu-west-1 --access-key=$MY_S3_ACCESS_KEY --secret-key=$MY_S3_SECRET_KEY
```

# Latest releases

Coming soon...

# Bugs

To report any bug, please use the project [Issues](http://github.com/agorapulse/grails-s3-asset-pipeline/issues) section on GitHub.