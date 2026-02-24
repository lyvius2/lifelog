package com.walter.lifelog.entity.code;

public enum ContentType {
    PROFILE("개발자 자기소개"),
    CAR("보유 자동차 소개");

    private final String typeDescription;

    ContentType(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public String getTypeDescription() {
        return typeDescription;
    }
}
