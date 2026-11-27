package com.example.edu_kt_hw_06.data

internal object UserSeed {

    data class SeedUser(val login: String, val password: String, val role: UserRole)

    // в открытом виде не очень красиво, надо бы хешировать, но для наших целей допустимо
    val users: List<SeedUser> = listOf(
        SeedUser(login = "admin", password = "admin", role = UserRole.ADMIN),
        SeedUser(login = "user", password = "qwerty", role = UserRole.USER),
        SeedUser(login = "maria", password = "1234", role = UserRole.USER)
    )
}
