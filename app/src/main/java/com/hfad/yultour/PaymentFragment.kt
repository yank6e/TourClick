package com.hfad.yultour

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.hfad.yultour.databinding.FragmentPaymentBinding
import kotlinx.coroutines.launch
import com.hfad.yultour.MainActivity
import com.hfad.yultour.utils.applyBottomPadding

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val args: PaymentFragmentArgs by navArgs()
    private val paymentManager = MockPaymentManager()
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    private var bookingId: String = ""
    private var tourId: String = ""
    private var tourTitle: String = ""
    private var tourDate: String = ""
    private var tourTime: String = ""
    private var seatNumbers: List<String> = emptyList()
    private var totalPrice: Float = 0f
    private var passengersCount: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем данные из аргументов
        bookingId = args.bookingId
        tourId = args.tourId
        tourTitle = args.tourTitle
        tourDate = args.tourDate
        tourTime = args.tourTime
        seatNumbers = args.seatNumbers?.toList() ?: emptyList()
        totalPrice = args.totalPrice
        passengersCount = args.passengersCount

        setupUI()
        setupInputMasks()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.tvTourTitle.text = tourTitle
        binding.tvTourDate.text = tourDate
        binding.tvSeats.text = seatNumbers.joinToString(", ")
        binding.tvTotalAmount.text = "${totalPrice.toInt()} ₽"
    }

    private fun setupInputMasks() {
        // Маска для номера карты (XXXX XXXX XXXX XXXX)
        binding.etCardNumber.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.etCardNumber.removeTextChangedListener(this)
                    val cleaned = s.toString().filter { it.isDigit() }
                    var formatted = ""
                    for (i in cleaned.indices) {
                        if (i > 0 && i % 4 == 0) formatted += " "
                        if (i < 16) formatted += cleaned[i]
                    }
                    current = formatted
                    binding.etCardNumber.setText(formatted)
                    binding.etCardNumber.setSelection(formatted.length)
                    binding.etCardNumber.addTextChangedListener(this)
                }
            }
        })

        // Маска для срока действия (ММ/ГГ)
        binding.etExpiry.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.etExpiry.removeTextChangedListener(this)
                    val cleaned = s.toString().filter { it.isDigit() }
                    var formatted = ""
                    for (i in cleaned.indices) {
                        if (i == 2) formatted += "/"
                        if (i < 4) formatted += cleaned[i]
                    }
                    current = formatted
                    binding.etExpiry.setText(formatted)
                    binding.etExpiry.setSelection(formatted.length)
                    binding.etExpiry.addTextChangedListener(this)
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnPay.setOnClickListener {
            if (validateAndPay()) {
                // Обработка платежа
            }
        }
    }

    private fun validateAndPay(): Boolean {
        val cardNumber = binding.etCardNumber.text.toString().replace(" ", "")
        val cardHolder = binding.etCardHolder.text.toString().trim()
        val expiry = binding.etExpiry.text.toString()
        val cvv = binding.etCvv.text.toString()

        // Валидация полей
        if (cardNumber.length != 16) {
            binding.tilCardNumber.error = "Введите корректный номер карты"
            return false
        }
        binding.tilCardNumber.error = null

        if (cardHolder.isBlank()) {
            binding.tilCardHolder.error = "Введите имя держателя"
            return false
        }
        binding.tilCardHolder.error = null

        if (expiry.length != 5) {
            binding.tilExpiry.error = "Введите срок (ММ/ГГ)"
            return false
        }
        binding.tilExpiry.error = null

        if (cvv.length != 3) {
            binding.tilCvv.error = "Введите CVV"
            return false
        }
        binding.tilCvv.error = null

        // Создаём данные карты
        val cardData = CardData(
            cardNumber = cardNumber,
            expiryMonth = expiry.substring(0, 2),
            expiryYear = "20" + expiry.substring(3, 5),
            cvv = cvv,
            cardholderName = cardHolder
        )

        // Валидация через PaymentManager
        val validation = paymentManager.validateCardData(cardData)
        if (!validation.isValid) {
            Toast.makeText(requireContext(), validation.message, Toast.LENGTH_SHORT).show()
            return false
        }

        // Обрабатываем платёж
        processPayment(cardData)
        return true
    }

    private fun processPayment(cardData: CardData) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Показываем индикатор загрузки
                binding.progressBar.visibility = View.VISIBLE
                binding.btnPay.isEnabled = false
                binding.btnPay.text = "Обработка..."

                // Обрабатываем платёж
                val result = paymentManager.processPayment(
                    cardData = cardData,
                    amount = totalPrice.toDouble(),
                    orderId = bookingId
                )

                if (result.success) {
                    // Обновляем бронирование
                    updateBooking(result)

                    // Генерируем чек
                    val booking = BookingModel(
                        id = bookingId,
                        userId = auth.currentUser?.uid ?: "",
                        tourId = tourId,
                        tourTitle = tourTitle,
                        tourDate = tourDate,
                        tourTime = tourTime,
                        seatNumbers = seatNumbers,
                        passengersCount = passengersCount,
                        totalPrice = totalPrice.toDouble(),
                        status = "confirmed",
                        bookingDate = Timestamp.now()
                    )

                    val receipt = ReceiptGenerator.generateReceipt(
                        requireContext(),
                        booking,
                        result
                    )

                    Toast.makeText(
                        requireContext(),
                        "✅ ${result.message}\nТранзакция: ${result.transactionId}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Переходим к экрану с чеком
                    val action = PaymentFragmentDirections.actionPaymentFragmentToReceiptFragment(
                        receiptText = receipt,
                        transactionId = result.transactionId,
                        amount = totalPrice,
                        tourTitle = tourTitle,
                        tourDate = tourDate,
                        seats = seatNumbers.joinToString(", ")
                    )
                    findNavController().navigate(action)

                } else {
                    Toast.makeText(
                        requireContext(),
                        "❌ ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnPay.isEnabled = true
                binding.btnPay.text = "ОПЛАТИТЬ"
            }
        }
    }

    private suspend fun updateBooking(paymentResult: PaymentResult) {
        val currentUser = auth.currentUser ?: return
        val updatedBooking = BookingModel(
            id = bookingId,
            userId = currentUser.uid,
            tourId = tourId,
            tourTitle = tourTitle,
            tourDate = tourDate,
            tourTime = tourTime,
            seatNumbers = seatNumbers,
            passengersCount = passengersCount,
            totalPrice = totalPrice.toDouble(),
            status = "confirmed",
            bookingDate = Timestamp.now()
        )
        repository.updateBooking(updatedBooking)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}