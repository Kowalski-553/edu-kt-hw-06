package com.example.edu_kt_hw_06

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.edu_kt_hw_06.databinding.FragmentFormBinding

class FormFragment : Fragment() {

    private var _binding: FragmentFormBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // когда жмем кнопку, обрабатываем через ViewBinding
        binding.buttonSubmit.setOnClickListener {
            val inputText: String = binding.editTextName.text.toString()
            binding.textViewResult.text = "Привет, $inputText!"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // очищаем байндинг
        _binding = null
    }
}