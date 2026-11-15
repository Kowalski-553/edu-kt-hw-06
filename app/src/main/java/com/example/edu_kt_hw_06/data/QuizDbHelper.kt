package com.example.edu_kt_hw_06.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * таблицы users, questions/answers/results
 * создаем и наполняем
 */
class QuizDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_USERS)
        db.execSQL(CREATE_QUESTIONS)
        db.execSQL(CREATE_ANSWERS)
        db.execSQL(CREATE_RESULTS)
        insertSeedUser(db)
        insertSeedQuestions(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESULTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ANSWERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_QUESTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun insertSeedUser(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(COLUMN_LOGIN, QuizSeed.USER_LOGIN)
            put(COLUMN_PASSWORD, QuizSeed.USER_PASSWORD)
        }
        db.insert(TABLE_USERS, null, values)
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

    companion object {
        private const val DATABASE_NAME = "quiz.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_USERS = "users"
        const val TABLE_QUESTIONS = "questions"
        const val TABLE_ANSWERS = "answers"
        const val TABLE_RESULTS = "results"

        const val COLUMN_ID = "_id"
        const val COLUMN_LOGIN = "login"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_TEXT = "text"
        const val COLUMN_QUESTION_ID = "question_id"
        const val COLUMN_IS_CORRECT = "is_correct"
        const val COLUMN_SCORE = "score"
        const val COLUMN_TOTAL = "total"
        const val COLUMN_CREATED_AT = "created_at"

        private val CREATE_USERS = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LOGIN TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL
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
    }
}
