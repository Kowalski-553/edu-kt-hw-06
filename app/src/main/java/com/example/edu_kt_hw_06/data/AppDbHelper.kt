package com.example.edu_kt_hw_06.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * одна БД на всё приложение, для теста users, questions, answers, results
 * для библиотеки добавлена таблица books
 * схему создаём в onCreate, наполняем сидами в onCreate/onUpgrade
 */
class AppDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_USERS)
        db.execSQL(CREATE_QUESTIONS)
        db.execSQL(CREATE_ANSWERS)
        db.execSQL(CREATE_RESULTS)
        db.execSQL(CREATE_BOOKS)
        insertSeedUsers(db)
        insertSeedQuestions(db)
        insertSeedBooks(db)
    }

    /**
     * миграции без пересоздания таблиц, иначе потеряются результаты тестов.
     * первая миграция - добавляем роли пользователей и таблицу книг.
     * вторая миграция - каталог книг заменён учебниками по программированию.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_ROLE " +
                    "TEXT NOT NULL DEFAULT '${UserRole.USER.name}'"
            )
            db.execSQL(CREATE_BOOKS)
            insertSeedUsers(db)
            insertSeedBooks(db)
        }
        if (oldVersion == 2) {
            val placeholders: String = OLD_SEED_TITLES.joinToString(", ") { "?" }
            db.delete(TABLE_BOOKS, "$COLUMN_TITLE IN ($placeholders)", OLD_SEED_TITLES)
            insertSeedBooks(db)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun insertSeedUsers(db: SQLiteDatabase) {
        UserSeed.users.forEach { user ->
            val values = ContentValues().apply {
                put(COLUMN_LOGIN, user.login)
                put(COLUMN_PASSWORD, user.password)
                put(COLUMN_ROLE, user.role.name)
            }
            db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun insertSeedQuestions(db: SQLiteDatabase) {
        QuizSeed.questions.forEach { question ->
            val questionValues = ContentValues().apply { put(COLUMN_TEXT, question.text) }
            val questionId = db.insert(TABLE_QUESTIONS, null, questionValues)
            question.answers.forEach { answer ->
                val answerValues = ContentValues().apply {
                    put(COLUMN_QUESTION_ID, questionId)
                    put(COLUMN_TEXT, answer.text)
                    put(COLUMN_IS_CORRECT, if (answer.isCorrect) 1 else 0)
                }
                db.insert(TABLE_ANSWERS, null, answerValues)
            }
        }
    }

    private fun insertSeedBooks(db: SQLiteDatabase) {
        BookSeed.books.forEach { book ->
            val values = ContentValues().apply {
                put(COLUMN_TITLE, book.title)
                put(COLUMN_AUTHOR, book.author)
                put(COLUMN_GENRE, book.genre)
                put(COLUMN_BOOKED_BY, book.bookedBy)
                put(COLUMN_IS_ACTIVE, ACTIVE_VALUE)
                put(COLUMN_CREATED_AT, System.currentTimeMillis())
            }
            db.insert(TABLE_BOOKS, null, values)
        }
    }

    companion object {
        private const val DATABASE_NAME = "quiz.db"
        private const val DATABASE_VERSION = 3

        private const val ACTIVE_VALUE = 1

        /** названия книг из сида после первой миграции (художественная литература). После второй миграции удаляем */
        private val OLD_SEED_TITLES = arrayOf(
            "Мастер и Маргарита",
            "Дюна",
            "Убийство в «Восточном экспрессе»",
            "Пикник на обочине",
            "Гарри Поттер и философский камень",
            "Котлован"
        )

        const val TABLE_USERS = "users"
        const val TABLE_QUESTIONS = "questions"
        const val TABLE_ANSWERS = "answers"
        const val TABLE_RESULTS = "results"
        const val TABLE_BOOKS = "books"

        const val COLUMN_ID = "_id"
        const val COLUMN_LOGIN = "login"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_ROLE = "role"
        const val COLUMN_TEXT = "text"
        const val COLUMN_QUESTION_ID = "question_id"
        const val COLUMN_IS_CORRECT = "is_correct"
        const val COLUMN_SCORE = "score"
        const val COLUMN_TOTAL = "total"
        const val COLUMN_CREATED_AT = "created_at"

        // книги
        const val COLUMN_TITLE = "title"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_GENRE = "genre"
        const val COLUMN_BOOKED_BY = "booked_by"
        const val COLUMN_IS_ACTIVE = "is_active"

        private val CREATE_USERS = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LOGIN TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_ROLE TEXT NOT NULL DEFAULT '${UserRole.USER.name}'
            )
        """

        private val CREATE_QUESTIONS = """
            CREATE TABLE $TABLE_QUESTIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL
            )
        """

        private val CREATE_ANSWERS = """
            CREATE TABLE $TABLE_ANSWERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_QUESTION_ID INTEGER NOT NULL
                    REFERENCES $TABLE_QUESTIONS ($COLUMN_ID) ON DELETE CASCADE,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_IS_CORRECT INTEGER NOT NULL
            )
        """

        private val CREATE_RESULTS = """
            CREATE TABLE $TABLE_RESULTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LOGIN TEXT NOT NULL,
                $COLUMN_SCORE INTEGER NOT NULL,
                $COLUMN_TOTAL INTEGER NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """

        private val CREATE_BOOKS = """
            CREATE TABLE $TABLE_BOOKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_AUTHOR TEXT NOT NULL,
                $COLUMN_GENRE TEXT NOT NULL,
                $COLUMN_BOOKED_BY TEXT,
                $COLUMN_IS_ACTIVE INTEGER NOT NULL DEFAULT $ACTIVE_VALUE,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """
    }
}
