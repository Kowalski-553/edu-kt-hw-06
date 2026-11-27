package com.example.edu_kt_hw_06.data

import android.content.ContentValues
import android.content.Context
import com.example.edu_kt_hw_06.library.BookCatalog
import com.example.edu_kt_hw_06.library.BookEditor
import com.example.edu_kt_hw_06.library.BookingService

class LibraryRepository(context: Context) : BookCatalog, BookEditor, BookingService {

    private val dbHelper = AppDbHelper(context.applicationContext)

    override fun allBooks(): List<Book> =
        queryBooks(selection = "$COLUMN_IS_ACTIVE = $ACTIVE")

    override fun bookedBooks(): List<Book> =
        queryBooks(selection = "$COLUMN_IS_ACTIVE = $ACTIVE AND $COLUMN_BOOKED_BY IS NOT NULL")

    override fun myBooks(login: String): List<Book> = queryBooks(
        selection = "$COLUMN_IS_ACTIVE = $ACTIVE AND $COLUMN_BOOKED_BY = ?",
        selectionArgs = arrayOf(login)
    )

    override fun addBook(title: String, author: String, genre: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_AUTHOR, author)
            put(COLUMN_GENRE, genre)
            put(COLUMN_IS_ACTIVE, ACTIVE)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        return dbHelper.writableDatabase.insert(AppDbHelper.TABLE_BOOKS, null, values)
    }

    override fun updateBook(id: Long, title: String, author: String, genre: String) {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_AUTHOR, author)
            put(COLUMN_GENRE, genre)
        }
        dbHelper.writableDatabase.update(
            AppDbHelper.TABLE_BOOKS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    override fun deactivateBook(id: Long) {
        val values = ContentValues().apply { put(COLUMN_IS_ACTIVE, DELETED) }
        dbHelper.writableDatabase.update(
            AppDbHelper.TABLE_BOOKS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    override fun book(bookId: Long, login: String): Boolean {
        // booked_by IS NULL прямо в условии UPDATE: если книгу успели забрать,
        // строки не обновятся и мы честно вернём false
        val values = ContentValues().apply { put(COLUMN_BOOKED_BY, login) }
        val updatedRows = dbHelper.writableDatabase.update(
            AppDbHelper.TABLE_BOOKS,
            values,
            "$COLUMN_ID = ? AND $COLUMN_BOOKED_BY IS NULL AND $COLUMN_IS_ACTIVE = $ACTIVE",
            arrayOf(bookId.toString())
        )
        return updatedRows > 0
    }

    override fun cancelBooking(bookId: Long, login: String): Boolean {
        val values = ContentValues().apply { putNull(COLUMN_BOOKED_BY) }
        // отменить можно только СВОЮ бронь
        val updatedRows = dbHelper.writableDatabase.update(
            AppDbHelper.TABLE_BOOKS,
            values,
            "$COLUMN_ID = ? AND $COLUMN_BOOKED_BY = ?",
            arrayOf(bookId.toString(), login)
        )
        return updatedRows > 0
    }

    private fun queryBooks(selection: String, selectionArgs: Array<String>? = null): List<Book> {
        val books = mutableListOf<Book>()
        dbHelper.readableDatabase.query(
            AppDbHelper.TABLE_BOOKS,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$COLUMN_TITLE COLLATE NOCASE ASC"
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(COLUMN_TITLE)
            val authorIndex = cursor.getColumnIndexOrThrow(COLUMN_AUTHOR)
            val genreIndex = cursor.getColumnIndexOrThrow(COLUMN_GENRE)
            val bookedByIndex = cursor.getColumnIndexOrThrow(COLUMN_BOOKED_BY)
            val isActiveIndex = cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)
            while (cursor.moveToNext()) {
                books.add(
                    Book(
                        id = cursor.getLong(idIndex),
                        title = cursor.getString(titleIndex),
                        author = cursor.getString(authorIndex),
                        genre = cursor.getString(genreIndex),
                        bookedBy = if (cursor.isNull(bookedByIndex)) null
                        else cursor.getString(bookedByIndex),
                        isActive = cursor.getInt(isActiveIndex) == ACTIVE
                    )
                )
            }
        }
        return books
    }

    private companion object {
        const val ACTIVE = 1
        const val DELETED = 0

        const val COLUMN_ID = AppDbHelper.COLUMN_ID
        const val COLUMN_TITLE = AppDbHelper.COLUMN_TITLE
        const val COLUMN_AUTHOR = AppDbHelper.COLUMN_AUTHOR
        const val COLUMN_GENRE = AppDbHelper.COLUMN_GENRE
        const val COLUMN_BOOKED_BY = AppDbHelper.COLUMN_BOOKED_BY
        const val COLUMN_IS_ACTIVE = AppDbHelper.COLUMN_IS_ACTIVE
        const val COLUMN_CREATED_AT = AppDbHelper.COLUMN_CREATED_AT
    }
}
