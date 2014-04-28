
CDN Asset Pipeline Grails Plugin
===============================

# Introduction

The CDN Asset Pipeline Plugin provides Gant scripts to automatically upload [Grails](http://grails.org) app static assets to CDNs.
Those scripts can easily be integrated to a build pipeline for continuous delivery/deployment.

You should always use a CDN to host all your app static assets:

- **great for your users**: faster browser page rendering thanks to CDN,
- **great for your servers**: less static requests to handle = increased load capabilities.

Undercover, it uses [Asset Pipeline](http://grails.org/plugin/asset-pipeline) Grails Plugin to precompile assets and [Karman](http://grails.org/plugin/karman) Grails Plugin to upload files to various Cloud Storage Services.

It adds two [Grails](http://grails.org) Gant scripts:

- *asset-cdn-push* to upload assets to a CDN directory/bucket,
- *asset-cdn-cors* to add a CORS GetRule to a directory/bucket.

Kudos to *David Estes* for [Asset Pipeline](http://grails.org/plugin/asset-pipeline) and [Karman](http://grails.org/plugin/karman) Grails plugins as well as his feedback on this one!

Note: for this initial release, only *S3* provider is supported.

# Installation

Declare the plugin dependency in the BuildConfig.groovvy file, as shown here:

```groovy
grails.project.dependency.resolution = {
		inherits("global") { }
		log "info"
		repositories {
                //your repositories
                mavenRepo 'http://dl.bintray.com/karman/karman'
        }
        dependencies {
                //your dependencies
        }
		plugins {
				//here go your plugin dependencies
				compile ':cdn-asset-pipeline:0.3.3'
		}
}
```


# Config

You can add your config in **Config.groovy** but it is not required, all parameters can be passed as arguments to *asset-cdn-push*.

```groovy
def appName = grails.util.Metadata.current.'app.name'
def appVersion = grails.util.Metadata.current.'app.version'

// Single provider
grails {
    assets {
        cdn {
            provider = 's3' // Karman provider
            directory = 'my-bucket'
            accessKey = '{MY_S3_ACCESS_KEY}'
            secretKey = '{MY_S3_SECRET_KEY}'
            storagePath = "assets/${appName}-${appVersion}/" // This is just a prefix example
            expires = 365 // Expires in 1 year (value in days)
            gzip = true
        }
    }
}

// Or multiple providers
grails {
    assets {
        cdn {
            providers = [
                [
                    provider: 's3',
                    directory: 'my-s3-bucket',
                    accessKey: '{MY_S3_ACCESS_KEY}',
                    secretKey: '{MY_S3_SECRET_KEY}',
                    storagePath: "assets/${appName}-${appVersion}/", // This is just a prefix example
                    expires: 365 // Expires in 1 year (value in days)
                ],
                [
                    provider: 'gae', // Fictive provider
                    directory: 'my-gae-bucket',
                    accessKey: '{MY_GAE_ACCESS_KEY}',
                    secretKey: '{MY_GAE_SECRET_KEY}',
                    storagePath: "assets/${appName}-${appVersion}/", // This is just a prefix example
                    expires: 365 // Expires in 1 year (value in days)
                ]
            ]
        }
    }
}
```

**storagePath** config param is not required, but it is useful to version your assets automatically, so that you don't have to handle cache invalidation.

**gzip** config param default is **false**, only original compiled files are uploaded.
If **gzip** is set to **true**, it will upload compressed compiled files.
If **gzip** is set to **both**, it will upload original compiled files + compressed compiled files (with .gz extension).

You should set a pretty big **expires** value (to add **Cache-Control** and **Expires** metadata), so that browsers cache assets locally.

For S3 provider, config params search order is `grails.assets.cdn` config, then `grails.plugin.awssdk.s3`, then `grails.plugin.awssdk`.

Note: for providers credentials, never use your root user access keys, you should create a specific user (ex. AWS IAM user) with the corresponding bucket permissions.


# Usage

## Pushing your assets to a Cloud Storage Service

Add this command to your build process (usually before war generation and deployment).

```groovy
// If all the settings are defined in your Config.groovy
grails asset-cdn-push
// Or
grails asset-cdn-push --provider=S3 --directory=my-bucket --gzip=true --storage-path=some-prefix --expires=365 --region=eu-west-1 --access-key=$MY_S3_ACCESS_KEY --secret-key=$MY_S3_SECRET_KEY
```

## Allowing your domain with a CORS rule

When you create your bucket on your Cloud Storage Service, you might need to add a [CORS rule](http://docs.aws.amazon.com/AmazonS3/latest/dev/cors.html) (Cross-Origin Resource Sharing).

Here is a command that will automatically do it for you!

```groovy
// If all the settings are defined in your Config.groovy
grails asset-cdn-directory-cors --origin=*.mydomain.com
// Or
grails asset-cdn-directory-cors --origin=*.mydomain.com --provider=S3 --directory=my-bucket --region=eu-west-1 --access-key=$MY_S3_ACCESS_KEY --secret-key=$MY_S3_SECRET_KEY
```

## Using your CDN-based assets

In your [Asset Pipeline](http://grails.org/plugin/asset-pipeline) config, add your CDN URL (including your app prefix)

```groovy
grails.assets.url = "https://s3.amazonaws.com/my-bucket/assets/${appName}-${appVersion}"
```

# Latest releases

* 2014-04-29 **V0.3.3** : Minor fix
* 2014-04-28 **V0.3.2** : Karman AWS Grails Plugin upgraded to 0.4.2 and Asset pipeline Grails Plugin upgraded to 1.8.5
* 2014-04-03 **V0.3.1** : Upload original web font files (for web fonts referenced in CSS)
* 2014-03-27 **V0.3.0** : Only compiled files are now uploaded (based on assets manifest) + optional gzip param to upload compressed compiled files
* 2014-03-21 **V0.2.3** : Asset pipeline plugin upgraded to 1.7.1 + plugin dependencies changed to runtime
* 2014-03-07 **V0.2.2** : Minor update to BuildConfig + README
* 2014-03-07 **V0.2.1** : Minor update to BuildConfig (do not export Karman plugins)
* 2014-03-04 **V0.2** : Initial release

# Bugs

To report any bug, please use the project [Issues](http://github.com/agorapulse/grails-cdn-asset-pipeline/issues) section on GitHub.