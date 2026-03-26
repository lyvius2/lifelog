package com.walter.lifelog.shared.service.dto;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record AiChatRequest(
    String systemMessage,
    String userMessage
) {
    @NotNull
    @Contract("_, _ -> new")
    public static AiChatRequest of(String systemMessage, String userMessage) {
        return new AiChatRequest(systemMessage, userMessage);
    }
}
