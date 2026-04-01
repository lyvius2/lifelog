package com.walter.lifelog.shared.config;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.services.drive.DriveScopes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
public class GoogleDriveConfig {
    public static final String APPLICATION_NAME = "Lifelog Photo Archive";
    public static final String SERVICE_ACCOUNT_FILE_PATH = "/service-account.json";
    public static final String ROOT_FOLDER_ID = "14dXd-dVIFuaUZB1Cy8E6ZnIaU1O1r2iD";

    @Bean("googleCredentials")
    public GoogleCredentials googleCredentials() throws IOException {
        final InputStream serviceAccountStream = GoogleDriveConfig.class.getResourceAsStream(SERVICE_ACCOUNT_FILE_PATH);
        if (serviceAccountStream == null) {
            throw new IllegalStateException("service-account.json 파일을 찾을 수 없습니다.");
        }
        return GoogleCredentials.fromStream(serviceAccountStream)
                .createScoped(List.of(DriveScopes.DRIVE));
    }

    @Bean
    @DependsOn("googleCredentials")
    public Drive drive(GoogleCredentials googleCredentials) {
        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(googleCredentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
