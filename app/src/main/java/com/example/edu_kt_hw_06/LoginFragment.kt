package com.example.edu_kt_hw_06

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.edu_kt_hw_06.data.AuthPreferences
import com.example.edu_kt_hw_06.data.QuizRepository
import com.example.edu_kt_hw_06.databinding.FragmentLoginBinding

/**
 * Экран входа сделан "двухрежимным". Стартовый экран, при входе в приложение, и потом
 * используем этот экран как профиль пользователя. По Назад на этот экран не возвращаемся.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null

    private val binding get() = _binding!!

    private lateinit var authPreferences: AuthPreferences
    private lateinit var repository: QuizRepository

    private val sessionViewModel: QuizSessionViewModel by activityViewModels()
    private val navController: NavController get() = findNavController()

    /** если true - экран входа открыт как профиль, а не как стартовый экран. */
    private var isProfileMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authPreferences = AuthPreferences(requireContext())
        repository = QuizRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // previousBackStackEntry пропускает графы и возвращает предыдущий экран:
        // если он есть, значит вход открыт поверх вкладок (режим профиля)
        isProfileMode = navController.previousBackStackEntry != null

        // данные входа подставляем из SharedPreferences
        binding.editTextLogin.setText(authPreferences.getLogin())
        binding.editTextPassword.setText(authPreferences.getPassword())
        renderAuthorizedUser()

        binding.buttonLogin.setOnClickListener { onLoginClick() }
        binding.buttonContinue.setOnClickListener { enterMainGraph() }
        binding.buttonLogout.setOnClickListener { onLogoutClick() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderAuthorizedUser() {
        val savedLogin: String? = authPreferences.getLogin()
        val isAuthorized: Boolean = !savedLogin.isNullOrEmpty()

        binding.textViewLoginTitle.text =
            if (isProfileMode) getString(R.string.login_title_profile) else getString(R.string.login_title)

        binding.textInputLayoutLogin.isVisible = !isAuthorized
        binding.textInputLayoutPassword.isVisible = !isAuthorized
        binding.buttonLogin.isVisible = !isAuthorized
        if (isAuthorized) {
            binding.textViewLoginError.isVisible = false
        }

        binding.textViewAuthorized.isVisible = isAuthorized
        // в режиме профиля основной экран уже открыт под фрагментом, кнопку Продолжить не рисуем
        binding.buttonContinue.isVisible = isAuthorized && !isProfileMode
        binding.buttonLogout.isVisible = isAuthorized
        if (isAuthorized) {
            binding.textViewAuthorized.text = getString(R.string.login_authorized_as, savedLogin)
        }
    }

    private fun onLoginClick() {
        val login: String = binding.editTextLogin.text?.toString()?.trim().orEmpty()
        val password: String = binding.editTextPassword.text?.toString().orEmpty()

        binding.textInputLayoutLogin.error = null
        binding.textInputLayoutPassword.error = null
        binding.textViewLoginError.isVisible = false

        if (login.isEmpty()) {
            binding.textInputLayoutLogin.error = getString(R.string.login_error_empty_login)
            return
        }
        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = getString(R.string.login_error_empty_password)
            return
        }

        // проверяем логин и пароль по таблице users
        if (!repository.checkCredentials(login, password)) {
            binding.textViewLoginError.isVisible = true
            return
        }

        authPreferences.save(login, password)
        if (isProfileMode) {
            // профиль открыт поверх вкладок - просто возвращаемся назад
            navController.popBackStack()
            return
        }

        enterMainGraph()
    }

    /** после авторизации граф авторизации убираем из стека, назад в него уже не вернуться, нам это не надо */
    private fun enterMainGraph() {
        sessionViewModel.reset()
        navController.navigate(
            R.id.nav_graph_main,
            null,
            navOptions { popUpTo(R.id.nav_graph_auth) { inclusive = true } }
        )
    }

    private fun onLogoutClick() {
        authPreferences.clear()
        sessionViewModel.reset()

        if (isProfileMode) {
            // перезапускаем приложение с чистого экрана входа  и основной граф убираем из стека
            navController.navigate(
                R.id.nav_graph_auth,
                null,
                navOptions { popUpTo(R.id.nav_graph_main) { inclusive = true } }
            )
            return
        }

        binding.editTextLogin.setText("")
        binding.editTextPassword.setText("")
        binding.textViewLoginError.isVisible = false
        renderAuthorizedUser()
    }
}

