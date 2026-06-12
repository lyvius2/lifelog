package com.walter.lifelog.api.controller

import com.walter.lifelog.api.controller.dto.Rest
import com.walter.lifelog.api.util.LikedPhotoCookieHandler
import com.walter.lifelog.photo.dto.PhotoCategoryResponse
import com.walter.lifelog.photo.dto.PhotoLikeCountResponse
import com.walter.lifelog.photo.dto.PhotoSearchResponse
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.dto.UploadResponse
import com.walter.lifelog.photo.facade.PhotoArchiveFacade
import com.walter.lifelog.api.annotation.AdminRequired
import com.walter.lifelog.shared.paging.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "사진", description = "사진 업로드 및 관리 API")
@RequestMapping("/api/photo")
@RestController
class PhotoController(
    private val photoArchiveFacade: PhotoArchiveFacade,
    @Value("\${photo.upload-dir:lifelog/pictures}") private val uploadBasePath: String,
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

    @AdminRequired
    @PostMapping("/upload", consumes = ["multipart/form-data"])
    @Operation(summary = "사진 업로드", description = "이미지 파일을 Google Drive에 업로드한다. 로그인 세션 또는 AccessToken이 필요.", security = [SecurityRequirement(name = "Authorization")])
    @ResponseBody
    fun uploadPhoto(
        @Parameter(description = "이미지 파일", required = true)
        @RequestPart("file") file: MultipartFile,
        @Parameter(description = "업로드 메타데이터 (JSON)", required = true)
        @RequestPart("uploadRequest") uploadRequest: UploadRequest,
        @Parameter(hidden = true) request: HttpServletRequest
    ): Rest<UploadResponse> {
        val userSeq = request.getAttribute("userSeq") as Long
        return Rest.ok(photoArchiveFacade.uploadPhoto(uploadRequest, "$uploadBasePath/${uploadRequest.categorySeq}", userSeq, file))
    }

    @GetMapping("/categories")
    @Operation(summary = "사진 카테고리 조회", description = "활성화된 사진 카테고리 목록을 조회한다.")
    fun getPhotoCategories(): Rest<List<PhotoCategoryResponse>> {
        return Rest.ok(photoArchiveFacade.getActivePhotoCategories())
    }

    @AdminRequired
    @DeleteMapping("/delete")
    @Operation(summary = "사진 삭제", description = "사진을 논리 삭제한다 (is_active = false). 관리자 로그인 필요.", security = [SecurityRequirement(name = "Authorization")])
    fun deletePhoto(
        @Parameter(description = "사진 시퀀스", required = true, example = "1")
        @RequestParam("photoSeq") photoSeq: Long,
    ): Rest<Unit> {
        photoArchiveFacade.deletePhoto(photoSeq)
        return Rest.ok(Unit)
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
        LikedPhotoCookieHandler.validateCookie(photoSeq, request, response)
        return Rest.ok(photoArchiveFacade.increaseLikeCount(photoSeq))
    }
}