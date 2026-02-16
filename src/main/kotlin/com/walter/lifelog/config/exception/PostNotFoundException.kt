package com.walter.lifelog.config.exception

import java.lang.IllegalArgumentException

class PostNotFoundException(
    postInquiryStr: String,
): IllegalArgumentException("Post not found for inquiry: $postInquiryStr") {
}