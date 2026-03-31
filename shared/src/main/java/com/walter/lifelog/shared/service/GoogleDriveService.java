package com.walter.lifelog.shared.service;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.walter.lifelog.shared.annotation.DynamicCacheable;
import com.walter.lifelog.shared.dto.ImageResource;
import com.walter.lifelog.shared.util.AsyncSupporter;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class GoogleDriveService {
    private static final Logger log = LoggerFactory.getLogger(GoogleDriveService.class);
    private static final int THUMB_MAX_WIDTH = 600;
    private final Drive drive;
    private final TaskExecutor virtualThreadExecutor;

    public GoogleDriveService(Drive drive, TaskExecutor virtualThreadExecutor) {
        this.drive = drive;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    private GoogleDriveService self;

    @Lazy
    @Autowired
    public void setSelf(GoogleDriveService self) {
        this.self = self;
    }

    @DynamicCacheable(value = "driveFileId", key = "#parentId + ':' + #name + ':' + #isFolder", ttlMinutes = 180)
    public String findFileId(String parentId, @NotNull String name, boolean isFolder) throws IOException {
        final String mimeCondition = isFolder
                ? " and mimeType = 'application/vnd.google-apps.folder'"
                : " and mimeType != 'application/vnd.google-apps.folder'";

        final String query = "'" + parentId + "' in parents"
                + " and name = '" + name.replace("'", "\\'") + "'"
                + mimeCondition
                + " and trashed = false";

        final var result = drive.files().list()
                .setQ(query)
                .setPageSize(1)
                .setFields("files(id)")
                .execute();
        final List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            return null;
        }
        return files.getFirst().getId();
    }

    public String findOrCreateFolder(String parentId, String folderName) throws IOException {
        final String folderId = self.findFileId(parentId, folderName, true);
        if (folderId != null) {
            return folderId;
        }
        final File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(parentId));
        final File folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute();
        return folder.getId();
    }

    public File uploadFile(String fileName, String parentId, InputStream inputStream, String contentType) {
        try {
            final File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(parentId));
            final InputStreamContent content = new InputStreamContent(contentType, inputStream);
            return drive.files()
                    .create(fileMetadata, content)
                    .setFields("id, name, mimeType, size, webViewLink, webContentLink")
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + fileName, e);
        }
    }

    public File generateThumbnail(String fileName, String originalParentId, InputStream inputStream, String contentType) {
        try {
            final BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                return null;
            }

            final BufferedImage thumbImage = resizeImage(originalImage, THUMB_MAX_WIDTH);
            final String formatName = getFormatName(contentType);
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(thumbImage, formatName, outputStream);
            final byte[] thumbBytes = outputStream.toByteArray();
            final String thumbParentId = findOrCreateFolder(originalParentId, "thumb");
            final String thumbFileName = getThumbFileName(fileName);
            return uploadFile(thumbFileName, thumbParentId, new ByteArrayInputStream(thumbBytes), contentType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate thumbnail: " + fileName, e);
        }
    }

    public File[] uploadImage(String folderPath, String originalFilename, String contentType, InputStream mainInputStream, InputStream thumbInputStream) throws IOException {
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files can be uploaded : " + contentType);
        }
        try {
            final String[] folders = Arrays.stream(folderPath.split("/"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
            String parentId = "root";
            for (final String folder : folders) {
                parentId = findOrCreateFolder(parentId, folder);
            }

            final String finalParentId = parentId;
            final CompletableFuture<File> mainFuture = AsyncSupporter.asyncSupply(virtualThreadExecutor, () -> uploadFile(originalFilename, finalParentId, mainInputStream, contentType));
            final CompletableFuture<File> thumbFuture = AsyncSupporter.asyncSupply(virtualThreadExecutor, () -> generateThumbnail(originalFilename, finalParentId, thumbInputStream, contentType));
            self.evictAllFileIdCache();
            return new File[]{mainFuture.get(), thumbFuture.get()};
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image: " + originalFilename, e);
        }
    }

    public ImageResource getImageByPath(@NotNull String path) throws IOException {
        final String[] segments = Arrays.stream(path.split("/"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (segments.length == 0) {
            throw new RuntimeException("path is empty : " + path);
        }

        final String[] folderSegments = Arrays.copyOfRange(segments, 0, segments.length - 1);
        final String parentId = resolveFolderPath(folderSegments);
        final String fileName = segments[segments.length - 1];
        final String fileId = self.findFileId(parentId, fileName, false);
        final CompletableFuture<File> metaFuture = AsyncSupporter.asyncSupply(virtualThreadExecutor, () -> getMetaFromDrive(drive, fileId));
        final CompletableFuture<byte[]> downloadFuture = AsyncSupporter.asyncSupply(virtualThreadExecutor, () -> getBytesFromDrive(drive, fileId));

        try {
            final File fileMeta = metaFuture.get();
            final String mimeType = fileMeta.getMimeType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                throw new RuntimeException("file is not image : " + fileMeta.getName() + " (경로: " + path + ")");
            }
            final byte[] bytes = downloadFuture.get();
            return new ImageResource(
                    new ByteArrayInputStream(bytes),
                    mimeType,
                    fileMeta.getName(),
                    bytes.length
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get image by path: " + path, e);
        }
    }

    private static File getMetaFromDrive(@NotNull Drive drive, String fileId) {
        try {
            return drive.files().get(fileId)
                    .setFields("id, name, mimeType, size")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get file metadata: " + fileId, e);
        }
    }

    @NotNull
    private static byte[] getBytesFromDrive(@NotNull Drive drive, String fileId) {
        try {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            drive.files().get(fileId).executeMediaAndDownloadTo(buffer);
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file: " + fileId, e);
        }
    }

    private String resolveFolderPath(@NotNull String[] folders) throws IOException {
        if (folders.length == 0) {
            return "root";
        }
        String parentId = "root";
        final StringBuilder pathBuilder = new StringBuilder();
        for (final String folder : folders) {
            if (!pathBuilder.isEmpty()) pathBuilder.append("/");
            pathBuilder.append(folder);
            final String folderId = self.findFileId(parentId, folder, true);
            if (folderId == null) {
                throw new RuntimeException("폴더를 찾을 수 없습니다: " + pathBuilder);
            }
            parentId = folderId;
        }
        return parentId;
    }

    @NotNull
    private BufferedImage resizeImage(@NotNull BufferedImage original, int maxWidth) {
        if (original.getWidth() <= maxWidth) {
            return original;
        }

        final double ratio = (double) maxWidth / original.getWidth();
        final int newHeight = (int) (original.getHeight() * ratio);
        final int imageType = original.getType() != 0 ? original.getType() : BufferedImage.TYPE_INT_RGB;
        final BufferedImage resized = new BufferedImage(maxWidth, newHeight, imageType);
        final Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, maxWidth, newHeight, null);
        g2d.dispose();
        return resized;
    }

    @NotNull
    private String getFormatName(@NotNull String contentType) {
        if (contentType.contains("png")) {
            return "png";
        }
        if (contentType.contains("webp")) {
            return "webp";
        }
        return "jpg";
    }

    @NotNull
    private String getThumbFileName(String fileName) {
        final String originalName = Objects.toString(fileName, "image");
        final int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            return originalName.substring(0, dotIndex) + "_thumb" + originalName.substring(dotIndex);
        }
        return originalName + "_thumb";
    }

    @CacheEvict(value = "driveFileId", allEntries = true)
    public void evictAllFileIdCache() {
        log.info("All file ID cache evicted.");
    }
}
