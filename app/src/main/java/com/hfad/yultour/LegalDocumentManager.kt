package com.hfad.yultour

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LegalDocument(
    val id: String,
    val title: String,
    val filename: String,
    val version: String,
    val updated: String
)

data class LegalDocumentsList(
    val documents: List<LegalDocument>
)

class LegalDocumentManager(private val context: Context) {

    private val gson = Gson()

    suspend fun getDocumentsList(): List<LegalDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open("legal/terms.json")
                    .bufferedReader()
                    .use { it.readText() }
                val list = gson.fromJson(json, LegalDocumentsList::class.java)
                list.documents
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getDocumentContent(documentId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val documents = getDocumentsList()
                val document = documents.find { it.id == documentId }
                document?.let {
                    context.assets.open("legal/${it.filename}")
                        .bufferedReader()
                        .use { reader -> reader.readText() }
                } ?: "Документ не найден"
            } catch (e: Exception) {
                "Ошибка загрузки документа: ${e.message}"
            }
        }
    }

    fun getDocumentContentSync(documentId: String): String {
        return try {
            val inputStream = when (documentId) {
                "offer" -> context.assets.open("legal/offer.txt")
                "privacy" -> context.assets.open("legal/privacy.txt")
                "rules" -> context.assets.open("legal/rules.txt")
                else -> throw IllegalArgumentException("Неизвестный документ: $documentId")
            }
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Ошибка загрузки документа"
        }
    }
}