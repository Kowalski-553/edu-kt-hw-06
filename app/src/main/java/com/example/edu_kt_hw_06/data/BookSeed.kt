package com.example.edu_kt_hw_06.data

internal object BookSeed {

    data class SeedBook(
        val title: String,
        val author: String,
        val genre: String,
        val bookedBy: String? = null
    )

    val books: List<SeedBook> = listOf(
        SeedBook("Kotlin в действии", "Дмитрий Жемеров, Светлана Исакова", "Kotlin", bookedBy = "user"),
        SeedBook("Эффективный Kotlin", "Марчин Москала", "Kotlin"),
        SeedBook("Kotlin. Программирование для профессионалов", "Джош Смит, Марчин Москала", "Kotlin"),
        SeedBook("Android. Программирование для профессионалов", "Билл Филлипс, Крис Стюарт", "Android"),
        SeedBook("Чистый код. Создание, анализ и рефакторинг", "Роберт Мартин", "Разработка ПО"),
        SeedBook("Грокаем алгоритмы", "Адитья Бхаргава", "Алгоритмы", bookedBy = "maria")
    )
}
