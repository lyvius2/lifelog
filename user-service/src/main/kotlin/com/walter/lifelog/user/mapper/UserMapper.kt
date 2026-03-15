package com.walter.lifelog.user.mapper

import com.walter.lifelog.user.entity.User
import com.walter.lifelog.user.dto.Author
import com.walter.lifelog.user.dto.UserSimpleInfo
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface UserMapper {
    @Mapping(target = "name", source = "displayName")
    @Mapping(target = "profileImageUrl", source = "profileImageUrl")
    fun toAuthorDto(user: User): Author

    fun toUserSimpleInfoDto(user: User): UserSimpleInfo
}