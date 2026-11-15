package com.example.edu_kt_hw_06

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.edu_kt_hw_06.data.AuthPreferences
import com.example.edu_kt_hw_06.data.QuizRepository
import com.example.edu_kt_hw_06.data.QuizResult
import com.example.edu_kt_hw_06.databinding.FragmentResultBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * здесь видим последнюю попытку, берем её из сессии квиза, и можем запросить
 * историю из БД
 */
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null

    private val binding get() = _binding!!

    private lateinit var repository: QuizRepository
    private lateinit var authPreferences: AuthPreferences

    private val sessionViewModel: QuizSessionViewModel by activityViewModels()

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    /** история открывается по кнопке */
    private var isHistoryVisible: Boolean = false

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
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonHistory.setOnClickListener {
            isHistoryVisible = !isHistoryVisible
            renderHistory()
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        // результат мог появиться, пока экран был в back stack
        if (_binding != null) {
            render()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render() {
        val result: QuizResult? = sessionViewModel.lastResult
        binding.textViewScore.isVisible = result != null
        binding.textViewNoResult.isVisible = result == null
        if (result != null) {
            binding.textViewScore.text =
                getString(R.string.result_score, result.score, result.total)
        }
        renderHistory()
    }

    private fun renderHistory() {
        binding.buttonHistory.setText(
            if (isHistoryVisible) R.string.result_history_hide else R.string.result_history_show
        )
        binding.textViewHistoryHeader.isVisible = isHistoryVisible
        binding.layoutHistory.isVisible = isHistoryVisible
        binding.layoutHistory.removeAllViews()
        if (!isHistoryVisible) {
            binding.textViewHistoryEmpty.isVisible = false
            return
        }

        val results: List<QuizResult> = repository.getResults(authPreferences.getLogin().orEmpty())
        binding.textViewHistoryEmpty.isVisible = results.isEmpty()
        results.forEach { result ->
            val row = TextView(requireContext()).apply {
                text = getString(
                    R.string.result_history_row,
                    dateFormat.format(Date(result.createdAt)),
                    result.score,
                    result.total
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, HISTORY_ROW_TEXT_SIZE_SP)
            }
            binding.layoutHistory.addView(row)
        }
    }

    private companion object {
        const val HISTORY_ROW_TEXT_SIZE_SP = 16f
    }
}

