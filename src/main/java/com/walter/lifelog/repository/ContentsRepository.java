package com.walter.lifelog.repository;

import com.walter.lifelog.entity.Content;
import com.walter.lifelog.entity.code.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentsRepository extends JpaRepository<Content, Long> {
    Content findByContentType(ContentType contentType);
}
