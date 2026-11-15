package com.example.edu_kt_hw_06.data

/** вариант ответа */
data class Answer(
    val text: String,
    val isCorrect: Boolean
)

/** вопрос + варианты */
data class Question(
    val id: Long,
    val text: String,
    val answers: List<Answer>
)
