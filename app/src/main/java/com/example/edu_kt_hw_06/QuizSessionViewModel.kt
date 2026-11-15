package com.example.edu_kt_hw_06

import androidx.lifecycle.ViewModel
import com.example.edu_kt_hw_06.data.Answer
import com.example.edu_kt_hw_06.data.Question
import com.example.edu_kt_hw_06.data.QuizResult

/**
 * попытка прохождения теста.
 * при повороте/смне вкладки не сбрасывается, т.к. все вкладки её видят.
 * Живет в памяти, при перезапуске сбрасывается, результат пишется в базу только если
 * тест пройден за одну сессию
 */
class QuizSessionViewModel : ViewModel() {

    var questions: List<Question> = emptyList()
        private set

    var questionIndex: Int = 0
        private set

    var score: Int = 0
        private set

    var selectedAnswerIndex: Int = NO_ANSWER

    var lastResult: QuizResult? = null
        private set

    var isStarted: Boolean = false
        private set

    val isFinished: Boolean get() = lastResult != null

    val total: Int get() = questions.size

    val currentQuestion: Question? get() = questions.getOrNull(questionIndex)

    val selectedAnswer: Answer? get() = currentQuestion?.answers?.getOrNull(selectedAnswerIndex)

    fun loadQuestions(questions: List<Question>) {
        if (this.questions.isEmpty()) {
            this.questions = questions
        }
    }

    fun startQuiz() {
        isStarted = true
    }

    fun answerCurrentQuestion(): Boolean {
        val answer = selectedAnswer ?: return false
        if (answer.isCorrect) {
            score++
        }
        questionIndex++
        selectedAnswerIndex = NO_ANSWER
        return questionIndex >= questions.size
    }

    fun finish(result: QuizResult) {
        lastResult = result
    }

    fun restart() {
        clearAttempt()
    }

    fun reset() {
        isStarted = false
        clearAttempt()
    }

    private fun clearAttempt() {
        questionIndex = 0
        score = 0
        selectedAnswerIndex = NO_ANSWER
        lastResult = null
    }

    companion object {
        const val NO_ANSWER: Int = -1
    }
}
