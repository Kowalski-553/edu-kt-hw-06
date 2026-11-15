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
import com.example.edu_kt_hw_06.data.AuthPreferences
import com.example.edu_kt_hw_06.data.Question
import com.example.edu_kt_hw_06.data.QuizRepository
import com.example.edu_kt_hw_06.data.QuizResult
import com.example.edu_kt_hw_06.databinding.FragmentQuizBinding
import com.google.android.material.radiobutton.MaterialRadioButton
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
        navigateToResultAfterDelay()
    }

    private fun onRestartClick() {
        sessionViewModel.restart()
        renderSession()
    }

    /** здесь потом будет анимация. Сейчас пока просто задерживаемся на 1 секунду посе прохождения
     * и потом автоматически уходим на экран результата */
    private fun navigateToResultAfterDelay() {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(RESULT_NAVIGATION_DELAY_MS)
            // пользователь мог за секунду уйти на другую вкладку или начать заново -
            // тогда не мешаем ему и остаёмся на месте
            val keepWaiting: Boolean = sessionViewModel.isFinished &&
                navController.currentDestination?.id == R.id.quizFragment
            if (isAdded && keepWaiting) {
                navController.navigateToTab(R.id.resultFragment)
            }
        }
    }

    private companion object {
        const val RESULT_NAVIGATION_DELAY_MS = 1_000L
        const val ANSWERS_MARGIN_DP = 8
    }
}

