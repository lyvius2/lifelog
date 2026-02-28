package com.walter.lifelog.service

import com.walter.lifelog.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
        if (user == null) {
            throw UsernameNotFoundException("User not found with email: $username")
        }

        return User.builder()
            .username(user.email)
            .password(user.passwordHash)
            .authorities(SimpleGrantedAuthority("ROLE_ADMIN"))
            .disabled(!user.isActive)
            .build()
    }
}

