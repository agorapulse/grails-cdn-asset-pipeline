package asset.pipeline.cdn

import com.bertramlabs.plugins.karman.*
import com.bertramlabs.plugins.karman.local.*

class AssetSync {

	def providers = []
	def localProvider
	def localDirectory = 'assets'
	def expirationDate
	def eventListener

	AssetSync(options,eventListener) {
		this.eventListener = eventListener

		providers = options.providers ?: []
		if(options.localProvider) {
			localProvider = localProvider
		} else {
			localProvider = StorageProvider.create(provider: 'local', basePath: options.assetPath ?: 'target')
		}
		localDirectory = options.localDirectory ?: localDirectory
		expirationDate = options.expirationDate
		
	}

	def sync() {
		providers.eachWithIndex { provider, index ->
			eventListener?.triggerEvent("StatusUpdate", "Syncing Assets with Storage Provider ${index+1} of ${providers.size()}")

			if(synchronizeProvider(provider.clone())) {
				provider.synchronized = true //This flag is marked on the provider map when its synchronized
			}
		}
	}

	def synchronizeProvider(providerMeta) {

		try {
			def remoteDirectory = providerMeta.remove('storagePath')
			def provider = StorageProvider.create(providerMeta + [defaultFileACL: CloudFileACL.PublicRead])

			def remoteManifestFile = provider[localDirectory]['manifest.properties'] //Lets check if a remote manifest exists
			def remoteManifest = new Properties()
			if(remoteManifestFile.exists()) {
				remoteManifest.load(remoteManifestFile.inputStream)
				// TODO: We need to download this manifest, run a comparison and only upload/remove whats changed
			}


			def files = localProvider.listFiles()
			files.eachWithIndex { localFile, index ->
				eventListener?.triggerEvent("StatusUpdate", "Uploading File ${index+1} of ${files.size()} - ${localFile.name}")
				def cloudFile = provider[remoteDirectory ?: localDirectory][localFile.name]

				if (expirationDate) {
	                cloudFile.setMetaAttribute("Cache-Control", "PUBLIC, max-age=${(expirationDate.time / 1000).toInteger()}, must-revalidate")
	                cloudFile.setMetaAttribute("Expires", expirationDate)
	            }
				
				cloudFile.bytes = localFile.bytes
				cloudFile.save()
			}
			return true
		} catch(e) {
			log.error("Error Synchronizing With Provider ${provider}", e)
		}
		return false
	}


}