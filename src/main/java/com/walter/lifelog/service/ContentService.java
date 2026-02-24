package com.walter.lifelog.service;

import com.walter.lifelog.entity.Content;
import com.walter.lifelog.entity.code.ContentType;
import com.walter.lifelog.repository.ContentsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ContentService {
    private final ContentsRepository contentsRepository;

    public ContentService(ContentsRepository contentsRepository) {
        this.contentsRepository = contentsRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getContentByType(@NotNull ContentType contentType) {
        final Content content = contentsRepository.findByContentType(contentType);
        if (content == null) {
            return null;
        }
        return content.content();
    }
}
