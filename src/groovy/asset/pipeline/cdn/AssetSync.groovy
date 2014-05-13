package asset.pipeline.cdn

import com.bertramlabs.plugins.karman.CloudFile
import com.bertramlabs.plugins.karman.CloudFileACL
import com.bertramlabs.plugins.karman.Directory
import com.bertramlabs.plugins.karman.StorageProvider
import com.bertramlabs.plugins.karman.util.Mimetypes

class AssetSync {

    List providers = []
    Directory localDirectory
    StorageProvider localProvider
	String localStoragePath
    Date expirationDate
    String gzip
	def eventListener

	AssetSync(options, eventListener) {
		this.eventListener = eventListener

		providers = options.providers ?: []
		if (options.localProvider) {
			localProvider = options.localProvider
		} else {
			localProvider = StorageProvider.create(provider: 'local', basePath: options.assetPath ?: 'target/')
		}
        localStoragePath = options.localStoragePath ?: 'assets/'
        localDirectory = localProvider[localStoragePath]
        expirationDate = options.expirationDate
        gzip = options.gzip
	}

	def sync() {
		if (!localDirectory.exists()) {
	        eventListener?.triggerEvent("StatusError", "Could not push assets, ${localDirectory} local directory not found")
	        return false
		}
		providers.eachWithIndex { provider, index ->
			eventListener?.triggerEvent("StatusUpdate", "Syncing Assets with Storage Provider ${index+1} of ${providers.size()}")

			if (synchronizeProvider(provider.clone())) {
				provider.synchronized = true // This flag is marked on the provider map when its synchronized
			}
		}
	}

	def synchronizeProvider(providerMeta) {
		try {
            String remoteDirectoryName = providerMeta.remove('directory')
            String remoteStoragePath = providerMeta.remove('storagePath')
            providerMeta.remove('expires')
            if (!remoteStoragePath.endsWith('/')) {
				remoteStoragePath = "${remoteStoragePath}/"
			}
		    if (remoteStoragePath.startsWith('/')) {
		    	remoteStoragePath = remoteStoragePath.replaceFirst('/', '')
	    	}
            StorageProvider removeProvider = StorageProvider.create(providerMeta + [defaultFileACL: CloudFileACL.PublicRead])

            Directory remoteDirectory = removeProvider[remoteDirectoryName]

            Map manifestFiles = [:]
            CloudFile localManifestFile = localDirectory['manifest.properties']
            if (localManifestFile.exists()) {
                CloudFile remoteManifestFile = remoteDirectory[remoteStoragePath + 'manifest.properties'] //Lets check if a remote manifest exists
                Properties remoteManifest = new Properties()
                if (remoteManifestFile.exists()) {
                    remoteManifest.load(remoteManifestFile.inputStream)
                }

                int count = 0
                localManifestFile.text.eachLine { line ->
                    String originalFileName = line.tokenize('=').first()
                    String compiledFileName = line.tokenize('=').last()
                    // Ignore file already defined in remote manifest
                    if (!line.startsWith('#') && (!remoteManifest || !remoteManifest.getProperty(originalFileName) || remoteManifest.getProperty(originalFileName) != compiledFileName)) {
                        manifestFiles[originalFileName] = compiledFileName
                        CloudFile localFile = localDirectory[compiledFileName]

                        if (localFile.exists()) {
                            eventListener?.triggerEvent('StatusUpdate', "Uploading File ${count+1} - ${localFile.name}")
                            CloudFile cloudFile = remoteDirectory[remoteStoragePath + localFile.name]
                            String cacheControl = "PUBLIC, max-age=${(expirationDate.time / 1000).toInteger()}, must-revalidate"

                            if (expirationDate) {
                                cloudFile.setMetaAttribute('Cache-Control', cacheControl)
                                cloudFile.setMetaAttribute('Expires', expirationDate)
                            }

                            CloudFile compressedLocalFile = localDirectory["${compiledFileName}.gz"]
                            if (gzip == 'true' && compressedLocalFile.exists()) {
                                // Upload compressed version
                                cloudFile.setMetaAttribute('Content-Encoding', 'gzip')
                                cloudFile.bytes = compressedLocalFile.bytes
                            } else {
                                // Upload original version
                                cloudFile.bytes = localFile.bytes
                            }

                            cloudFile.contentType = Mimetypes.instance.getMimetype(localFile.name)
                            cloudFile.save()
                            count++

                            if (gzip == 'both' && compressedLocalFile.exists()) {
                                // Upload additional compressed version (with .gz extension)
                                eventListener?.triggerEvent('StatusUpdate', "Uploading File ${count+1} - ${compressedLocalFile.name}")
                                CloudFile compressedCloudFile = remoteDirectory[remoteStoragePath + compressedLocalFile.name]
                                compressedCloudFile.setMetaAttribute('Content-Encoding', 'gzip')

                                if (expirationDate) {
                                    compressedCloudFile.setMetaAttribute('Cache-Control', cacheControl)
                                    compressedCloudFile.setMetaAttribute('Expires', expirationDate)
                                }

                                compressedCloudFile.contentType = cloudFile.contentType
                                compressedCloudFile.bytes = compressedLocalFile.bytes
                                compressedCloudFile.save()
                                count++
                            }

                            String extension = originalFileName.tokenize('.').last()
                            if (extension in ['eot', 'svg', 'ttf', 'woff']) {
                                // Workaround for webfonts referenced in CSS, upload original file
                                eventListener?.triggerEvent('StatusUpdate', "Uploading File ${count+1} - ${originalFileName}")
                                CloudFile originalCloudFile = remoteDirectory[remoteStoragePath + originalFileName]

                                if (expirationDate) {
                                    originalCloudFile.setMetaAttribute('Cache-Control', cacheControl)
                                    originalCloudFile.setMetaAttribute('Expires', expirationDate)
                                }

                                if (gzip == 'true' && compressedLocalFile.exists()) {
                                    // Upload compressed version
                                    originalCloudFile.setMetaAttribute('Content-Encoding', 'gzip')
                                    originalCloudFile.bytes = compressedLocalFile.bytes
                                } else {
                                    // Upload original version
                                    originalCloudFile.bytes = localFile.bytes
                                }

                                originalCloudFile.contentType = cloudFile.contentType
                                originalCloudFile.save()
                                count++
                            }
                        }
                    }
                }
                // Upload manifest
                remoteManifestFile.bytes = localManifestFile.bytes
                remoteManifestFile.save()
            }
			return true
		} catch(e) {
            providerMeta.remove('secretKey') // Remove secret key from error logs
            eventListener?.triggerEvent("StatusError", "Error synchronizing with provider ${providerMeta}")
        }
		return false
	}


}