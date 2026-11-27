package com.example.edu_kt_hw_06.data

internal object QuizSeed {

    data class SeedAnswer(val text: String, val isCorrect: Boolean)

    data class SeedQuestion(val text: String, val answers: List<SeedAnswer>)

    val questions: List<SeedQuestion> = listOf(
        SeedQuestion(
            text = "Каким оператором в Kotlin делают безопасное приведение типа?",
            answers = listOf(
                SeedAnswer("as?", true),
                SeedAnswer("as", false),
                SeedAnswer("is", false),
                SeedAnswer("typeof", false)
            )
        ),
        SeedQuestion(
            text = "Что объявляет ключевое слово val?",
            answers = listOf(
                SeedAnswer("Неизменяемую ссылку на значение", true),
                SeedAnswer("Изменяемую переменную", false),
                SeedAnswer("Приватное поле класса", false),
                SeedAnswer("Константу времени компиляции", false)
            )
        ),
        SeedQuestion(
            text = "Какая функция создаёт список только для чтения?",
            answers = listOf(
                SeedAnswer("listOf()", true),
                SeedAnswer("mutableListOf()", false),
                SeedAnswer("arrayListOf()", false),
                SeedAnswer("toMutableList()", false)
            )
        ),
        SeedQuestion(
            text = "Какой блок try выполняется всегда, при любом исходе?",
            answers = listOf(
                SeedAnswer("finally", true),
                SeedAnswer("catch", false),
                SeedAnswer("else", false),
                SeedAnswer("init", false)
            )
        ),
        SeedQuestion(
            text = "Каким словом помечают переопределённый метод в Kotlin?",
            answers = listOf(
                SeedAnswer("override", true),
                SeedAnswer("open", false),
                SeedAnswer("final", false),
                SeedAnswer("virtual", false)
            )
        )
    )
}
