package com.hfad.yultour

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hfad.yultour.databinding.ItemNotificationBinding

class NotificationsAdapter(
    private var notifications: List<NotificationModel>,
    private val onNotificationClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationModel) {
            binding.tvNotificationTitle.text = notification.title
            binding.tvNotificationMessage.text = notification.message
            binding.tvNotificationTime.text = notification.getTimeAgo()

            // Показываем индикатор для непрочитанных
            binding.vUnreadIndicator.visibility =
                if (notification.isRead) View.GONE else View.VISIBLE

            // Устанавливаем иконку в зависимости от типа
            val iconRes = when (notification.type) {
                "booking" -> R.drawable.ic_booking
                "promo" -> R.drawable.ic_promo
                else -> R.drawable.ic_system
            }
            binding.ivNotificationIcon.setImageResource(iconRes)

            // Убираем tint для правильного отображения цвета
            binding.ivNotificationIcon.imageTintList = null

            binding.root.setOnClickListener {
                onNotificationClick(notification)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifications: List<NotificationModel>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}