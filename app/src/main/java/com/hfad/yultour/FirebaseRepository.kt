package com.hfad.yultour

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseRepository @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

    // Туры
    suspend fun getAllTours(): List<TourModel> {
        return try {
            val snapshot = db.collection("tours").get().await()
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.map { doc ->
                    TourModel.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
                }
            } else {
                // Если в Firebase нет данных - пустой список
                emptyList()
            }
        } catch (e: Exception) {
            // В случае ошибки - пустой список
            emptyList()
        }
    }

    suspend fun searchTours(
        fromCity: String? = null,
        toCity: String? = null,
        date: String? = null,
        peopleCount: Int? = null
    ): List<TourModel> {
        return try {
            val allTours = getAllTours()

            // Применяем фильтры
            allTours.filter { tour ->
                var matches = true

                // Фильтр по городу отправления
                fromCity?.let { from ->
                    if (from.isNotEmpty() && from != "Current location") {
                        val searchQuery = from.lowercase()
                        matches = matches && tour.startPoint.lowercase().contains(searchQuery)
                    }
                }

                // Фильтр по городу назначения
                toCity?.let { to ->
                    if (to.isNotEmpty() && to != "Destination city") {
                        val searchQuery = to.lowercase()
                        matches = matches && (
                                tour.title.lowercase().contains(searchQuery) ||
                                        tour.description.lowercase().contains(searchQuery) ||
                                        tour.stops.lowercase().contains(searchQuery)
                                )
                    }
                }

                // Фильтр по дате (упрощенная логика)
                date?.let {
                    matches = matches && isTourAvailableOnDate(tour, date)
                }

                // Фильтр по количеству людей
                peopleCount?.let {
                    matches = matches && tour.maxPassengers >= peopleCount
                }

                matches
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isTourAvailableOnDate(tour: TourModel, date: String): Boolean {
        // Упрощенная логика - в реальном приложении нужно проверять доступность даты
        return true
    }

    suspend fun getTourById(tourId: String): TourModel? {
        return try {
            val doc = db.collection("tours").document(tourId).get().await()
            if (doc.exists()) {
                val data = doc.data ?: emptyMap()
                TourModel(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    price = (data["price"] as? Double) ?: 0.0,
                    duration = data["duration"] as? String ?: "",
                    rating = (data["rating"] as? Double) ?: 0.0,
                    startPoint = data["startPoint"] as? String ?: "",
                    stops = data["stops"] as? String ?: "",
                    highlights = (data["highlights"] as? List<String>) ?: emptyList(),
                    guideId = data["guideId"] as? String ?: "",
                    imageResource = data["imageResource"] as? String ?: "",
                    availableTimes = (data["availableTimes"] as? List<String>) ?: emptyList(), // Важно!
                    maxPassengers = (data["maxPassengers"] as? Long)?.toInt() ?: 0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getBookingById(bookingId: String): BookingModel? {
        return try {
            val doc = db.collection("bookings").document(bookingId).get().await()
            if (doc.exists()) {
                BookingModel.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Избранное
    suspend fun addToFavorites(userId: String, tourId: String) {
        db.collection("favorites")
            .document("${userId}_$tourId")
            .set(mapOf(
                "userId" to userId,
                "tourId" to tourId,
                "addedAt" to Timestamp.now()
            )).await()
    }

    suspend fun removeFromFavorites(userId: String, tourId: String) {
        db.collection("favorites")
            .document("${userId}_$tourId")
            .delete()
            .await()
    }

    suspend fun getUserFavorites(userId: String): List<TourModel> {
        return try {
            val snapshot = db.collection("favorites")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val tourIds = snapshot.documents.map { it.getString("tourId") ?: "" }

            if (tourIds.isEmpty()) return emptyList()

            // Получаем все туры по списку ID
            val tours = mutableListOf<TourModel>()
            tourIds.forEach { tourId ->
                if (tourId.isNotEmpty()) {
                    val tourDoc = db.collection("tours").document(tourId).get().await()
                    if (tourDoc.exists()) {
                        val tour = TourModel.fromMap(tourDoc.data ?: emptyMap()).copy(id = tourDoc.id)
                        tours.add(tour)
                    }
                }
            }

            tours
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun isTourFavorite(userId: String, tourId: String): Boolean {
        return try {
            val doc = db.collection("favorites")
                .document("${userId}_$tourId")
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    // Бронирования
    suspend fun createBooking(booking: BookingModel): String {
        val docRef = db.collection("bookings").document()
        val bookingWithId = booking.copy(id = docRef.id)
        docRef.set(bookingWithId).await()
        return docRef.id
    }

    suspend fun getOccupiedSeats(tourId: String, date: String, time: String): List<String> {
        return try {
            val snapshot = db.collection("occupied_seats")
                .whereEqualTo("tourId", tourId)
                .whereEqualTo("date", date)
                .whereEqualTo("time", time)
                .get()
                .await()

            snapshot.documents.flatMap { doc ->
                (doc["seats"] as? List<String>) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun reserveSeats(tourId: String, date: String, time: String, seats: List<String>, userId: String) {
        val reservationData = mapOf(
            "tourId" to tourId,
            "date" to date,
            "time" to time,
            "seats" to seats,
            "userId" to userId,
            "reservedAt" to Timestamp.now()
        )

        db.collection("occupied_seats").add(reservationData).await()
    }

    // Добавляем в FirebaseRepository
    // В классе FirebaseRepository добавьте эту функцию:
    suspend fun updateBooking(booking: BookingModel) {
        try {
            val bookingMap = mapOf(
                "id" to booking.id,
                "userId" to booking.userId,
                "tourId" to booking.tourId,
                "tourTitle" to booking.tourTitle,
                "bookingDate" to booking.bookingDate,
                "tourDate" to booking.tourDate, // Сохраняем дату тура
                "tourTime" to booking.tourTime, // Сохраняем время тура
                "seatNumbers" to booking.seatNumbers,
                "passengersCount" to booking.passengersCount,
                "totalPrice" to booking.totalPrice,
                "status" to booking.status,
                "duration" to booking.duration
            )

            db.collection("bookings").document(booking.id).set(bookingMap).await()
            Log.d("FirebaseRepository", "Бронирование обновлено: ${booking.id}")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Ошибка обновления бронирования: ${e.message}")
            throw e
        }
    }

    suspend fun getUserBookings(userId: String): List<BookingModel> {
        return try {
            val snapshot = db.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents.map { doc ->
                BookingModel.fromMap(doc.data ?: emptyMap()).copy(id = doc.id)
            }.sortedByDescending { it.bookingDate }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Чат поддержки
    suspend fun sendSupportMessage(userId: String, message: String, isUser: Boolean = true) {
        db.collection("support_messages").add(
            mapOf(
                "userId" to userId,
                "message" to message,
                "isUser" to isUser,
                "timestamp" to Timestamp.now()
            )
        ).await()
    }

    fun getSupportMessagesFlow(userId: String): Flow<List<SupportMessage>> {
        return db.collection("support_messages")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                Log.d("FirebaseRepository", "Snapshot size: ${snapshot.documents.size}")
                snapshot.documents.map { doc ->
                    SupportMessage(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        message = doc.getString("message") ?: "",
                        isUser = doc.getBoolean("isUser") ?: true,
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    )
                }
            }
            .catch { e ->
                Log.e("FirebaseRepository", "Error in flow: ${e.message}", e)
                emit(emptyList())
            }
    }

    suspend fun getSupportMessages(userId: String): List<SupportMessage> {
        return try {
            val snapshot = db.collection("support_messages")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.map { doc ->
                SupportMessage(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    message = doc.getString("message") ?: "",
                    isUser = doc.getBoolean("isUser") ?: true,
                    timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Профиль пользователя
    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                UserProfile.fromMap(doc.data ?: emptyMap())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserProfile(userId: String, profile: UserProfile) {
        db.collection("users").document(userId).set(profile.toMap()).await()
    }
}

data class SupportMessage(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val isUser: Boolean = true,
    val timestamp: Timestamp = Timestamp.now()
)

data class UserProfile(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val memberSince: Int = 2022,
    val documents: List<UserDocument> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "phone" to phone,
            "memberSince" to memberSince,
            "documents" to documents.map { it.toMap() }
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): UserProfile {
            return UserProfile(
                userId = map["userId"] as? String ?: "",
                firstName = map["firstName"] as? String ?: "",
                lastName = map["lastName"] as? String ?: "",
                email = map["email"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                memberSince = (map["memberSince"] as? Long)?.toInt() ?: 2022,
                documents = (map["documents"] as? List<Map<String, Any>>)?.map {
                    UserDocument.fromMap(it)
                } ?: emptyList()
            )
        }
    }
}

@Parcelize
data class UserDocument(
    val type: String = "",

    // Общие поля
    val number: String = "",
    val expiryDate: String = "",
    val details: String = "",

    // Специфичные поля для паспорта
    val passportSeries: String = "",      // Серия паспорта (4 цифры)
    val passportNumber: String = "",      // Номер паспорта (6 цифр)
    val passportIssuedBy: String = "",    // Кем выдан
    val passportIssueDate: String = "",   // Дата выдачи (ДД.ММ.ГГГГ)
    val passportDepartmentCode: String = "", // Код подразделения (XXX-XXX)
    val passportBirthDate: String = "",   // Дата рождения (ДД.ММ.ГГГГ)
    val passportBirthPlace: String = "",  // Место рождения

    // Специфичные поля для водительских прав
    val licenseCategory: String = "",     // Категории прав (A,B,C и т.д.)
    val licenseIssueDate: String = "",    // Дата выдачи

    // Специфичные поля для страховки
    val insuranceCompany: String = "",    // Страховая компания
    val insurancePolicyNumber: String = "" // Номер полиса
) : Parcelable {

    fun toMap(): Map<String, Any> {
        return mapOf(
            "type" to type,
            "number" to number,
            "expiryDate" to expiryDate,
            "details" to details,
            "passportSeries" to passportSeries,
            "passportNumber" to passportNumber,
            "passportIssuedBy" to passportIssuedBy,
            "passportIssueDate" to passportIssueDate,
            "passportDepartmentCode" to passportDepartmentCode,
            "passportBirthDate" to passportBirthDate,
            "passportBirthPlace" to passportBirthPlace,
            "licenseCategory" to licenseCategory,
            "licenseIssueDate" to licenseIssueDate,
            "insuranceCompany" to insuranceCompany,
            "insurancePolicyNumber" to insurancePolicyNumber
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): UserDocument {
            return UserDocument(
                type = map["type"] as? String ?: "",
                number = map["number"] as? String ?: "",
                expiryDate = map["expiryDate"] as? String ?: "",
                details = map["details"] as? String ?: "",
                passportSeries = map["passportSeries"] as? String ?: "",
                passportNumber = map["passportNumber"] as? String ?: "",
                passportIssuedBy = map["passportIssuedBy"] as? String ?: "",
                passportIssueDate = map["passportIssueDate"] as? String ?: "",
                passportDepartmentCode = map["passportDepartmentCode"] as? String ?: "",
                passportBirthDate = map["passportBirthDate"] as? String ?: "",
                passportBirthPlace = map["passportBirthPlace"] as? String ?: "",
                licenseCategory = map["licenseCategory"] as? String ?: "",
                licenseIssueDate = map["licenseIssueDate"] as? String ?: "",
                insuranceCompany = map["insuranceCompany"] as? String ?: "",
                insurancePolicyNumber = map["insurancePolicyNumber"] as? String ?: ""
            )
        }
    }

    // Функции для форматированного отображения
    fun getFormattedPassportNumber(): String {
        return if (passportSeries.isNotEmpty() && passportNumber.isNotEmpty()) {
            "$passportSeries $passportNumber"
        } else {
            number
        }
    }

    fun getFormattedDepartmentCode(): String {
        return if (passportDepartmentCode.length == 6) {
            "${passportDepartmentCode.substring(0, 3)}-${passportDepartmentCode.substring(3)}"
        } else {
            passportDepartmentCode
        }
    }

    fun getFormattedExpiryDate(): String {
        return if (expiryDate.length == 8) {
            "${expiryDate.substring(0, 2)}.${expiryDate.substring(2, 4)}.${expiryDate.substring(4)}"
        } else {
            expiryDate
        }
    }

    fun getFullPassportInfo(): String {
        val builder = StringBuilder()

        if (passportSeries.isNotEmpty() && passportNumber.isNotEmpty()) {
            builder.append("Паспорт: $passportSeries $passportNumber")
        } else if (number.isNotEmpty()) {
            builder.append("Паспорт: $number")
        }

        if (passportIssuedBy.isNotEmpty()) {
            builder.append("\nКем выдан: $passportIssuedBy")
        }

        if (passportIssueDate.isNotEmpty()) {
            builder.append("\nДата выдачи: $passportIssueDate")
        }

        if (passportDepartmentCode.isNotEmpty()) {
            builder.append("\nКод подразделения: $passportDepartmentCode")
        }

        if (passportBirthDate.isNotEmpty()) {
            builder.append("\nДата рождения: $passportBirthDate")
        }

        if (passportBirthPlace.isNotEmpty()) {
            builder.append("\nМесто рождения: $passportBirthPlace")
        }

        return builder.toString()
    }
}