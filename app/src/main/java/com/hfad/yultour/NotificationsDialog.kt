package com.hfad.yultour

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hfad.yultour.databinding.NotificationsDialogBinding

class NotificationsDialog : BottomSheetDialogFragment() {

    private var _binding: NotificationsDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        _binding = NotificationsDialogBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()

        return dialog
    }

    private fun setupRecyclerView() {
        val notifications = NotificationModel.getSampleNotifications()
        adapter = NotificationsAdapter(notifications) { notification ->
            onNotificationClicked(notification)
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter

        // Показываем количество непрочитанных в заголовке
        val unreadCount = notifications.count { !it.isRead }
        if (unreadCount > 0) {
            binding.tvMarkAllRead.text = "Прочитать все ($unreadCount)"
        }
    }

    private fun setupClickListeners() {
        binding.tvMarkAllRead.setOnClickListener {
            markAllAsRead()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun onNotificationClicked(notification: NotificationModel) {
        when (notification.type) {
            "booking" -> {
                Toast.makeText(requireContext(), "Переход к бронированию", Toast.LENGTH_SHORT).show()
                // Можно добавить навигацию к деталям тура
            }
            "promo" -> {
                Toast.makeText(requireContext(), "Просмотр акции", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), "Уведомление открыто", Toast.LENGTH_SHORT).show()
            }
        }
        dismiss()
    }

    private fun markAllAsRead() {
        val updatedNotifications = NotificationModel.getSampleNotifications().map {
            it.copy(isRead = true)
        }
        adapter.updateNotifications(updatedNotifications)

        // Обновляем текст кнопки
        binding.tvMarkAllRead.text = "Прочитать все"

        Toast.makeText(requireContext(), "Все уведомления прочитаны", Toast.LENGTH_SHORT).show()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // Отправляем результат обратно в фрагмент
        parentFragmentManager.setFragmentResult("notifications_dismissed", Bundle())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}