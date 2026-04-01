package com.walter.lifelog.shared.service;

import com.walter.lifelog.shared.service.dto.AiChatRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.ai.openai.api-key")
public class OpenAiChatService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiChatService.class);
    private final ChatClient openAiChatClient;

    public OpenAiChatService(ChatClient openAiChatClient) {
        this.openAiChatClient = openAiChatClient;
    }

    public String chat(AiChatRequest aiChatRequest) {
        try {
            return openAiChatClient.prompt()
                    .system(aiChatRequest.systemMessage())
                    .user(aiChatRequest.userMessage())
                    .call()
                    .content();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }
}
