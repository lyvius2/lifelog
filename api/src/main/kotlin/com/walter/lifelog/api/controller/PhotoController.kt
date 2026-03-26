package com.walter.lifelog.api.controller

import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.api.util.CookieHandler
import com.walter.lifelog.photo.dto.*
import com.walter.lifelog.photo.facade.PhotoArchiveFacade
import com.walter.lifelog.shared.paging.PageResponse
import com.walter.lifelog.shared.util.AccessTokenHandler
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "사진", description = "사진 업로드 및 관리 API")
@RequestMapping("/api/photo")
@RestController
class PhotoController(
    private val photoArchiveFacade: PhotoArchiveFacade,
    @Value("\${photo.upload-dir:lifelog/pictures}") private val uploadBasePath: String,
    @Value("\${jwt.secret-key:tempKey}") private val jwtSecretKey: String,
) {
    @GetMapping("")
    @Operation(summary = "사진 목록 조회", description = "전체 또는 카테고리별 사진 목록을 순차적으로 조회한다.")
    fun getPhotos(
        @Parameter(description = "카테고리 시퀀스", required = false, example = "1")
        @RequestParam("categorySeq", required = false) categorySeq: Long?,
        @Parameter(description = "페이지 번호 (1부터 시작)", required = false, example = "1")
        @RequestParam("page", required = false) page: Int?
    ): Rest<PageResponse<PhotoSearchResponse>> {
        return Rest.ok(photoArchiveFacade.getPhotos(categorySeq, page))
    }

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    @Operation(summary = "사진 업로드", description = "이미지 파일을 Google Drive에 업로드한다.", security = [SecurityRequirement(name = "Authorization")])
    @ResponseBody
    fun uploadPhoto(
        @Parameter(description = "이미지 파일", required = true)
        @RequestPart("file") file: MultipartFile,
        @Parameter(description = "업로드 메타데이터 (JSON)", required = true)
        @RequestPart("uploadRequest") uploadRequest: UploadRequest,
        @Parameter(description = "JWT 인증 토큰", required = false)
        @RequestHeader("Authorization") @Parameter(hidden = true) authorization: String?,
        @Parameter(hidden = true) session: HttpSession?
    ): Rest<UploadResponse> {
        val userSeq = if (authorization != null) {
            AccessTokenHandler.getUserSeqFromToken(authorization, jwtSecretKey)
        } else {
            session!!.getAttribute("userSeq") as? Long ?: throw IllegalStateException("로그인이 필요합니다.")
        }
        return Rest.ok(photoArchiveFacade.uploadPhoto(uploadRequest, "$uploadBasePath/${uploadRequest.categorySeq}", userSeq, file))
    }

    @GetMapping("/categories")
    @Operation(summary = "사진 카테고리 조회", description = "활성화된 사진 카테고리 목록을 조회한다.")
    fun getPhotoCategories(): Rest<List<PhotoCategoryResponse>> {
        return Rest.ok(photoArchiveFacade.getActivePhotoCategories())
    }

    @PostMapping("/like-count")
    @Operation(summary = "사진 Like 증가", description = "특정 사진의 Like 수를 증가시킨다. Photo Archive 화면에서만 호출 가능하며, 같은 클라이언트에서 동일 사진에 중복 좋아요를 방지한다.")
    fun getLikeCount(
        @Parameter(description = "사진 시퀀스", required = true, example = "1")
        @RequestParam("photoSeq") photoSeq: Long,
        @Parameter(hidden = true)
        @RequestHeader("Referer", required = false) referer: String?,
        @Parameter(hidden = true) request: HttpServletRequest,
        @Parameter(hidden = true) response: HttpServletResponse,
    ): Rest<PhotoLikeCountResponse> {
        if (referer.isNullOrBlank() || !referer.contains("/photos")) {
            throw IllegalArgumentException("Photo Archive 화면에서만 Like를 할 수 있습니다.")
        }
        CookieHandler.validateCookie(photoSeq, request, response)
        return Rest.ok(photoArchiveFacade.increaseLikeCount(photoSeq))
    }
}