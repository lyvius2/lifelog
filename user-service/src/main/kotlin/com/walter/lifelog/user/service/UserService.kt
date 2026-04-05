package com.walter.lifelog.user.service

import com.walter.lifelog.shared.annotation.DynamicCacheable
import com.walter.lifelog.user.dto.Author
import com.walter.lifelog.user.dto.UserSimpleInfo
import com.walter.lifelog.user.mapper.UserMapper
import com.walter.lifelog.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
) {
    fun getUserSimpleInfo(email: String) : UserSimpleInfo {
        val user = userRepository.findByEmail(email) ?: throw IllegalArgumentException("User not found with email: $email")
        return userMapper.toUserSimpleInfoDto(user)
    }

    @DynamicCacheable(value = ["authorInfoByUserSeq"], key = "#userSeq", ttlMinutes = 1440)
    fun getAuthorInfoByUserSeq(userSeq: Long) : Author {
        val user = userRepository.findByUserSeq(userSeq) ?: throw IllegalArgumentException("User not found with userSeq: $userSeq")
        return userMapper.toAuthorDto(user)
    }
}