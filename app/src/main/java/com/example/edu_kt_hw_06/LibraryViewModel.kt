package com.example.edu_kt_hw_06

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.edu_kt_hw_06.data.Book
import com.example.edu_kt_hw_06.data.LibraryRepository
import com.example.edu_kt_hw_06.data.UserRole
import com.example.edu_kt_hw_06.library.BookCatalog
import com.example.edu_kt_hw_06.library.BookEditor
import com.example.edu_kt_hw_06.library.BookingService
import com.example.edu_kt_hw_06.library.LibraryFactory
import com.example.edu_kt_hw_06.library.RoleLibrary

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    /** создастся один раз при первом обращении, а не в конструкторе */
    private val repository: LibraryRepository by lazy { LibraryRepository(application) }

    private var library: RoleLibrary? = null

    var role: UserRole = UserRole.USER
        private set

    var login: String = ""
        private set

    val isAdmin: Boolean get() = role == UserRole.ADMIN

    /** роль могла смениться после выхода/входа */
    fun start(login: String, role: UserRole) {
        if (this.login == login && this.role == role && library != null) return
        this.login = login
        this.role = role
        library = LibraryFactory.create(role, repository)
    }

    fun books(tabIndex: Int): List<Book> {
        val catalog: BookCatalog = requireNotNull(library) { "Не вызван start()" }.catalog
        return when {
            tabIndex == ALL_BOOKS_TAB -> catalog.allBooks()
            isAdmin -> catalog.bookedBooks()
            else -> catalog.myBooks(login)
        }
    }

    fun addBook(title: String, author: String, genre: String): Boolean {
        val editor = library as? BookEditor ?: return false
        return editor.addBook(title, author, genre) > 0
    }

    fun updateBook(id: Long, title: String, author: String, genre: String) {
        (library as? BookEditor)?.updateBook(id, title, author, genre)
    }

    fun deleteBook(id: Long) {
        (library as? BookEditor)?.deactivateBook(id)
    }

    fun book(id: Long): Boolean = (library as? BookingService)?.book(id, login) ?: false

    fun cancelBooking(id: Long): Boolean =
        (library as? BookingService)?.cancelBooking(id, login) ?: false

    companion object {
        const val ALL_BOOKS_TAB = 0
        const val SECOND_TAB = 1
    }
}
