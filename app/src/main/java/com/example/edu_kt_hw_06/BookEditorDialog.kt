package com.example.edu_kt_hw_06

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.edu_kt_hw_06.data.Book
import com.example.edu_kt_hw_06.databinding.DialogBookEditorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * диалго добавления/редактирования книги
 */
class BookEditorDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogBookEditorBinding.inflate(layoutInflater)
        val arguments = requireArguments()
        val bookId = arguments.getLong(ARG_BOOK_ID, NEW_BOOK_ID)

        binding.editTextTitle.setText(arguments.getString(ARG_TITLE).orEmpty())
        binding.editTextAuthor.setText(arguments.getString(ARG_AUTHOR).orEmpty())
        binding.editTextGenre.setText(arguments.getString(ARG_GENRE).orEmpty())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                if (bookId == NEW_BOOK_ID) R.string.library_add_title
                else R.string.library_edit_title
            )
            .setView(binding.root)
            .setPositiveButton(R.string.library_save, null)
            .setNegativeButton(R.string.library_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                if (validate(binding)) {
                    val result = Bundle().apply {
                        putLong(ARG_BOOK_ID, bookId)
                        putString(ARG_TITLE, binding.editTextTitle.text?.toString()?.trim())
                        putString(ARG_AUTHOR, binding.editTextAuthor.text?.toString()?.trim())
                        putString(ARG_GENRE, binding.editTextGenre.text?.toString()?.trim())
                    }
                    parentFragmentManager.setFragmentResult(REQUEST_KEY, result)
                    dismiss()
                }
            }
        }
        return dialog
    }

    /** проверяем полей перед отправкой */
    private fun validate(binding: DialogBookEditorBinding): Boolean {
        val title = binding.editTextTitle.text?.toString()?.trim().orEmpty()
        val author = binding.editTextAuthor.text?.toString()?.trim().orEmpty()
        val genre = binding.editTextGenre.text?.toString()?.trim().orEmpty()

        binding.textInputLayoutTitle.error =
            if (title.isEmpty()) getString(R.string.library_error_title) else null
        binding.textInputLayoutAuthor.error =
            if (author.isEmpty()) getString(R.string.library_error_author) else null
        binding.textInputLayoutGenre.error =
            if (genre.isEmpty()) getString(R.string.library_error_genre) else null

        return title.isNotEmpty() && author.isNotEmpty() && genre.isNotEmpty()
    }

    companion object {
        const val REQUEST_KEY = "book_editor_result"
        const val TAG = "BookEditorDialog"

        // ключи аргументов и результата
        const val ARG_BOOK_ID = "book_id"
        const val ARG_TITLE = "title"
        const val ARG_AUTHOR = "author"
        const val ARG_GENRE = "genre"

        /** id для новой книги, в SQLite начинаются с 1 */
        const val NEW_BOOK_ID = 0L

        fun newInstance(book: Book?): BookEditorDialog = BookEditorDialog().apply {
            arguments = Bundle().apply {
                putLong(ARG_BOOK_ID, book?.id ?: NEW_BOOK_ID)
                putString(ARG_TITLE, book?.title ?: "")
                putString(ARG_AUTHOR, book?.author ?: "")
                putString(ARG_GENRE, book?.genre ?: "")
            }
        }
    }
}
