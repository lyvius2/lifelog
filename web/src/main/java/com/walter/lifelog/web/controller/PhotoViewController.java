package com.walter.lifelog.web.controller;

import com.walter.lifelog.photo.dto.ImageResource;
import com.walter.lifelog.photo.dto.UploadResult;
import com.walter.lifelog.photo.service.GoogleDriveService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/photo")
public class PhotoViewController {
    private static final String DRIVE_UPLOAD_BASE_PATH = "lifelog/photos";

    private final GoogleDriveService googleDriveService;

    public PhotoViewController(GoogleDriveService googleDriveService) {
        this.googleDriveService = googleDriveService;
    }

    @GetMapping("/**")
    @ResponseBody
    public ResponseEntity<byte[]> photoView(HttpServletRequest request) throws IOException {
        final String path = request.getRequestURI().replaceFirst("^/photo/", "");
        if (StringUtils.isEmpty(path)) {
            return ResponseEntity.badRequest().build();
        }
        final ImageResource image = googleDriveService.getImageByPath(path);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentLength(image.getFileSize())
                .body(image.getInputStream().readAllBytes());
    }

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
    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<?> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false, defaultValue = "") String title,
            @RequestParam(value = "caption", required = false, defaultValue = "") String caption,
            @RequestParam(value = "category", required = false, defaultValue = "etc") String category,
            @RequestParam(value = "date", required = false, defaultValue = "") String date,
            @RequestParam(value = "tags", required = false, defaultValue = "[]") String tags,
            @RequestParam(value = "exif", required = false) String exif
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "파일이 비어 있습니다."));
        }

        final String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "이미지 파일만 업로드 가능합니다."));
        }

        try {
            final String folderPath = DRIVE_UPLOAD_BASE_PATH + "/" + category;
            final UploadResult result = googleDriveService.uploadImage(
                    folderPath,
                    Objects.requireNonNull(file.getOriginalFilename()),
                    contentType,
                    file.getInputStream()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "fileId", result.getFileId(),
                    "fileName", result.getFileName(),
                    "mimeType", result.getMimeType(),
                    "fileSize", result.getFileSize(),
                    "drivePath", folderPath + "/" + result.getFileName()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "업로드 실패: " + e.getMessage()
            ));
        }
    }
}
