package com.walter.lifelog.mapper

import com.walter.lifelog.controller.dto.AuthorResponse
import com.walter.lifelog.entity.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface UserMapper {

    @Mapping(target = "name", source = "displayName")
    fun toAuthorDto(user: User) : AuthorResponse
}