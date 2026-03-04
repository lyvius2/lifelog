package com.walter.lifelog.service

import com.walter.lifelog.dto.Author
import com.walter.lifelog.mapper.UserMapper
import com.walter.lifelog.repository.UserRepository
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