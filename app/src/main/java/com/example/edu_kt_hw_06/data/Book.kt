package com.example.edu_kt_hw_06.data

data class Book(
    val id: Long,
    val title: String,
    val author: String,
    val genre: String,
    val bookedBy: String? = null,
    val isActive: Boolean = true
) {
    val isBooked: Boolean get() = bookedBy != null

    fun isBookedBy(login: String): Boolean = bookedBy == login
}
