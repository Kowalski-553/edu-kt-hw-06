package com.example.edu_kt_hw_06

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.edu_kt_hw_06.data.AuthPreferences
import com.example.edu_kt_hw_06.data.Book
import com.example.edu_kt_hw_06.data.UserRole
import com.example.edu_kt_hw_06.databinding.FragmentLibraryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

/**
 * Экран библиотеки.
 * Общий для всех ролей, но права проверяются не флагами внутри экрана,
 * а тем, какой фасад вернул LibraryFactory.
 */
class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var authPreferences: AuthPreferences
    private val viewModel: LibraryViewModel by viewModels()

    /** адаптер пересоздаём при смене роли */
    private var adapter: BookAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authPreferences = AuthPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewBooks.layoutManager = LinearLayoutManager(requireContext())
        setupTabs()
        binding.fabAddBook.setOnClickListener { showBookEditor(book = null) }

        // обрабатываем результат работы диалога добавления
        childFragmentManager.setFragmentResultListener(
            BookEditorDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result -> onBookSubmitted(result) }
    }

    override fun onResume() {
        super.onResume()
        // при перелогине пересобираем экран
        applyRoleIfChanged()
        renderBooks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewBooks.adapter = null
        adapter = null
        _binding = null
    }

    private fun setupTabs() {
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.library_tab_all))
        binding.tabLayout.addTab(
            binding.tabLayout.newTab().setText(
                // у администратора это чужие брони, у юзеров свои
                if (authPreferences.getRole() == UserRole.ADMIN) R.string.library_tab_booked
                else R.string.library_tab_my
            )
        )
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = renderBooks()

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun applyRoleIfChanged() {
        val login = authPreferences.getLogin().orEmpty()
        val role = authPreferences.getRole()
        if (viewModel.login == login && viewModel.role == role && adapter != null) return

        // роль сменилась прямо на этом экране (выход и вход под другим логином)
        val roleChanged = adapter != null && (viewModel.login != login || viewModel.role != role)

        viewModel.start(login, role)

        binding.fabAddBook.isVisible = viewModel.isAdmin
        binding.tabLayout.getTabAt(LibraryViewModel.SECOND_TAB)?.setText(
            if (viewModel.isAdmin) R.string.library_tab_booked else R.string.library_tab_my
        )

        adapter = BookAdapter(
            role = role,
            login = login,
            onBookClick = ::bookBook,
            onCancelBookingClick = ::cancelBooking,
            onEditClick = ::showBookEditor,
            onDeleteClick = ::confirmDelete
        )
        binding.recyclerViewBooks.adapter = adapter

        // при повороте экрана выбранный таб восстанавливает сам TabLayout, сбрасываем его только при смене роли
        if (roleChanged) {
            binding.tabLayout.getTabAt(LibraryViewModel.ALL_BOOKS_TAB)?.select()
        }
    }

    private fun renderBooks() {
        val tabIndex = binding.tabLayout.selectedTabPosition
            .coerceAtLeast(LibraryViewModel.ALL_BOOKS_TAB)
        val books = if (adapter == null) emptyList() else viewModel.books(tabIndex)

        adapter?.submitList(books)
        binding.textViewEmpty.isVisible = books.isEmpty()
        binding.textViewEmpty.setText(emptyTextRes(tabIndex))
    }

    @StringRes
    private fun emptyTextRes(tabIndex: Int): Int = when {
        tabIndex != LibraryViewModel.SECOND_TAB -> R.string.library_empty_all
        viewModel.isAdmin -> R.string.library_empty_booked
        else -> R.string.library_empty_my
    }

    private fun bookBook(book: Book) {
        val booked = viewModel.book(book.id)
        renderBooks()
        showMessage(if (booked) R.string.library_book_success else R.string.library_book_failed)
    }

    private fun cancelBooking(book: Book) {
        viewModel.cancelBooking(book.id)
        renderBooks()
        showMessage(R.string.library_cancel_success)
    }

    private fun confirmDelete(book: Book) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_delete_title)
            .setMessage(getString(R.string.library_delete_message, book.title))
            .setPositiveButton(R.string.library_delete_confirm) { _, _ ->
                viewModel.deleteBook(book.id)
                renderBooks()
            }
            .setNegativeButton(R.string.library_delete_cancel, null)
            .show()
    }

    private fun showBookEditor(book: Book?) {
        BookEditorDialog.newInstance(book)
            .show(childFragmentManager, BookEditorDialog.TAG)
    }

    private fun onBookSubmitted(result: Bundle) {
        val bookId = result.getLong(BookEditorDialog.ARG_BOOK_ID, BookEditorDialog.NEW_BOOK_ID)
        val title = result.getString(BookEditorDialog.ARG_TITLE).orEmpty()
        val author = result.getString(BookEditorDialog.ARG_AUTHOR).orEmpty()
        val genre = result.getString(BookEditorDialog.ARG_GENRE).orEmpty()

        if (bookId == BookEditorDialog.NEW_BOOK_ID) {
            viewModel.addBook(title, author, genre)
        } else {
            viewModel.updateBook(bookId, title, author, genre)
        }
        renderBooks()
    }

    private fun showMessage(@StringRes messageRes: Int) {
        Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_SHORT).show()
    }
}
