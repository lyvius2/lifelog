package com.walter.lifelog.shared.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.walter.lifelog.shared.config.GoogleDriveConfig;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GoogleDriveHelper {
    private static final int THUMB_MAX_WIDTH = 600;
    private final GoogleAuthorizationCodeFlow flow;

    public GoogleDriveHelper(GoogleAuthorizationCodeFlow flow) {
        this.flow = flow;
    }

    public Drive getDrive() {
        try {
            final var credential = flow.loadCredential("user");
            if (credential == null || credential.getAccessToken() == null) {
                throw new IllegalStateException("Google Drive 인증이 필요합니다.");
            }
            return new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(GoogleDriveConfig.APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Google Drive 클라이언트 생성 중 오류가 발생했습니다.", e);
        }
    }

    public String findFileId(Drive drive, String parentId, String name, boolean isFolder) throws IOException {
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

    public String findOrCreateFolder(Drive drive, String parentId, String folderName) throws IOException {
        final String folderId = findFileId(drive, parentId, folderName, true);
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

    public File uploadFile(String fileName, String parentId, InputStream inputStream, String contentType) throws IOException {
        final File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(parentId));
        final InputStreamContent content = new InputStreamContent(contentType, inputStream);
        return getDrive().files()
                .create(fileMetadata, content)
                .setFields("id, name, mimeType, size, webViewLink, webContentLink")
                .execute();
    }

    public File generateThumbnail(String fileName, String originalParentId, InputStream inputStream, String contentType) throws IOException {
        final BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            return null;
        }

        final BufferedImage thumbImage = resizeImage(originalImage, THUMB_MAX_WIDTH);
        final String formatName = getFormatName(contentType);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(thumbImage, formatName, outputStream);
        final byte[] thumbBytes = outputStream.toByteArray();
        final String thumbParentId = findOrCreateFolder(getDrive(), originalParentId, "thumb");
        final String thumbFileName = getThumbFileName(fileName);
        return uploadFile(thumbFileName, thumbParentId, new ByteArrayInputStream(thumbBytes), contentType);
    }

    private BufferedImage resizeImage(BufferedImage original, int maxWidth) {
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

    private String getFormatName(String contentType) {
        if (contentType.contains("png")) {
            return "png";
        }
        if (contentType.contains("webp")) {
            return "webp";
        }
        return "jpg";
    }

    private String getThumbFileName(String fileName) {
        final String originalName = Objects.toString(fileName, "image");
        final int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            return originalName.substring(0, dotIndex) + "_thumb" + originalName.substring(dotIndex);
        }
        return originalName + "_thumb";
    }
}
