package com.walter.lifelog.api.controller

import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.photo.dto.UploadResult
import com.walter.lifelog.photo.service.GoogleDriveService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import kotlin.String

@RequestMapping("/api/photo")
@RestController
class PhotoController(
    private val googleDriveService: GoogleDriveService,
    @Value("\${photo.upload-dir:lifelog/photos}") private val uploadBasePath: String,
) {
    /**
     * 이미지 파일을 Google Drive에 업로드합니다.
     * photo-upload.html에서 드래그 앤 드롭 또는 파일 선택을 통해 호출됩니다.
     *
     * @param file     이미지 파일
     * @param title    사진 제목
     * @param caption  사진 설명
     * @param category 카테고리 (nature, travel, hokkaido, urban, etc, daily)
     * @param date     촬영 일자
     * @param tags     태그 JSON 배열 문자열
     * @param exif     EXIF JSON 문자열
     */
    @PostMapping("/upload")
    @ResponseBody
    fun uploadPhoto(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(value = "title", required = false, defaultValue = "") title: String?,
        @RequestParam(value = "caption", required = false, defaultValue = "") caption: String?,
        @RequestParam(value = "category", required = false, defaultValue = "etc") category: String?,
        @RequestParam(value = "date", required = false, defaultValue = "") date: String?,
        @RequestParam(value = "tags", required = false, defaultValue = "[]") tags: String?,
        @RequestParam(value = "exif", required = false) exif: String?
    ): Rest<UploadResult> {
        return Rest.ok(googleDriveService.uploadImage("$uploadBasePath/$category", file))
    }
}