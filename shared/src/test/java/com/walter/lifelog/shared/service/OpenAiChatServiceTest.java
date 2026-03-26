package com.walter.lifelog.shared.service;

import com.walter.lifelog.shared.service.dto.AiChatRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiChatService 테스트")
@ExtendWith(MockitoExtension.class)
class OpenAiChatServiceTest {

    @Mock
    private ChatClient openAiChatClient;

    @InjectMocks
    private OpenAiChatService openAiChatService;

    @Nested
    @DisplayName("chat() 메서드")
    class Chat {

        @Test
        @DisplayName("정상 요청 시 AI 응답 문자열을 반환한다")
        void shouldReturnAiResponse() {
            // given
            AiChatRequest request = AiChatRequest.of("You are a helpful assistant.", "Hello!");
            String expectedResponse = "안녕하세요! 무엇을 도와드릴까요?";

            ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.ChatClientRequestSpec systemSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.ChatClientRequestSpec userSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

            when(openAiChatClient.prompt()).thenReturn(promptSpec);
            when(promptSpec.system(anyString())).thenReturn(systemSpec);
            when(systemSpec.user(anyString())).thenReturn(userSpec);
            when(userSpec.call()).thenReturn(callSpec);
            when(Objects.requireNonNull(callSpec.content())).thenReturn(expectedResponse);

            // when
            String result = openAiChatService.chat(request);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            verify(openAiChatClient).prompt();
            verify(promptSpec).system("You are a helpful assistant.");
            verify(systemSpec).user("Hello!");
        }

        @Test
        @DisplayName("예외 발생 시 빈 문자열을 반환한다")
        void shouldReturnEmptyStringOnException() {
            // given
            AiChatRequest request = AiChatRequest.of("system", "user");

            when(openAiChatClient.prompt()).thenThrow(new RuntimeException("API 호출 실패"));

            // when
            String result = openAiChatService.chat(request);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("call() 단계에서 예외 발생 시 빈 문자열을 반환한다")
        void shouldReturnEmptyStringWhenCallFails() {
            // given
            AiChatRequest request = AiChatRequest.of("system", "user");

            ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.ChatClientRequestSpec systemSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.ChatClientRequestSpec userSpec = mock(ChatClient.ChatClientRequestSpec.class);

            when(openAiChatClient.prompt()).thenReturn(promptSpec);
            when(promptSpec.system(anyString())).thenReturn(systemSpec);
            when(systemSpec.user(anyString())).thenReturn(userSpec);
            when(userSpec.call()).thenThrow(new RuntimeException("타임아웃 발생"));

            // when
            String result = openAiChatService.chat(request);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("content()가 null을 반환해도 정상 처리된다")
        void shouldHandleNullContent() {
            // given
            AiChatRequest request = AiChatRequest.of("system", "user");

            ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.ChatClientRequestSpec systemSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.ChatClientRequestSpec userSpec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

            when(openAiChatClient.prompt()).thenReturn(promptSpec);
            when(promptSpec.system(anyString())).thenReturn(systemSpec);
            when(systemSpec.user(anyString())).thenReturn(userSpec);
            when(userSpec.call()).thenReturn(callSpec);
            when(Objects.requireNonNull(callSpec.content())).thenReturn(null);

            // when
            String result = openAiChatService.chat(request);

            // then
            assertThat(result).isNull();
        }
    }
}

