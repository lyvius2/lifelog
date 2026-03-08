package com.walter.lifelog.web.controller;

import com.walter.lifelog.photo.dto.ImageResource;
import com.walter.lifelog.photo.service.GoogleDriveService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
@RequestMapping("/photo")
public class PhotoViewController {
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
}
