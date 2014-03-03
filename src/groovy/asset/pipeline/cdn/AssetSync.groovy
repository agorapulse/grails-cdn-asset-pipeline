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

            CloudFile localManifestFile = localDirectory['manifest.properties']
            if (localManifestFile.exists()) {
                // TODO: We need to download this manifest, run a comparison and only upload\/remove whats changed
                /*CloudFile remoteManifestFile = remoteDirectory[remoteStoragePath + 'manifest.properties'] //Lets check if a remote manifest exists
                Properties remoteManifest = new Properties()
                if (remoteManifestFile.exists()) {
                    //remoteManifest.load(remoteManifestFile.inputStream)
                    // ...
                }*/
            }

            List files = localDirectory.listFiles()
            files.eachWithIndex { localFile, index ->
                eventListener?.triggerEvent("StatusUpdate", "Uploading File ${index+1} of ${files.size()} - ${localFile.name}")
                CloudFile cloudFile = remoteDirectory[remoteStoragePath + localFile.name]

                if (expirationDate) {
                    cloudFile.setMetaAttribute("Cache-Control", "PUBLIC, max-age=${(expirationDate.time / 1000).toInteger()}, must-revalidate")
                    cloudFile.setMetaAttribute("Expires", expirationDate)
                }

                cloudFile.contentType = Mimetypes.instance.getMimetype(localFile.name)
                cloudFile.bytes = localFile.bytes
                cloudFile.save()
            }
			return true
		} catch(e) {
            providerMeta.remove('secretKey') // Remove secret key from error logs
            eventListener?.triggerEvent("StatusError", "Error synchronizing with provider ${providerMeta}")
        }
		return false
	}


}