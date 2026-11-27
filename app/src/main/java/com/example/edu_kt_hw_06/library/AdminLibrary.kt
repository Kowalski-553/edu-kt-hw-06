package com.example.edu_kt_hw_06.library

import com.example.edu_kt_hw_06.data.LibraryRepository

/**
 * админ может смотреть каталог и менять его.
 */
class AdminLibrary(private val repository: LibraryRepository) :
    RoleLibrary,
    BookCatalog by repository,
    BookEditor by repository {

    override val catalog: BookCatalog get() = this

    override fun addBook(title: String, author: String, genre: String): Long {
        require(title.isNotBlank()) { "Название книги не может быть пустым" }
        return repository.addBook(title.trim(), author.trim(), genre.trim())
    }
}
