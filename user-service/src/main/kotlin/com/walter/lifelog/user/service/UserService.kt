package com.walter.lifelog.user.service

import com.walter.lifelog.user.dto.Author
import com.walter.lifelog.user.mapper.UserMapper
import com.walter.lifelog.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
) {
    fun getUserSeqByEmail(email: String) : Long {
        val user = userRepository.findByEmail(email) ?: throw IllegalArgumentException("User not found with email: $email")
        return user.userSeq!!
    }

    fun getAuthorInfoByUserSeq(userSeq: Long) : Author {
        val user = userRepository.findByUserSeq(userSeq) ?: throw IllegalArgumentException("User not found with userSeq: $userSeq")
        return userMapper.toAuthorDto(user)
    }
}