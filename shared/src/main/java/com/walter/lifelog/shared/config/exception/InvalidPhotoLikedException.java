package com.walter.lifelog.shared.config.exception;

public class InvalidPhotoLikedException extends RuntimeException {
    public InvalidPhotoLikedException(String message) {
        super(message);
    }
    public InvalidPhotoLikedException() {
        super("24시간 이내에 이미 좋아요를 누른 사진입니다.");
    }
}
