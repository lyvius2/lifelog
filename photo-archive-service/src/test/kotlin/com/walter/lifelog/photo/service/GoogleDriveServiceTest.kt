package com.walter.lifelog.photo.service

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.walter.lifelog.photo.util.GoogleDriveCacheSaver
import com.walter.lifelog.shared.config.exception.GoogleDriveException
import com.walter.lifelog.shared.util.GoogleDriveHelper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskExecutor
import java.io.ByteArrayOutputStream

class GoogleDriveServiceTest {
    private lateinit var virtualThreadExecutor: TaskExecutor
    private lateinit var googleDriveHelper: GoogleDriveHelper
    private lateinit var googleDriveCacheSaver: GoogleDriveCacheSaver
    private lateinit var drive: Drive
    private lateinit var service: GoogleDriveService

    @BeforeEach
    fun setUp() {
        virtualThreadExecutor = TaskExecutor { it.run() }
        googleDriveHelper = mockk()
        googleDriveCacheSaver = mockk(relaxed = true)
        drive = mockk()
        service = GoogleDriveService(virtualThreadExecutor, googleDriveHelper, googleDriveCacheSaver)

        every { googleDriveHelper.drive } returns drive
    }

    @Test
    @DisplayName("경로로 이미지 파일을 정상 조회한다")
    fun getImageByPath_success() {
        // given
        val folderId = "folder-123"
        val fileId = "file-456"
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        every { googleDriveCacheSaver.getFolderId("lifelog", drive, "root", "lifelog") } returns folderId
        every { googleDriveCacheSaver.getFileId("lifelog/furaiki.png", drive, folderId, "furaiki.png") } returns fileId

        // 메타데이터 조회 mock
        val fileMeta = File().apply {
            id = fileId
            name = "furaiki.png"
            mimeType = "image/png"
            setSize(pngBytes.size.toLong())
        }
        val getRequest = mockk<Drive.Files.Get>()
        val filesResource = mockk<Drive.Files>()
        every { drive.files() } returns filesResource
        every { filesResource.get(fileId) } returns getRequest
        every { getRequest.setFields("id, name, mimeType, size") } returns getRequest
        every { getRequest.execute() } returns fileMeta

        // 파일 다운로드 mock
        val downloadRequest = mockk<Drive.Files.Get>()
        every { filesResource.get(fileId) } returns downloadRequest
        every { downloadRequest.setFields("id, name, mimeType, size") } returns downloadRequest
        every { downloadRequest.execute() } returns fileMeta
        every { downloadRequest.executeMediaAndDownloadTo(any<ByteArrayOutputStream>()) } answers {
            val os = firstArg<ByteArrayOutputStream>()
            os.write(pngBytes)
        }

        // when
        val result = service.getImageByPath("lifelog/furaiki.png")

        // then
        assertNotNull(result)
        assertEquals("image/png", result!!.mimeType)
        assertEquals("furaiki.png", result.fileName)
        assertEquals(pngBytes.size.toLong(), result.fileSize)
        assertArrayEquals(pngBytes, result.inputStream.readAllBytes())
    }

    @Test
    @DisplayName("이미지가 아닌 파일 조회 시 GoogleDriveException 발생")
    fun getImageByPath_notImage() {
        // given
        val fileId = "file-789"

        every { googleDriveCacheSaver.getFileId("readme.txt", drive, "root", "readme.txt") } returns fileId

        val fileMeta = File().apply {
            id = fileId
            name = "readme.txt"
            mimeType = "text/plain"
            setSize(100L)
        }
        val getRequest = mockk<Drive.Files.Get>()
        val filesResource = mockk<Drive.Files>()
        every { drive.files() } returns filesResource
        every { filesResource.get(fileId) } returns getRequest
        every { getRequest.setFields("id, name, mimeType, size") } returns getRequest
        every { getRequest.execute() } returns fileMeta

        // when & then
        val exception = assertThrows(GoogleDriveException::class.java) {
            service.getImageByPath("readme.txt")
        }
        assertTrue(exception.message!!.contains("file is not image"))
    }

    @Test
    @DisplayName("빈 경로 전달 시 GoogleDriveException 발생")
    fun getImageByPath_emptyPath() {
        val exception = assertThrows(GoogleDriveException::class.java) {
            service.getImageByPath("   ")
        }
        assertTrue(exception.message!!.contains("path is empty"))
    }
}

