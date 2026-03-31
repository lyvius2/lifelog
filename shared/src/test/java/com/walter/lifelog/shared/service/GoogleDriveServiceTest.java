package com.walter.lifelog.shared.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GoogleDriveService 테스트")
@ExtendWith(MockitoExtension.class)
class GoogleDriveServiceTest {

    @Mock
    private GoogleAuthorizationCodeFlow flow;

    private GoogleDriveService googleDriveService;

    @BeforeEach
    void setUp() {
        TaskExecutor virtualThreadExecutor = Runnable::run;
        googleDriveService = new GoogleDriveService(flow, virtualThreadExecutor);
    }

    @Nested
    @DisplayName("findFileId() 메서드")
    class FindFileId {

        @Test
        @DisplayName("파일이 존재하면 파일 ID를 반환한다")
        void shouldReturnFileIdWhenFileExists() throws IOException {
            // given
            Drive drive = mock(Drive.class);
            Drive.Files files = mock(Drive.Files.class);
            Drive.Files.List listRequest = mock(Drive.Files.List.class);

            File file = new File();
            file.setId("file123");
            FileList fileList = new FileList();
            fileList.setFiles(List.of(file));

            when(drive.files()).thenReturn(files);
            when(files.list()).thenReturn(listRequest);
            when(listRequest.setQ(anyString())).thenReturn(listRequest);
            when(listRequest.setPageSize(1)).thenReturn(listRequest);
            when(listRequest.setFields(anyString())).thenReturn(listRequest);
            when(listRequest.execute()).thenReturn(fileList);

            // when
            String result = googleDriveService.findFileId(drive, "root", "testFile.jpg", false);

            // then
            assertThat(result).isEqualTo("file123");
        }

        @Test
        @DisplayName("파일이 존재하지 않으면 null을 반환한다")
        void shouldReturnNullWhenFileNotExists() throws IOException {
            // given
            Drive drive = mock(Drive.class);
            Drive.Files files = mock(Drive.Files.class);
            Drive.Files.List listRequest = mock(Drive.Files.List.class);

            FileList fileList = new FileList();
            fileList.setFiles(Collections.emptyList());

            when(drive.files()).thenReturn(files);
            when(files.list()).thenReturn(listRequest);
            when(listRequest.setQ(anyString())).thenReturn(listRequest);
            when(listRequest.setPageSize(1)).thenReturn(listRequest);
            when(listRequest.setFields(anyString())).thenReturn(listRequest);
            when(listRequest.execute()).thenReturn(fileList);

            // when
            String result = googleDriveService.findFileId(drive, "root", "nonExistent.jpg", false);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("폴더 검색 시 폴더 mimeType 조건이 적용된다")
        void shouldSearchForFolderMimeType() throws IOException {
            // given
            Drive drive = mock(Drive.class);
            Drive.Files files = mock(Drive.Files.class);
            Drive.Files.List listRequest = mock(Drive.Files.List.class);

            File folder = new File();
            folder.setId("folder456");
            FileList fileList = new FileList();
            fileList.setFiles(List.of(folder));

            when(drive.files()).thenReturn(files);
            when(files.list()).thenReturn(listRequest);
            when(listRequest.setQ(anyString())).thenReturn(listRequest);
            when(listRequest.setPageSize(1)).thenReturn(listRequest);
            when(listRequest.setFields(anyString())).thenReturn(listRequest);
            when(listRequest.execute()).thenReturn(fileList);

            // when
            String result = googleDriveService.findFileId(drive, "root", "photos", true);

            // then
            assertThat(result).isEqualTo("folder456");
        }
    }

    @Nested
    @DisplayName("findOrCreateFolder() 메서드")
    class FindOrCreateFolder {

        @Test
        @DisplayName("폴더가 이미 존재하면 기존 폴더 ID를 반환한다")
        void shouldReturnExistingFolderIdWhenFolderExists() throws IOException {
            // given
            Drive drive = mock(Drive.class);
            Drive.Files files = mock(Drive.Files.class);
            Drive.Files.List listRequest = mock(Drive.Files.List.class);

            File folder = new File();
            folder.setId("existingFolder");
            FileList fileList = new FileList();
            fileList.setFiles(List.of(folder));

            when(drive.files()).thenReturn(files);
            when(files.list()).thenReturn(listRequest);
            when(listRequest.setQ(anyString())).thenReturn(listRequest);
            when(listRequest.setPageSize(1)).thenReturn(listRequest);
            when(listRequest.setFields(anyString())).thenReturn(listRequest);
            when(listRequest.execute()).thenReturn(fileList);

            // self 주입 (findFileId 호출을 위해)
            googleDriveService.setSelf(googleDriveService);

            // when
            String result = googleDriveService.findOrCreateFolder(drive, "root", "myFolder");

            // then
            assertThat(result).isEqualTo("existingFolder");
        }

        @Test
        @DisplayName("폴더가 존재하지 않으면 새 폴더를 생성하고 ID를 반환한다")
        void shouldCreateNewFolderWhenNotExists() throws IOException {
            // given
            Drive drive = mock(Drive.class);
            Drive.Files files = mock(Drive.Files.class);
            Drive.Files.List listRequest = mock(Drive.Files.List.class);
            Drive.Files.Create createRequest = mock(Drive.Files.Create.class);

            FileList emptyList = new FileList();
            emptyList.setFiles(Collections.emptyList());

            File createdFolder = new File();
            createdFolder.setId("newFolder789");

            when(drive.files()).thenReturn(files);
            when(files.list()).thenReturn(listRequest);
            when(listRequest.setQ(anyString())).thenReturn(listRequest);
            when(listRequest.setPageSize(1)).thenReturn(listRequest);
            when(listRequest.setFields(anyString())).thenReturn(listRequest);
            when(listRequest.execute()).thenReturn(emptyList);
            when(files.create(any(File.class))).thenReturn(createRequest);
            when(createRequest.setFields(anyString())).thenReturn(createRequest);
            when(createRequest.execute()).thenReturn(createdFolder);

            googleDriveService.setSelf(googleDriveService);

            // when
            String result = googleDriveService.findOrCreateFolder(drive, "root", "newFolder");

            // then
            assertThat(result).isEqualTo("newFolder789");
            verify(files).create(any(File.class));
        }
    }

    @Nested
    @DisplayName("getImageByPath() 메서드")
    class GetImageByPath {

        @Test
        @DisplayName("빈 경로가 주어지면 RuntimeException을 발생시킨다")
        void shouldThrowExceptionForEmptyPath() {
            // given / when / then
            assertThatThrownBy(() -> googleDriveService.getImageByPath(""))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("path is empty");
        }

        @Test
        @DisplayName("공백만 있는 경로가 주어지면 RuntimeException을 발생시킨다")
        void shouldThrowExceptionForBlankPath() {
            // given / when / then
            assertThatThrownBy(() -> googleDriveService.getImageByPath("   /  / "))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("path is empty");
        }
    }

    @Nested
    @DisplayName("uploadImage() 메서드")
    class UploadImage {

        @Test
        @DisplayName("이미지가 아닌 파일을 업로드하면 IllegalArgumentException을 발생시킨다")
        void shouldThrowExceptionForNonImageFile() {
            // given
            InputStream mainStream = new ByteArrayInputStream(new byte[0]);
            InputStream thumbStream = new ByteArrayInputStream(new byte[0]);

            // when / then
            assertThatThrownBy(() ->
                    googleDriveService.uploadImage("photos", "test.pdf", "application/pdf", mainStream, thumbStream)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only image files can be uploaded");
        }

        @Test
        @DisplayName("contentType이 null이면 IllegalArgumentException을 발생시킨다")
        void shouldThrowExceptionForNullContentType() {
            // given
            InputStream mainStream = new ByteArrayInputStream(new byte[0]);
            InputStream thumbStream = new ByteArrayInputStream(new byte[0]);

            // when / then
            assertThatThrownBy(() ->
                    googleDriveService.uploadImage("photos", "test.jpg", null, mainStream, thumbStream)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only image files can be uploaded");
        }
    }

    @Nested
    @DisplayName("getDrive() 메서드")
    class GetDrive {

        @Test
        @DisplayName("인증 정보가 없으면 RuntimeException을 발생시킨다")
        void shouldThrowExceptionWhenCredentialIsNull() throws IOException {
            // given
            when(flow.loadCredential("user")).thenReturn(null);

            // when / then
            assertThatThrownBy(() -> googleDriveService.getDrive())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Google Drive 클라이언트 생성 중 오류가 발생했습니다.");
        }
    }

    @Nested
    @DisplayName("evictAllFileIdCache() 메서드")
    class EvictAllFileIdCache {

        @Test
        @DisplayName("캐시 무효화 메서드가 정상적으로 실행된다")
        void shouldEvictCacheSuccessfully() {
            // given / when / then
            googleDriveService.evictAllFileIdCache();
            // 예외 없이 실행되면 성공
        }
    }
}

