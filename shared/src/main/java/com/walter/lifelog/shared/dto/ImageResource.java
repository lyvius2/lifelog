package com.walter.lifelog.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.InputStream;

@Schema(description = "이미지 리소스")
public class ImageResource {
    @Schema(description = "이미지 InputStream", hidden = true)
    private final InputStream inputStream;

    @Schema(description = "MIME 타입", example = "image/jpeg")
    private final String mimeType;

    @Schema(description = "파일 이름", example = "photo.jpg")
    private final String fileName;

    @Schema(description = "파일 크기 (bytes)", example = "2048576")
    private final long fileSize;

    public ImageResource(InputStream inputStream, String mimeType, String fileName, long fileSize) {
        this.inputStream = inputStream;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }
}

