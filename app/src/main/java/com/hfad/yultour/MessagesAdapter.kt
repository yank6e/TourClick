package com.hfad.yultour

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hfad.yultour.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(
    private var messages: List<SupportMessage>
) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_SUPPORT = 2
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: SupportMessage) {
            binding.tvMessage.text = message.message

            // Форматируем время
            val date = message.timestamp.toDate()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.tvTime.text = timeFormat.format(date)

            // Настраиваем отображение в зависимости от типа сообщения
            if (message.isUser) {
                // Сообщение пользователя - справа
                binding.messageCard.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.green_light)
                )
                (binding.messageCard.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                    marginStart = 80.dpToPx(binding.root.context)
                    marginEnd = 16.dpToPx(binding.root.context)
                }
                binding.tvMessage.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_primary)
                )
            } else {
                // Сообщение поддержки - слева
                binding.messageCard.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.gray_light)
                )
                (binding.messageCard.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                    marginStart = 16.dpToPx(binding.root.context)
                    marginEnd = 80.dpToPx(binding.root.context)
                }
                binding.tvMessage.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_primary)
                )
            }

            // Принудительно запрашиваем перерисовку
            binding.messageCard.requestLayout()
        }

        private fun Int.dpToPx(context: Context): Int {
            return (this * context.resources.displayMetrics.density).toInt()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_SUPPORT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<SupportMessage>) {
        messages = newMessages
        notifyDataSetChanged()
        Log.d("MessagesAdapter", "Обновлено ${newMessages.size} сообщений")
    }
}