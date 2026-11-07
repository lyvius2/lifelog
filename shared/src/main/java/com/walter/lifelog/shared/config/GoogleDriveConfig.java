package com.walter.lifelog.shared.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.walter.lifelog.shared.util.GoogleDriveHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@Configuration
public class GoogleDriveConfig {
    public static final String APPLICATION_NAME = "Lifelog Photo Archive";
    public static final String CREDENTIALS_FILE_PATH = "classpath:/credential.json";
    public static final String TOKENS_DIRECTORY_PATH = "tokens";
    public static final String USER_ID = "user";
    public static final String CALLBACK_PATH = "/google-auth/callback";

    /**
     * OAuth 클라이언트 파일의 위치.
     *
     * <p>기본값은 클래스패스이며 로컬 개발에서는 그대로 동작합니다. 컨테이너에서는
     * 자격증명을 이미지에 넣지 않고 마운트하므로 {@code file:} 경로를 줍니다.
     *
     * <pre>
     *   GOOGLE_CREDENTIALLOCATION=file:/app/config/credential.json
     * </pre>
     *
     * <p>네이티브 이미지에서는 이 설정이 선택이 아니라 필수입니다. 네이티브는 클래스패스
     * 리소스를 빌드 시점에 바이너리로 임베드하기 때문에, 마운트한 파일을 클래스패스로는
     * 읽을 수 없습니다.
     */
    @Value("${google.credential-location:" + CREDENTIALS_FILE_PATH + "}")
    private Resource credentialResource;

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow() throws IOException {
        final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        final NetHttpTransport httpTransport = new NetHttpTransport();
        if (!credentialResource.exists()) {
            throw new IllegalStateException(
                    "credential.json 을 찾을 수 없습니다: " + credentialResource
                            + " (google.credential-location 으로 위치를 지정하십시오)");
        }

        final GoogleClientSecrets clientSecrets;
        try (InputStream credentialStream = credentialResource.getInputStream()) {
            clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(credentialStream));
        }
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientSecrets,
                List.of(DriveScopes.DRIVE)
        )
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }

    @Bean
    @DependsOn("googleAuthorizationCodeFlow")
    public GoogleDriveHelper googleDriveHelper(GoogleAuthorizationCodeFlow flow) {
        return new GoogleDriveHelper(flow);
    }
}

