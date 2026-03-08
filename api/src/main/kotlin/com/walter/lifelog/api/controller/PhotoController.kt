package com.walter.lifelog.api.controller

import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.photo.dto.UploadResult
import com.walter.lifelog.photo.service.GoogleDriveService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import kotlin.String

@Tag(name = "Photo", description = "사진 업로드 및 관리 API")
@RequestMapping("/api/photo")
@RestController
class PhotoController(
    private val googleDriveService: GoogleDriveService,
    @Value("\${photo.upload-dir:lifelog/photos}") private val uploadBasePath: String,
) {
    @Operation(summary = "사진 업로드", description = "이미지 파일을 Google Drive에 업로드한다.")
    @PostMapping("/upload")
    @ResponseBody
    fun uploadPhoto(
        @Parameter(description = "이미지 파일", required = true)
        @RequestParam("file") file: MultipartFile,
        @Parameter(description = "사진 제목")
        @RequestParam(value = "title", required = false, defaultValue = "") title: String?,
        @Parameter(description = "사진 설명")
        @RequestParam(value = "caption", required = false, defaultValue = "") caption: String?,
        @Parameter(description = "카테고리 (nature, travel, hokkaido, urban, etc, daily)")
        @RequestParam(value = "category", required = false, defaultValue = "etc") category: String?,
        @Parameter(description = "촬영 일자 (yyyy-MM-dd)")
        @RequestParam(value = "date", required = false, defaultValue = "") date: String?,
        @Parameter(description = "태그 JSON 배열 문자열")
        @RequestParam(value = "tags", required = false, defaultValue = "[]") tags: String?,
        @Parameter(description = "EXIF 정보 JSON 문자열")
        @RequestParam(value = "exif", required = false) exif: String?
    ): Rest<UploadResult> {
        return Rest.ok(googleDriveService.uploadImage("$uploadBasePath/$category", file))
    }
}