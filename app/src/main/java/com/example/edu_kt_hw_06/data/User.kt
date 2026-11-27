package com.example.edu_kt_hw_06.data

/** роль пользователей. Админ и просто юзер. (ADMIN, USER)
 */
enum class UserRole {
    ADMIN,
    USER;

    companion object {
        fun fromName(value: String?): UserRole =
            entries.firstOrNull { role -> role.name == value } ?: USER
    }
}

data class User(
    val id: Long,
    val login: String,
    val role: UserRole
)
