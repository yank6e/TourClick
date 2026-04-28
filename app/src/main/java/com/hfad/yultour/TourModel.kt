package com.hfad.yultour

data class TourModel(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val duration: String = "",
    val rating: Double = 0.0,
    val startPoint: String = "",
    val stops: String = "",
    val highlights: List<String> = emptyList(),
    val guideId: String = "",
    val imageResource: String = "", // ИЗМЕНИЛ imageUrl на imageResource
    val availableTimes: List<String> = emptyList(),
    val maxPassengers: Int = 0
) {
    companion object {
        fun fromMap(map: Map<String, Any>): TourModel {
            return TourModel(
                id = map["id"] as? String ?: "",
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                price = (map["price"] as? Double) ?: 0.0,
                duration = map["duration"] as? String ?: "",
                rating = (map["rating"] as? Double) ?: 0.0,
                startPoint = map["startPoint"] as? String ?: "",
                stops = map["stops"] as? String ?: "",
                highlights = (map["highlights"] as? List<String>) ?: emptyList(),
                guideId = map["guideId"] as? String ?: "",
                imageResource = map["imageResource"] as? String ?: "", // ИЗМЕНИЛ
                availableTimes = (map["availableTimes"] as? List<String>) ?: emptyList(),
                maxPassengers = (map["maxPassengers"] as? Long)?.toInt() ?: 0
            )
        }
    }
}