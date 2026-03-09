package com.walter.lifelog.api.controller

import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.dto.UploadResponse
import com.walter.lifelog.photo.service.GoogleDriveService
import com.walter.lifelog.photo.service.PhotoService
import com.walter.lifelog.shared.util.AccessTokenHandler
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import kotlin.String

@Tag(name = "사진", description = "사진 업로드 및 관리 API")
@RequestMapping("/api/photo")
@RestController
class PhotoController(
    private val googleDriveService: GoogleDriveService,
    private val photoService: PhotoService,
    @Value("\${photo.upload-dir:lifelog/photos}") private val uploadBasePath: String,
    @Value("\${jwt.secret-key:tempKey}") private val jwtSecretKey: String,
) {
    @PostMapping("/upload")
    @Operation(summary = "사진 업로드", description = "이미지 파일을 Google Drive에 업로드한다.")
    @ResponseBody
    fun uploadPhoto(
        @Parameter(description = "이미지 파일", required = true)
        @RequestParam("file") file: MultipartFile,
        @RequestBody uploadRequest: UploadRequest,
        @Parameter(description = "JWT 인증 토큰", required = false)
        @RequestHeader("Authorization") authorization: String?,
        @Parameter(hidden = true) session: HttpSession?
    ): Rest<UploadResponse> {
        val userSeq = if (authorization != null) {
            AccessTokenHandler.getUserSeqFromToken(authorization, jwtSecretKey)
        } else {
            session!!.getAttribute("userSeq") as? Long ?: throw IllegalStateException("로그인이 필요합니다.")
        }
        return Rest.ok(googleDriveService.uploadImage("$uploadBasePath/${uploadRequest.categorySeq}", file))
    }

    @GetMapping("/categories")
    @Operation(summary = "사진 카테고리 조회", description = "활성화된 사진 카테고리 목록을 조회한다.")
    fun getPhotoCategories(): Rest<List<PhotoCategoryResponse>> {
        return Rest.ok(photoService.getActivePhotoCategories())
    }
}