package com.example.edu_kt_hw_06.library

import com.example.edu_kt_hw_06.data.Book

/** просмотр доступен всем ролям */
interface BookCatalog {
    fun allBooks(): List<Book>

    fun bookedBooks(): List<Book>

    fun myBooks(login: String): List<Book>
}

/** редактирование доступно только администратору */
interface BookEditor {
    fun addBook(title: String, author: String, genre: String): Long

    fun updateBook(id: Long, title: String, author: String, genre: String)

    /** удалят ьрешил мягко - просто деактивируем книгу */
    fun deactivateBook(id: Long)
}

/** брониование доступно только юзерам */
interface BookingService {
    fun book(bookId: Long, login: String): Boolean

    fun cancelBooking(bookId: Long, login: String): Boolean
}

sealed interface RoleLibrary {
    val catalog: BookCatalog
}
