package com.walter.lifelog.service

import com.walter.lifelog.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun getUserByEmail(email: String) = userRepository.findByEmail(email)
}