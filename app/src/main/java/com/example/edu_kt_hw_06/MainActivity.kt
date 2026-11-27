package com.example.edu_kt_hw_06

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.fragment.NavHostFragment
import com.example.edu_kt_hw_06.data.AuthPreferences
import com.example.edu_kt_hw_06.data.UserRole
import com.example.edu_kt_hw_06.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var authPreferences: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        authPreferences = AuthPreferences(this)

        // навигацию берём прямо у NavHostFragment, а не у самой активити
        val navHostFragment: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // кнопка справа сверху открывает профиль поверх вкладок, а на самом профиле - закрывает его
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    closeOrOpenProfile()
                    true
                }

                else -> false
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            if (navController.currentDestination?.id != item.itemId) {
                navController.navigateToTab(item.itemId)
            }
            true
        }

        // на экране входа (граф авторизации) нет нижнего меню, а кнопка профиля видна там,
        // когда экран открыт поверх вкладок - тогда она же его и закрывает
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val insideMainGraph: Boolean = destination.isInsideMainGraph()
            val isProfileOverlay: Boolean = !insideMainGraph &&
                destination.isInsideAuthGraph() && navController.previousBackStackEntry != null

            binding.toolbar.title = if (isProfileOverlay) {
                getString(R.string.login_title_profile)
            } else {
                destination.label ?: getString(R.string.app_name)
            }
            binding.bottomNavigationView.isVisible = insideMainGraph
            binding.toolbar.menu.findItem(R.id.action_profile).isVisible =
                insideMainGraph || isProfileOverlay
            if (insideMainGraph) {
                // у каждой роли свой набор вкладок
                applyRoleMenu()
                if (binding.bottomNavigationView.menu.findItem(destination.id)?.isVisible == true) {
                    binding.bottomNavigationView.selectedItemId = destination.id
                } else {
                    // админу доступна только библиотека
                    leaveHiddenScreenIfNeeded()
                }
            }
        }

        // подтверждение выхода по НАЗАД
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.previousBackStackEntry == null) {
                    showExitConfirmationDialog()
                } else {
                    navController.popBackStack()
                }
            }
        })
    }

    /**
     * Показывает профиль поверх текущей вкладки, а если он уже открыт - закрывает его.
     * Так повторное нажатие на иконку не открывает второй экземпляр фрагмента.
     */
    private fun closeOrOpenProfile() {
        if (navController.currentDestination?.id == R.id.loginFragment) {
            navController.popBackStack()
        } else {
            navController.navigate(R.id.nav_graph_auth)
        }
    }

    /**
     * проверяем относится ли экрн к графу с вкладками.
     * [hierarchy] идёт от самого экрана к корню графа, поэтому работает и для вложенных графов.
     */
    private fun NavDestination.isInsideMainGraph(): Boolean =
        hierarchy.any { destination -> destination.id == R.id.nav_graph_main }

    /** проверям, относится ли экран к графу авторизации */
    private fun NavDestination.isInsideAuthGraph(): Boolean =
        hierarchy.any { destination -> destination.id == R.id.nav_graph_auth }

    /**
     * в зависимости от роли меняется меню у пользователя. У админа есть только библиотека.
     * У юзеров видно всё.
     */
    private fun applyRoleMenu() {
        val isAdmin: Boolean = authPreferences.getRole() == UserRole.ADMIN
        binding.bottomNavigationView.menu.apply {
            findItem(R.id.quizFragment).isVisible = !isAdmin
            findItem(R.id.resultFragment).isVisible = !isAdmin
            findItem(R.id.libraryFragment).isVisible = true
        }
    }

    /**
     * Страховка на случай, если администратор как-то попал на недоступный экран.
     * Мало ли чё глюкнет.
     */
    private fun leaveHiddenScreenIfNeeded() {
        binding.bottomNavigationView.post {
            // пока задача ждала очереди, активити могла закрыться
            if (isFinishing || isDestroyed) return@post
            val destinationId: Int = navController.currentDestination?.id ?: return@post
            if (binding.bottomNavigationView.menu.findItem(destinationId)?.isVisible != true) {
                navController.navigateToTab(R.id.libraryFragment)
            }
        }
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.exit_dialog_title)
            .setMessage(R.string.exit_dialog_message)
            .setPositiveButton(R.string.exit_dialog_confirm) { _, _ -> finish() }
            .setNegativeButton(R.string.exit_dialog_cancel, null)
            .show()
    }
}
