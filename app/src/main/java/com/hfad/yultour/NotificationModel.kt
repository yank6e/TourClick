package com.hfad.yultour

import com.google.firebase.Timestamp

data class NotificationModel(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // "booking", "promo", "system"
    val isRead: Boolean = false,
    val timestamp: Timestamp = Timestamp.now(),
    val relatedTourId: String? = null,
    val action: String? = null
) {
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val time = timestamp.toDate().time
        val diff = now - time

        return when {
            diff < 60000 -> "Только что"
            diff < 3600000 -> "${diff / 60000} мин назад"
            diff < 86400000 -> "${diff / 3600000} ч назад"
            diff < 604800000 -> "${diff / 86400000} дн назад"
            else -> "${diff / 604800000} нед назад"
        }
    }

    companion object {
        fun getSampleNotifications(): List<NotificationModel> {
            return listOf(
                NotificationModel(
                    id = "1",
                    title = "Бронирование подтверждено!",
                    message = "Ваш тур 'Золотое кольцо России' подтвержден",
                    type = "booking",
                    isRead = false,
                    relatedTourId = "russia_1"
                ),
                NotificationModel(
                    id = "2",
                    title = "Специальное предложение",
                    message = "Скидка 15% на все туры по Карелии",
                    type = "promo",
                    isRead = true
                ),
                NotificationModel(
                    id = "3",
                    title = "Напоминание о туре",
                    message = "Через 3 дня у вас начинается тур 'Байкальская сказка'",
                    type = "system",
                    isRead = false,
                    relatedTourId = "russia_4"
                ),
                NotificationModel(
                    id = "4",
                    title = "Отзыв о туре",
                    message = "Пожалуйста, оставьте отзыв о прошедшем туре",
                    type = "system",
                    isRead = true
                )
            )
        }
    }
}