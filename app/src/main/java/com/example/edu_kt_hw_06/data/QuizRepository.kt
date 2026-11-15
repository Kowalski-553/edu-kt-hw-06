package com.example.edu_kt_hw_06.data

import android.content.ContentValues
import android.content.Context

class QuizRepository(context: Context) {

    private val dbHelper = QuizDbHelper(context.applicationContext)

    /** сверяемлогин/пароль по таблице users. */
    fun checkCredentials(login: String, password: String): Boolean {
        val selection = "${QuizDbHelper.COLUMN_LOGIN} = ? AND ${QuizDbHelper.COLUMN_PASSWORD} = ?"
        val selectionArgs = arrayOf(login, password)
        return dbHelper.readableDatabase
            .query(
                QuizDbHelper.TABLE_USERS,
                arrayOf(QuizDbHelper.COLUMN_ID),
                selection,
                selectionArgs,
                null,
                null,
                null
            )
            .use { cursor -> cursor.moveToFirst() }
    }

    /** читаем вопрос с вариантами ответа */
    fun getQuestions(): List<Question> {
        val answersByQuestionId = mutableMapOf<Long, MutableList<Answer>>()
        dbHelper.readableDatabase
            .query(
                QuizDbHelper.TABLE_ANSWERS,
                null,
                null,
                null,
                null,
                null,
                "${QuizDbHelper.COLUMN_ID} ASC"
            )
            .use { cursor ->
                val questionIdIndex = cursor.getColumnIndexOrThrow(QuizDbHelper.COLUMN_QUESTION_ID)
                val textIndex = cursor.getColumnIndexOrThrow(QuizDbHelper.COLUMN_TEXT)
                val isCorrectIndex = cursor.getColumnIndexOrThrow(QuizDbHelper.COLUMN_IS_CORRECT)
                while (cursor.moveToNext()) {
                    val questionId = cursor.getLong(questionIdIndex)
                    val answer = Answer(
                        text = cursor.getString(textIndex),
                        isCorrect = cursor.getInt(isCorrectIndex) == CORRECT_VALUE
                    )
                    answersByQuestionId.getOrPut(questionId) { mutableListOf() }.add(answer)
                }
            }

        val questions = mutableListOf<Question>()
        dbHelper.readableDatabase
            .query(
                QuizDbHelper.TABLE_QUESTIONS,
                null,
                null,
                null,
                null,
                null,
                "${QuizDbHelper.COLUMN_ID} ASC"
            )
            .use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(QuizDbHelper.COLUMN_ID)
                val textIndex = cursor.getColumnIndexOrThrow(QuizDbHelper.COLUMN_TEXT)
                while (cursor.moveToNext()) {
                    val questionId = cursor.getLong(idIndex)
                    questions.add(
                        Question(
                            id = questionId,
                            text = cursor.getString(textIndex),
                            answers = answersByQuestionId[questionId].orEmpty()
                        )
                    )
                }
            }
        return questions
    }

    /** созраняем результат в БД */
    fun saveResult(login: String, score: Int, total: Int): QuizResult {
        val createdAt = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(QuizDbHelper.COLUMN_LOGIN, login)
            put(QuizDbHelper.COLUMN_SCORE, score)
            put(QuizDbHelper.COLUMN_TOTAL, total)
            put(QuizDbHelper.COLUMN_CREATED_AT, createdAt)
        }
        val id = dbHelper.writableDatabase.insert(QuizDbHelper.TABLE_RESULTS, null, values)
        return QuizResult(id = id, score = score, total = total, createdAt = createdAt)
    }

    /** запрос истории */
    fun getResults(login: String): List<QuizResult> {
        val results = mutableListOf<QuizResult>()
        dbHelper.readableDatabase
            .query(
                QuizDbHelper.TABLE_RESULTS,
                null,
                "${QuizDbHelper.COLUMN_LOGIN} = ?",
                arrayOf(login),
                null,
                null,
                "${QuizDbHelper.COLUMN_CREATED_AT} DESC"
            )
            .use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(QuizDbHelper.COLUMN_ID)
                val scoreIndex = cursor.getColumnIndexOrThrow(QuizDbHelper.COLUMN_SCORE)
                val totalIndex = cursor.getColumnIndexOrThrow(QuizDbHelper.COLUMN_TOTAL)
                val createdAtIndex = cursor.getColumnIndexOrThrow(QuizDbHelper.COLUMN_CREATED_AT)
                while (cursor.moveToNext()) {
                    results.add(
                        QuizResult(
                            id = cursor.getLong(idIndex),
                            score = cursor.getInt(scoreIndex),
                            total = cursor.getInt(totalIndex),
                            createdAt = cursor.getLong(createdAtIndex)
                        )
                    )
                }
            }
        return results
    }

    private companion object {
        const val CORRECT_VALUE = 1
    }
}
