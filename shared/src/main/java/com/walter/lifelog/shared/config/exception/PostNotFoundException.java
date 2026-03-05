package com.walter.lifelog.shared.config.exception;

public class PostNotFoundException extends IllegalArgumentException {
    public PostNotFoundException(String postInquiryStr) {
        super("Post not found for inquiry: " + postInquiryStr);
    }
}
