package com.example.edu_kt_hw_06

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.example.edu_kt_hw_06.data.AuthPreferences
import com.example.edu_kt_hw_06.data.Question
import com.example.edu_kt_hw_06.data.QuizRepository
import com.example.edu_kt_hw_06.data.QuizResult
import com.example.edu_kt_hw_06.databinding.FragmentQuizBinding
import com.google.android.material.color.MaterialColors
import com.google.android.material.radiobutton.MaterialRadioButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Экран прохождения теста. Чтобы прогресс не сбрасывался при повороте экрана или
 * переключении вкладок, храним прогресс в QuizSessionViewModel. Результат в базу
 * пишем только когда тест завершен
 */
class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null

    private val binding get() = _binding!!

    private lateinit var repository: QuizRepository
    private lateinit var authPreferences: AuthPreferences

    private val sessionViewModel: QuizSessionViewModel by activityViewModels()
    private val navController: NavController get() = findNavController()

    /** джоба отсчёта до экрана результатов, мы её отменяем, если экран закрыли или тест перезапустили */
    private var countdownJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = QuizRepository(requireContext())
        authPreferences = AuthPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // варианты ответа пересоздаются на каждом вопросе, поэтому слушатель вешаем один раз
        binding.radioGroupAnswers.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) return@setOnCheckedChangeListener
            sessionViewModel.selectedAnswerIndex = group.indexOfChild(group.findViewById(checkedId))
            binding.buttonNext.isEnabled = true
        }
        binding.buttonNext.setOnClickListener { onNextClick() }
        binding.buttonStart.setOnClickListener { onStartClick() }
        binding.buttonRestart.setOnClickListener { onRestartClick() }

        if (sessionViewModel.questions.isEmpty()) {
            sessionViewModel.loadQuestions(repository.getQuestions())
        }
        renderSession()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownJob?.cancel()
        countdownJob = null
        _binding = null
    }

    private fun renderSession() {
        // пока пользователь не начал тест, показываем вводный экран
        if (!sessionViewModel.isStarted) {
            renderIntro()
            return
        }
        if (sessionViewModel.isFinished) {
            renderFinished()
            return
        }
        val question: Question? = sessionViewModel.currentQuestion
        if (question == null) {
            // защищаемся, если в БД нет вопросов
            binding.layoutIntro.isVisible = false
            binding.layoutQuestion.isVisible = false
            binding.layoutFinished.isVisible = true
            binding.buttonRestart.isVisible = false
            binding.textViewFinished.setText(R.string.quiz_empty)
            hideResultCountdown()
            return
        }
        renderQuestion(question)
    }

    /** вводный экран перед тестов. Описание + кнопка начала */
    private fun renderIntro() {
        binding.layoutIntro.isVisible = true
        binding.layoutQuestion.isVisible = false
        binding.layoutFinished.isVisible = false

        val total: Int = sessionViewModel.total
        binding.textViewIntroDescription.text = if (total > 0) {
            getString(R.string.quiz_intro_description, total)
        } else {
            getString(R.string.quiz_empty)
        }
        binding.buttonStart.isEnabled = total > 0
    }

    private fun renderQuestion(question: Question) {
        binding.layoutIntro.isVisible = false
        binding.layoutQuestion.isVisible = true
        binding.layoutFinished.isVisible = false

        val total: Int = sessionViewModel.total
        val position: Int = sessionViewModel.questionIndex + 1
        binding.textViewCounter.text = getString(R.string.quiz_counter, position, total)
        binding.progressIndicator.max = total
        binding.progressIndicator.setProgressCompat(position, true)
        binding.textViewQuestion.text = question.text

        // на последнем вопросе кнопка говорит, что дальше не следующий вопрос, а итог
        binding.buttonNext.setText(
            if (position == total) R.string.quiz_finish else R.string.quiz_next
        )

        // варианты ответа создаём кодом, их количество приходит из базы
        binding.radioGroupAnswers.removeAllViews()
        val verticalMargin: Int = (ANSWERS_MARGIN_DP * resources.displayMetrics.density).toInt()
        question.answers.forEachIndexed { index, answer ->
            val radioButton = MaterialRadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = answer.text
                isChecked = index == sessionViewModel.selectedAnswerIndex
            }
            val layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = verticalMargin }
            binding.radioGroupAnswers.addView(radioButton, layoutParams)
        }

        // без выбранного ответа дальше не пускаем
        binding.buttonNext.isEnabled =
            sessionViewModel.selectedAnswerIndex != QuizSessionViewModel.NO_ANSWER
    }

    private fun renderFinished() {
        binding.layoutIntro.isVisible = false
        binding.layoutQuestion.isVisible = false
        binding.layoutFinished.isVisible = true
        binding.buttonRestart.isVisible = true
        binding.textViewFinished.setText(R.string.quiz_finished)
        // отсчёт показываем только пока переход на экран результата ещё не выполнен
        if (sessionViewModel.isResultNavigationPending) {
            startResultCountdown()
        } else {
            hideResultCountdown()
        }
    }

    private fun onStartClick() {
        sessionViewModel.startQuiz()
        renderSession()
    }

    private fun onNextClick() {
        val isLastQuestion: Boolean = sessionViewModel.answerCurrentQuestion()
        if (!isLastQuestion) {
            renderSession()
            return
        }

        // ответили на последний вопрос: сохраняем попытку в базу и показываем итог
        val result: QuizResult = repository.saveResult(
            login = authPreferences.getLogin().orEmpty(),
            score = sessionViewModel.score,
            total = sessionViewModel.total
        )
        sessionViewModel.finish(result)
        renderFinished()
    }

    private fun onRestartClick() {
        // новый заход: старый отсчёт и его переход на результат больше не нужны
        hideResultCountdown()
        sessionViewModel.restart()
        renderSession()
    }

    /**
     * Отсчёт до экрана результатов: Lottie считает в обратном порядке от 5,
     * после чего автоматически открываем вкладку результата.
     */
    private fun startResultCountdown() {
        val animation = binding.animationCountdown
        binding.textViewCountdownLabel.isVisible = true
        animation.isVisible = true
        // в JSON зашит базовый colorPrimary, в тёмной теме подменяем цвет на актуальный из темы
        val primaryColor: Int = MaterialColors.getColor(
            animation,
            com.google.android.material.R.attr.colorPrimary
        )
        animation.addValueCallback(KeyPath("**"), LottieProperty.COLOR, LottieValueCallback(primaryColor))
        animation.addValueCallback(
            KeyPath("**"),
            LottieProperty.STROKE_COLOR,
            LottieValueCallback(primaryColor)
        )
        animation.progress = 0f
        animation.playAnimation()

        countdownJob?.cancel()
        countdownJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(RESULT_COUNTDOWN_MS)
            // пользователь мог уйти на другую вкладку или начать тест заново -
            // тогда не мешаем ему и остаёмся на месте
            val keepWaiting: Boolean = sessionViewModel.isResultNavigationPending &&
                navController.currentDestination?.id == R.id.quizFragment
            if (isAdded && keepWaiting) {
                sessionViewModel.consumeResultNavigation()
                navController.navigateToTab(R.id.resultFragment)
            }
        }
    }

    private fun hideResultCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        binding.textViewCountdownLabel.isVisible = false
        binding.animationCountdown.apply {
            cancelAnimation()
            isVisible = false
        }
    }

    private companion object {
        const val RESULT_COUNTDOWN_MS = 5_000L
        const val ANSWERS_MARGIN_DP = 8
    }
}

