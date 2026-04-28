package com.hfad.yultour

import kotlinx.coroutines.delay

data class PaymentResult(
    val success: Boolean,
    val transactionId: String,
    val message: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class CardData(
    val cardNumber: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cvv: String,
    val cardholderName: String
)

class MockPaymentManager {

    // Валидация данных карты
    fun validateCardData(cardData: CardData): ValidationResult {
        if (cardData.cardNumber.replace(" ", "").length != 16) {
            return ValidationResult(false, "Неверный номер карты")
        }
        if (cardData.cvv.length != 3) {
            return ValidationResult(false, "Неверный CVV")
        }
        if (cardData.cardholderName.isBlank()) {
            return ValidationResult(false, "Введите имя держателя карты")
        }
        if (cardData.expiryMonth.isBlank() || cardData.expiryYear.isBlank()) {
            return ValidationResult(false, "Неверный срок действия")
        }
        return ValidationResult(true, "OK")
    }

    // Обработка платежа
    suspend fun processPayment(cardData: CardData, amount: Double, orderId: String): PaymentResult {
        // Имитация задержки обработки (как настоящий платеж)
        delay(2000)

        // Имитация успешной оплаты (95% успеха)
        val success = Math.random() > 0.05

        return if (success) {
            PaymentResult(
                success = true,
                transactionId = "YK_${System.currentTimeMillis()}",
                message = "Оплата успешно проведена",
                amount = amount
            )
        } else {
            PaymentResult(
                success = false,
                transactionId = "",
                message = "Платёж отклонён банком. Попробуйте другую карту.",
                amount = amount
            )
        }
    }

    // Возврат средств
    suspend fun refundPayment(transactionId: String): Boolean {
        delay(1500)
        return true
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String
)