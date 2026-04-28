package com.hfad.yultour

import com.google.firebase.Timestamp

data class BookingModel(
    val id: String = "",
    val userId: String = "",
    val tourId: String = "",
    val tourTitle: String = "",
    val bookingDate: Timestamp = Timestamp.now(),
    val tourDate: String = "", // ДАТА ТУРА (формат: "24 нояб., пн")
    val tourTime: String = "", // ВРЕМЯ ТУРА (формат: "09:00")
    val seatNumbers: List<String> = emptyList(),
    val passengersCount: Int = 1,
    val totalPrice: Double = 0.0,
    val status: String = "in_cart",
    val duration: String = ""
) {
    companion object {
        fun fromMap(map: Map<String, Any>): BookingModel {
            return BookingModel(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                tourId = map["tourId"] as? String ?: "",
                tourTitle = map["tourTitle"] as? String ?: "",
                bookingDate = map["bookingDate"] as? Timestamp ?: Timestamp.now(),
                tourDate = map["tourDate"] as? String ?: "", // ЧИТАЕМ ДАТУ ТУРА
                tourTime = map["tourTime"] as? String ?: "", // ЧИТАЕМ ВРЕМЯ ТУРА
                seatNumbers = (map["seatNumbers"] as? List<String>) ?: emptyList(),
                passengersCount = (map["passengersCount"] as? Long)?.toInt() ?: 1,
                totalPrice = (map["totalPrice"] as? Double) ?: 0.0,
                status = map["status"] as? String ?: "in_cart",
                duration = map["duration"] as? String ?: ""
            )
        }
    }

    // Функция для преобразования в Map для Firebase
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "tourId" to tourId,
            "tourTitle" to tourTitle,
            "bookingDate" to bookingDate,
            "tourDate" to tourDate,
            "tourTime" to tourTime,
            "seatNumbers" to seatNumbers,
            "passengersCount" to passengersCount,
            "totalPrice" to totalPrice,
            "status" to status,
            "duration" to duration
        )
    }
}