package com.walter.lifelog.shared.dto;

import java.io.InputStream;

public record ImageResource(
    InputStream inputStream,
    String mimeType,
    String parentId,
    String fileName,
    long fileSize
) {}