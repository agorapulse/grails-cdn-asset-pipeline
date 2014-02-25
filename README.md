
Karman Asset Pipeline Grails Plugin
===============================

# Introduction

The Karman Asset Pipeline Plugin provides Gant scripts to be able to automatically upload [Grails](http://grails.org) app static assets to CDNs.
Those scripts can easily be integrated to a build pipeline for continuous delivery/deployment.

You should always use a CDN to host all your app static assets:

- *great for your users*: faster browser page rendering thanks to CDN,
- *great for your servers*: less static requests to handle = increased load capabilities.

Undercover, it uses [Asset Pipeline](http://grails.org/plugin/asset-pipeline) Grails Plugin to precompile assets and [Karman](http://grails.org/plugin/karman) Grails Plugin to upload files to various Cloud Storage Services.

It adds two [Grails](http://grails.org) Gant scripts:

- *asset-karman-push* to upload assets to a CDN directory/bucket,
- *asset-karman-directory-cors* to add a CORS GetRule to a directory/bucket.

Note: for this initial release, only *S3* provider is supported.

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

## Pushing your assets to a Cloud Storage Service

Add this command to your build process (usually before war generation and deployment).

```groovy
// If all the settings are defined in your Config.groovy
grails asset-karman-push
// Or
grails asset-karman-push --provider=S3 --directory=my-bucket --prefix=some-prefix --expires=365 --region=eu-west-1 --access-key=$MY_S3_ACCESS_KEY --secret-key=$MY_S3_SECRET_KEY
```

## Allowing your domain with a CORS rule

When you create your bucket on S3, you might need to add a [CORS rule](http://docs.aws.amazon.com/AmazonS3/latest/dev/cors.html) (Cross-Origin Resource Sharing).

Here is a command that will automatically do it for you!

```groovy
// If all the settings are defined in your Config.groovy
grails asset-karman-directory-cors --origin=*.mydomain.com
// Or
grails asset-karman-directory-cors --origin=*.mydomain.com --provider=S3 --directory=my-bucket --region=eu-west-1 --access-key=$MY_S3_ACCESS_KEY --secret-key=$MY_S3_SECRET_KEY
```

## Using your CDN-based assets

In your [Asset Pipeline](http://grails.org/plugin/asset-pipeline) config, add your CDN URL (including your app prefix)

```groovy
grails.assets.url = "https://s3.amazonaws.com/my-bucket/assets/${appName}-${appVersion}"
```

# Latest releases

Coming soon...

# Bugs

To report any bug, please use the project [Issues](http://github.com/agorapulse/karman-asset-pipeline/issues) section on GitHub.