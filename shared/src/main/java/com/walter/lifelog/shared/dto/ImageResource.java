package com.walter.lifelog.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.InputStream;

@Schema(description = "이미지 리소스")
public record ImageResource(
    @Schema(description = "이미지 InputStream", hidden = true)
    InputStream inputStream,
    @Schema(description = "MIME 타입", example = "image/jpeg")
    String mimeType,
    @Schema(description = "파일 이름", example = "photo.jpg")
    String fileName,
    @Schema(description = "파일 크기 (bytes)", example = "2048576")
    long fileSize
) {}