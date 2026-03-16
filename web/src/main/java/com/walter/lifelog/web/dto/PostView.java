package com.walter.lifelog.web.dto;

import com.walter.lifelog.blog.dto.PostContents;
import com.walter.lifelog.blog.dto.PostResponse;
import com.walter.lifelog.blog.dto.PostSimpleInfo;
import com.walter.lifelog.user.dto.Author;

public record PostView(
    PostResponse content,
    Long viewCount,
    PostSimpleInfo prevContent,
    PostSimpleInfo nextContent,
    Author author
) {
    public static PostView of(PostContents postContents, Author author) {
        return new PostView(postContents.getContent(), postContents.getViewCount(), postContents.getPrevContent(), postContents.getNextContent(), author);
    }
}
