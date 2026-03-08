package com.walter.lifelog.shared.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.walter.lifelog.shared.config.GoogleDriveConfig;

import java.io.IOException;
import java.util.List;

public class GoogleDriveHelper {
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
            throw new RuntimeException("Google Drive에서 '" + name + "' 파일을 찾을 수 없습니다.");
        }
        return files.getFirst().getId();
    }
}
