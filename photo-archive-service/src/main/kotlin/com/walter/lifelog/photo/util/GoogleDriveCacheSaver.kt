package com.walter.lifelog.photo.util

import com.google.api.services.drive.Drive
import com.walter.lifelog.shared.annotation.DynamicCacheable
import com.walter.lifelog.shared.util.GoogleDriveHelper
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Component
class GoogleDriveCacheSaver(
    private val googleDriveHelper: GoogleDriveHelper,
) {
    private val log = LoggerFactory.getLogger(GoogleDriveCacheSaver::class.java)

    @DynamicCacheable(value = ["driveFolderId"], key = "#cacheKey", ttlMinutes = 180)
    fun getFolderId(cacheKey: String, drive: Drive, parentId: String, folderName: String): String {
        return googleDriveHelper.findFileId(drive, parentId, folderName, true)
            ?: throw RuntimeException("path cannot be found: $folderName")
    }

    @DynamicCacheable(value = ["driveFolderId"], key = "#cacheKey", ttlMinutes = 180)
    fun getOrCreateFolderId(cacheKey: String, drive: Drive, parentId: String, folderName: String): String {
        return googleDriveHelper.findOrCreateFolder(drive, parentId, folderName)
    }

    @DynamicCacheable(value = ["driveFileId"], key = "#cacheKey", ttlMinutes = 180)
    fun getFileId(cacheKey: String, drive: Drive, parentId: String, fileName: String): String {
        return googleDriveHelper.findFileId(drive, parentId, fileName, false)
            ?: throw RuntimeException("file cannot be found: $fileName")
    }

    @CacheEvict(value = ["driveFileId"], allEntries = true)
    fun evictAllFileIdCache() {
        log.info("All file ID cache evicted.")
    }
}