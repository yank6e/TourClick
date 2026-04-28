package com.hfad.yultour

import android.content.Context
import androidx.core.content.ContextCompat

object StatusUtils {

    fun getRussianStatus(status: String): String {
        return when (status.lowercase()) {
            "in_cart" -> "В корзине"
            "confirmed" -> "Подтвержден"
            "completed" -> "Завершен"
            "cancelled" -> "Отменен"
            "pending" -> "Ожидает"
            "processing" -> "В обработке"
            "refunded" -> "Возвращен"
            else -> status
        }
    }

    fun getStatusColor(status: String, context: Context): Int {
        return ContextCompat.getColor(context, when (status.lowercase()) {
            "in_cart" -> R.color.blue_primary
            "confirmed" -> R.color.green_primary
            "completed" -> R.color.green_dark
            "cancelled" -> R.color.red_primary
            "pending" -> R.color.orange_primary
            "processing" -> R.color.blue_primary
            "refunded" -> R.color.purple_primary
            else -> R.color.text_secondary
        })
    }

    fun getStatusBackgroundRes(status: String): Int {
        return when (status.lowercase()) {
            "in_cart" -> R.drawable.status_background_pending
            "confirmed" -> R.drawable.status_background_confirmed
            "completed" -> R.drawable.status_background_completed
            "cancelled" -> R.drawable.status_background_cancelled
            "pending" -> R.drawable.status_background_pending
            "processing" -> R.drawable.status_background_processing
            "refunded" -> R.drawable.status_background_refunded
            else -> R.drawable.status_background
        }
    }

    // Получить информацию о статусе для отображения
    fun getStatusInfo(status: String): Pair<String, Int> {
        val russianStatus = getRussianStatus(status)
        val colorResId = when (status.lowercase()) {
            "in_cart" -> R.color.blue_primary
            "confirmed" -> R.color.green_primary
            "completed" -> R.color.green_dark
            "cancelled" -> R.color.red_primary
            else -> R.color.text_secondary
        }
        return Pair(russianStatus, colorResId)
    }
}