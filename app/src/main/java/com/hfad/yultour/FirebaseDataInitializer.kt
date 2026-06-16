package com.hfad.yultour

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class FirebaseDataInitializer(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseDataInitializer"

    // Модели для парсинга JSON
    data class JsonTour(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("description") val description: String,
        @SerializedName("price") val price: Double,
        @SerializedName("duration") val duration: String,
        @SerializedName("rating") val rating: Double,
        @SerializedName("startPoint") val startPoint: String,
        @SerializedName("stops") val stops: String,
        @SerializedName("highlights") val highlights: List<String>,
        @SerializedName("guideId") val guideId: String,
        @SerializedName("availableTimes") val availableTimes: List<String>,
        @SerializedName("maxPassengers") val maxPassengers: Int,
        @SerializedName("imageResource") val imageResource: String,
        @SerializedName("category") val category: String
    )

    data class TourList(
        @SerializedName("tours") val tours: List<JsonTour>
    )

    // Загружаем туры из JSON файла
    private fun loadToursFromJson(): List<JsonTour> {
        return try {
            val jsonString = context.assets.open("russian_tours.json")
                .bufferedReader()
                .use { it.readText() }

            val tourList = Gson().fromJson(jsonString, TourList::class.java)
            Log.d(TAG, "✅ Загружено ${tourList.tours.size} туров из JSON")
            tourList.tours
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка загрузки JSON: ${e.message}")
            emptyList()
        }
    }

    fun initializeToursData() {
        Log.d(TAG, "🔍 Checking if tours collection exists...")

        db.collection("tours").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    Log.d(TAG, "✅ Tours collection already exists with ${snapshot.documents.size} documents")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "📝 Tours collection is empty, loading from JSON...")

                val toursFromJson = loadToursFromJson()

                if (toursFromJson.isEmpty()) {
                    Log.e(TAG, "❌ No tours loaded from JSON, using default data")
                    // Можно оставить старые данные как fallback
                    addDefaultTours()
                    return@addOnSuccessListener
                }

                var successCount = 0
                val totalTours = toursFromJson.size

                toursFromJson.forEach { tour ->
                    val tourData = mapOf(
                        "title" to tour.title,
                        "description" to tour.description,
                        "price" to tour.price,
                        "duration" to tour.duration,
                        "rating" to tour.rating,
                        "startPoint" to tour.startPoint,
                        "stops" to tour.stops,
                        "highlights" to tour.highlights,
                        "guideId" to tour.guideId,
                        "availableTimes" to tour.availableTimes,
                        "maxPassengers" to tour.maxPassengers,
                        "imageResource" to tour.imageResource,
                        "category" to tour.category
                    )

                    db.collection("tours").add(tourData)
                        .addOnSuccessListener { documentReference ->
                            successCount++
                            Log.d(TAG, "✅ Tour '${tour.title}' added with ID: ${documentReference.id}")

                            if (successCount == totalTours) {
                                Log.d(TAG, "🎉 All $totalTours Russian tours added successfully from JSON!")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Failed to add tour '${tour.title}': ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error checking tours collection: ${e.message}")
            }
    }

    // Fallback метод со старыми данными (на всякий случай)
    private fun addDefaultTours() {
        Log.d(TAG, "📝 Adding default Russian tours...")

        val defaultRussianTours = listOf(
            mapOf(
                "title" to "Золотое кольцо России",
                "description" to "Путешествие по древним городам Золотого кольца",
                "price" to 45.0,
                "duration" to "3 дня",
                "rating" to 4.9,
                "startPoint" to "Москва",
                "stops" to "Сергиев Посад, Переславль-Залесский, Ростов Великий, Ярославль",
                "highlights" to listOf("История", "Архитектура", "Культура"),
                "guideId" to "guide_rus_1",
                "availableTimes" to listOf("08:00", "09:00"),
                "maxPassengers" to 25,
                "imageResource" to "tour_eco_city1",
                "category" to "historical"
            ),
            mapOf(
                "title" to "Карельские озера",
                "description" to "Тур по живописным озерам Карелии",
                "price" to 65.0,
                "duration" to "2 дня",
                "rating" to 4.8,
                "startPoint" to "Санкт-Петербург",
                "stops" to "Сортавала, Рускеала, Валаам, Ладожское озеро",
                "highlights" to listOf("Природа", "Озера", "Водопады"),
                "guideId" to "guide_rus_2",
                "availableTimes" to listOf("07:00", "08:00"),
                "maxPassengers" to 20,
                "imageResource" to "tour_coastal_heritage1",
                "category" to "nature"
            )
        )

        defaultRussianTours.forEach { tourData ->
            db.collection("tours").add(tourData)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "✅ Default tour added: ${tourData["title"]}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to add default tour: ${e.message}")
                }
        }
    }

    fun initializeGuidesData() {
        Log.d(TAG, "🔍 Checking if guides collection exists...")

        db.collection("guides").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    Log.d(TAG, "✅ Guides collection already exists with ${snapshot.documents.size} documents")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "📝 Guides collection is empty, adding Russian guides...")

                val russianGuides = listOf(
                    mapOf(
                        "name" to "Анна Петрова",
                        "bio" to "Экскурсовод по Золотому кольцу с историческим образованием",
                        "experience" to "5+ лет",
                        "rating" to 4.9,
                        "specialties" to listOf("История", "Архитектура", "Культура")
                    ),
                    mapOf(
                        "name" to "Дмитрий Соколов",
                        "bio" to "Горный гид и специалист по активному туризму",
                        "experience" to "7+ лет",
                        "rating" to 4.8,
                        "specialties" to listOf("Горные туры", "Активный отдых", "Природа")
                    ),
                    mapOf(
                        "name" to "Елена Иванова",
                        "bio" to "Гид по Санкт-Петербургу с искусствоведческим образованием",
                        "experience" to "6+ лет",
                        "rating" to 4.9,
                        "specialties" to listOf("Искусство", "Архитектура", "Музеи")
                    ),
                    mapOf(
                        "name" to "Сергей Кузнецов",
                        "bio" to "Специалист по природным заповедникам и экотуризму",
                        "experience" to "8+ лет",
                        "rating" to 4.7,
                        "specialties" to listOf("Природа", "Экотуризм", "Фотография")
                    )
                )

                russianGuides.forEachIndexed { index, guideData ->
                    db.collection("guides").add(guideData)
                        .addOnSuccessListener { documentReference ->
                            Log.d(TAG, "✅ Russian guide ${index + 1} added: ${guideData["name"]}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Failed to add guide ${index + 1}: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error checking guides collection: ${e.message}")
            }
    }
}