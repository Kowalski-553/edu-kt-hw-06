package com.example.edu_kt_hw_06

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.edu_kt_hw_06.data.Book
import com.example.edu_kt_hw_06.data.UserRole
import com.example.edu_kt_hw_06.databinding.ItemBookBinding

/**
 * Список книг. н акарточке показываем кнопки, доступные по роли (редактирование, бронирование)
 */
class BookAdapter(
    private val role: UserRole,
    private val login: String,
    private val onBookClick: (Book) -> Unit,
    private val onCancelBookingClick: (Book) -> Unit,
    private val onEditClick: (Book) -> Unit,
    private val onDeleteClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            val context: Context = binding.root.context

            binding.textViewTitle.text = book.title
            binding.textViewAuthor.text = book.author
            binding.textViewGenre.text = book.genre

            val isAdmin = role == UserRole.ADMIN
            binding.buttonEdit.isVisible = isAdmin
            binding.buttonDelete.isVisible = isAdmin
            binding.buttonBook.isVisible = !isAdmin

            if (isAdmin) {
                // администратор дополнительно видит, кто забронировал книгу
                binding.textViewBookedBy.isVisible = book.isBooked
                binding.textViewBookedBy.text = book.bookedBy?.let { bookedBy ->
                    context.getString(R.string.library_booked_by, bookedBy)
                }
                binding.buttonEdit.setOnClickListener { onEditClick(book) }
                binding.buttonDelete.setOnClickListener { onDeleteClick(book) }
            } else {
                binding.textViewBookedBy.isVisible = false

                val isMyBooking = book.isBookedBy(login)
                // чужую бронь снять нельзя, поэтому кнопка брони неактивна
                binding.buttonBook.isEnabled = isMyBooking || !book.isBooked
                binding.buttonBook.text = context.getString(
                    when {
                        isMyBooking -> R.string.library_cancel_booking
                        book.isBooked -> R.string.library_booked
                        else -> R.string.library_book
                    }
                )
                binding.buttonBook.setOnClickListener {
                    if (isMyBooking) onCancelBookingClick(book) else onBookClick(book)
                }
            }
        }
    }

    private object BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
            oldItem == newItem
    }
}
