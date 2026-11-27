package com.example.edu_kt_hw_06.data

import android.content.ContentValues
import android.content.Context

class QuizRepository(context: Context) {

    private val dbHelper = AppDbHelper(context.applicationContext)

    /** ищем пользователя по логину/паролю: возвращаем его вместе с ролью */
    fun findUser(login: String, password: String): User? {
        val selection = "${AppDbHelper.COLUMN_LOGIN} = ? AND ${AppDbHelper.COLUMN_PASSWORD} = ?"
        val selectionArgs = arrayOf(login, password)
        return dbHelper.readableDatabase
            .query(
                AppDbHelper.TABLE_USERS,
                arrayOf(
                    AppDbHelper.COLUMN_ID,
                    AppDbHelper.COLUMN_LOGIN,
                    AppDbHelper.COLUMN_ROLE
                ),
                selection,
                selectionArgs,
                null,
                null,
                null
            )
            .use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_ID)),
                    login = cursor.getString(cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_LOGIN)),
                    role = UserRole.fromName(
                        cursor.getString(cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_ROLE))
                    )
                )
            }
    }

    /** читаем вопрос с вариантами ответа */
    fun getQuestions(): List<Question> {
        val answersByQuestionId = mutableMapOf<Long, MutableList<Answer>>()
        dbHelper.readableDatabase
            .query(
                AppDbHelper.TABLE_ANSWERS,
                null,
                null,
                null,
                null,
                null,
                "${AppDbHelper.COLUMN_ID} ASC"
            )
            .use { cursor ->
                val questionIdIndex = cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_QUESTION_ID)
                val textIndex = cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_TEXT)
                val isCorrectIndex = cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_IS_CORRECT)
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
                AppDbHelper.TABLE_QUESTIONS,
                null,
                null,
                null,
                null,
                null,
                "${AppDbHelper.COLUMN_ID} ASC"
            )
            .use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_ID)
                val textIndex = cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_TEXT)
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
            put(AppDbHelper.COLUMN_LOGIN, login)
            put(AppDbHelper.COLUMN_SCORE, score)
            put(AppDbHelper.COLUMN_TOTAL, total)
            put(AppDbHelper.COLUMN_CREATED_AT, createdAt)
        }
        val id = dbHelper.writableDatabase.insert(AppDbHelper.TABLE_RESULTS, null, values)
        return QuizResult(id = id, score = score, total = total, createdAt = createdAt)
    }

    /** запрос истории */
    fun getResults(login: String): List<QuizResult> {
        val results = mutableListOf<QuizResult>()
        dbHelper.readableDatabase
            .query(
                AppDbHelper.TABLE_RESULTS,
                null,
                "${AppDbHelper.COLUMN_LOGIN} = ?",
                arrayOf(login),
                null,
                null,
                "${AppDbHelper.COLUMN_CREATED_AT} DESC"
            )
            .use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_ID)
                val scoreIndex = cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_SCORE)
                val totalIndex = cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_TOTAL)
                val createdAtIndex = cursor.getColumnIndexOrThrow(AppDbHelper.COLUMN_CREATED_AT)
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
