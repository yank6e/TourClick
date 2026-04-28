package com.hfad.yultour

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReceiptGenerator {

    suspend fun generateReceipt(
        context: Context,
        booking: BookingModel,
        paymentResult: PaymentResult
    ): String = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(paymentResult.timestamp))

            val receiptText = """
╔══════════════════════════════════════════╗
║          YULTUR — ЧЕК ОПЛАТЫ             ║
╠══════════════════════════════════════════╣
║ Дата: $dateStr
║ Транзакция: ${paymentResult.transactionId}
╠══════════════════════════════════════════╣
║ ТУР: ${booking.tourTitle}
║ Дата тура: ${booking.tourDate}
║ Время: ${booking.tourTime}
║ Места: ${booking.seatNumbers.joinToString(", ")}
║ Пассажиры: ${booking.passengersCount}
╠══════════════════════════════════════════╣
║ СУММА: ${booking.totalPrice.toInt()} ₽
║ СТАТУС: ${booking.status.uppercase()}
╠══════════════════════════════════════════╣
║ Спасибо за покупку!                      ║
║ Поддержка: support@yultour.ru            ║
╚══════════════════════════════════════════╝
            """.trimIndent()

            // Сохраняем в файл
            val fileName = "receipt_${paymentResult.transactionId}.txt"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            file.writeText(receiptText)

            return@withContext receiptText
        } catch (e: Exception) {
            return@withContext "Ошибка генерации чека: ${e.message}"
        }
    }

    // Генерация PDF чека (опционально)
    suspend fun generatePdfReceipt(
        context: Context,
        booking: BookingModel,
        paymentResult: PaymentResult
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(400, 600, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Заголовок
            paint.textSize = 16f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("YULTUR — ЧЕК ОПЛАТЫ", 200f, 40f, paint)

            // Дата
            paint.textSize = 12f
            paint.textAlign = Paint.Align.LEFT
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            canvas.drawText("Дата: ${dateFormat.format(Date(paymentResult.timestamp))}", 40f, 80f, paint)
            canvas.drawText("Транзакция: ${paymentResult.transactionId}", 40f, 100f, paint)

            // Информация о туре
            canvas.drawText("Тур: ${booking.tourTitle}", 40f, 140f, paint)
            canvas.drawText("Дата: ${booking.tourDate}", 40f, 160f, paint)
            canvas.drawText("Время: ${booking.tourTime}", 40f, 180f, paint)
            canvas.drawText("Места: ${booking.seatNumbers.joinToString(", ")}", 40f, 200f, paint)

            // Сумма
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("Сумма: ${booking.totalPrice.toInt()} ₽", 40f, 240f, paint)

            pdfDocument.finishPage(page)

            // Сохранение
            val fileName = "receipt_${paymentResult.transactionId}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            return@withContext file
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}