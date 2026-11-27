package com.example.edu_kt_hw_06.library

import com.example.edu_kt_hw_06.data.LibraryRepository
import com.example.edu_kt_hw_06.data.UserRole

/** собирает фасад библиотеки под роль текущего пользователя */
object LibraryFactory {

    fun create(role: UserRole, repository: LibraryRepository): RoleLibrary = when (role) {
        UserRole.ADMIN -> AdminLibrary(repository)
        UserRole.USER -> UserLibrary(repository)
    }
}
