package com.walter.lifelog.service

import com.walter.lifelog.controller.dto.AuthorResponse
import com.walter.lifelog.mapper.UserMapper
import com.walter.lifelog.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
) {
    fun getUserByEmail(email: String) = userRepository.findByEmail(email)

    fun getAuthorInfoByUserSeq(userSeq: Long) : AuthorResponse {
        val user = userRepository.findByUserSeq(userSeq) ?: throw IllegalArgumentException("User not found with userSeq: $userSeq")
        return userMapper.toAuthorDto(user)
    }
}