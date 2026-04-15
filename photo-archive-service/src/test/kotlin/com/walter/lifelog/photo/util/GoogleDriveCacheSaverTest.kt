package com.walter.lifelog.photo.util

import com.google.api.services.drive.Drive
import com.walter.lifelog.shared.util.GoogleDriveHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GoogleDriveCacheSaverTest {

    private lateinit var googleDriveHelper: GoogleDriveHelper
    private lateinit var drive: Drive
    private lateinit var googleDriveCacheSaver: GoogleDriveCacheSaver

    @BeforeEach
    fun setUp() {
        googleDriveHelper = mockk()
        drive = mockk()
        googleDriveCacheSaver = GoogleDriveCacheSaver(googleDriveHelper)
    }

    // -----------------------------------------------------------------------
    // getFolderId
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getFolderId - 폴더가 존재하면 폴더 ID를 반환한다")
    fun getFolderId_success() {
        // given
        val cacheKey = "2024/01"
        val parentId = "root"
        val folderName = "2024"
        val expectedFolderId = "folder-abc"

        every { googleDriveHelper.findFileId(drive, parentId, folderName, true) } returns expectedFolderId

        // when
        val result = googleDriveCacheSaver.getFolderId(cacheKey, drive, parentId, folderName)

        // then
        assertThat(result).isEqualTo(expectedFolderId)
        verify(exactly = 1) { googleDriveHelper.findFileId(drive, parentId, folderName, true) }
    }

    @Test
    @DisplayName("getFolderId - 폴더를 찾을 수 없으면 RuntimeException을 던진다")
    fun getFolderId_notFound_throwsException() {
        // given
        val cacheKey = "2024/01"
        val parentId = "root"
        val folderName = "2024"

        every { googleDriveHelper.findFileId(drive, parentId, folderName, true) } returns null

        // when & then
        assertThatThrownBy {
            googleDriveCacheSaver.getFolderId(cacheKey, drive, parentId, folderName)
        }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("path cannot be found: $folderName")
    }

    // -----------------------------------------------------------------------
    // getOrCreateFolderId
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getOrCreateFolderId - 기존 폴더가 있으면 해당 폴더 ID를 반환한다")
    fun getOrCreateFolderId_existingFolder_returnsFolderId() {
        // given
        val cacheKey = "lifelog/photos"
        val parentId = "root"
        val folderName = "photos"
        val expectedFolderId = "folder-xyz"

        every { googleDriveHelper.findOrCreateFolder(drive, parentId, folderName) } returns expectedFolderId

        // when
        val result = googleDriveCacheSaver.getOrCreateFolderId(cacheKey, drive, parentId, folderName)

        // then
        assertThat(result).isEqualTo(expectedFolderId)
        verify(exactly = 1) { googleDriveHelper.findOrCreateFolder(drive, parentId, folderName) }
    }

    @Test
    @DisplayName("getOrCreateFolderId - 폴더가 없으면 새로 생성 후 폴더 ID를 반환한다")
    fun getOrCreateFolderId_newFolder_returnsFolderId() {
        // given
        val cacheKey = "lifelog/new-folder"
        val parentId = "parent-001"
        val folderName = "new-folder"
        val createdFolderId = "folder-new-999"

        every { googleDriveHelper.findOrCreateFolder(drive, parentId, folderName) } returns createdFolderId

        // when
        val result = googleDriveCacheSaver.getOrCreateFolderId(cacheKey, drive, parentId, folderName)

        // then
        assertThat(result).isEqualTo(createdFolderId)
        verify(exactly = 1) { googleDriveHelper.findOrCreateFolder(drive, parentId, folderName) }
    }

    // -----------------------------------------------------------------------
    // getFileId
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getFileId - 파일이 존재하면 파일 ID를 반환한다")
    fun getFileId_success() {
        // given
        val cacheKey = "lifelog/photos/photo.jpg"
        val parentId = "folder-abc"
        val fileName = "photo.jpg"
        val expectedFileId = "file-001"

        every { googleDriveHelper.findFileId(drive, parentId, fileName, false) } returns expectedFileId

        // when
        val result = googleDriveCacheSaver.getFileId(cacheKey, drive, parentId, fileName)

        // then
        assertThat(result).isEqualTo(expectedFileId)
        verify(exactly = 1) { googleDriveHelper.findFileId(drive, parentId, fileName, false) }
    }

    @Test
    @DisplayName("getFileId - 파일을 찾을 수 없으면 RuntimeException을 던진다")
    fun getFileId_notFound_throwsException() {
        // given
        val cacheKey = "lifelog/photos/missing.jpg"
        val parentId = "folder-abc"
        val fileName = "missing.jpg"

        every { googleDriveHelper.findFileId(drive, parentId, fileName, false) } returns null

        // when & then
        assertThatThrownBy {
            googleDriveCacheSaver.getFileId(cacheKey, drive, parentId, fileName)
        }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("file cannot be found: $fileName")
    }

    // -----------------------------------------------------------------------
    // evictAllFileIdCache
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("evictAllFileIdCache - 예외 없이 정상적으로 호출된다")
    fun evictAllFileIdCache_doesNotThrow() {
        // when & then
        // @CacheEvict는 Spring AOP 기반이므로 단위 테스트에서는 메서드 자체의 실행 여부만 검증
        googleDriveCacheSaver.evictAllFileIdCache()
    }
}

