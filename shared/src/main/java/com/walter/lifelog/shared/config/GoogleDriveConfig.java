package com.walter.lifelog.shared.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.walter.lifelog.shared.util.GoogleDriveHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@Configuration
public class GoogleDriveConfig {
    public static final String APPLICATION_NAME = "Lifelog Photo Archive";
    public static final String CREDENTIALS_FILE_PATH = "/credential.json";
    public static final String TOKENS_DIRECTORY_PATH = "tokens";
    public static final String USER_ID = "user";
    public static final String CALLBACK_PATH = "/google-auth/callback";

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow() throws IOException {
        final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        final NetHttpTransport httpTransport = new NetHttpTransport();
        final InputStream credentialStream = GoogleDriveConfig.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (credentialStream == null) {
            throw new IllegalStateException("credential.json 파일을 찾을 수 없습니다.");
        }

        final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(credentialStream));
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientSecrets,
                List.of(DriveScopes.DRIVE_READONLY)
        )
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
    }

    @Bean
    @DependsOn("googleAuthorizationCodeFlow")
    public GoogleDriveHelper googleDriveHelper(GoogleAuthorizationCodeFlow flow) {
        return new GoogleDriveHelper(flow);
    }
}

