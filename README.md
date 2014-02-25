
Karman Asset Pipeline Grails Plugin
===============================

# Introduction

The Karman Plugin allows you to push [Grails](http://grails.org) app assets to Cloud Storage Services, in order to use a CDN to serve all your [Grails](http://grails.org) app static assets:

- *great for your users*: faster browser page rendering thanks to CDN,
- *great for your servers*: less static requests to handle = increased load capabilities.

Undercover, it uses [Asset Pipeline](http://grails.org/plugin/asset-pipeline) Grails Plugin to precompile assets and [Karman](http://grails.org/plugin/karman) Grails Plugin to upload files to CDNs.

It adds two new [Grails](http://grails.org) CLI scripts:

- *asset-karman-push* to upload assets to a bucket,
- *asset-karman-directory-cors* to add a CORS GetRule to a directory (ex.: S3bucket).

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
				runtime ':karman-asset-pipeline:0.1'
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
        karman {
            provider = 'S3' // Karman provider
            directory = 'my-bucket'
            accessKey = '{MY_S3_ACCESS_KEY}'
            secretKey = '{MY_S3_SECRET_KEY}'
            prefix = "assets/${appName}-${appVersion}/" // This is just a prefix example
            expires = 365 // Expires in 1 year (value in days)
        }
    }
}
```

**prefix** config param is not required, but it is useful to version your assets automatically, so that you don't have to handle cache invalidation.

You should set a pretty big **expires** value (to add **Cache-Control** and **Expires** metadata), so that browsers cache assets locally.

Note: for S3 provider, never use your AWS root user access keys, you should create a specific IAM user with the corresponding S3 bucket permissions.


# Usage

## Pushing your assets to S3 bucket

Add this command to your build process (usually before war generation and deployment).

```groovy
// If all the settings are defined in your Config.groovy
grails asset-karman-push
// Or
grails asset-karman-push --provider=S3 --directory=my-bucket --prefix=some-prefix --expires=365 --region=eu-west-1 --access-key=$MY_S3_ACCESS_KEY --secret-key=$MY_S3_SECRET_KEY
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
grails asset-karman-directory-cors --origin=*.mydomain.com
// Or
grails asset-karman-directory-cors --origin=*.mydomain.com --provider=S3 --directory=my-bucket --region=eu-west-1 --access-key=$MY_S3_ACCESS_KEY --secret-key=$MY_S3_SECRET_KEY
```

# Latest releases

Coming soon...

# Bugs

To report any bug, please use the project [Issues](http://github.com/agorapulse/karman-asset-pipeline/issues) section on GitHub.