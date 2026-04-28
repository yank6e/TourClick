package com.hfad.yultour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.hfad.yultour.databinding.FragmentSeatSelectionBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

class SeatSelectionFragment : Fragment() {

    private var _binding: FragmentSeatSelectionBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseRepository()

    private var tourId: String = ""
    private var tourTitle: String = ""
    private var tourPrice: Double = 0.0
    private var bookingId: String = ""
    private var selectedTime: String = ""
    private var selectedDate: String = ""
    private val selectedSeats = mutableSetOf<String>()
    private val occupiedSeats = mutableSetOf<String>()
    private val paymentManager = MockPaymentManager()

    private val busSeats = listOf(
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
        "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24",
        "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36",
        "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSeatSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем данные из аргументов
        tourId = arguments?.getString("tourId") ?: ""
        tourTitle = arguments?.getString("tourTitle") ?: ""
        bookingId = arguments?.getString("bookingId") ?: ""

        // Получаем сохраненное время и дату из бронирования
        selectedTime = arguments?.getString("selectedTime") ?: "09:00"
        selectedDate = arguments?.getString("selectedDate") ?: ""

        val tourPriceFloat = arguments?.getFloat("tourPrice") ?: 0f
        tourPrice = tourPriceFloat.toDouble()

//        setupToolbar()
        setupRouteInfo()
        setupClickListeners()
        loadOccupiedSeats()

        binding.btnAddToCart.visibility = View.VISIBLE
        binding.btnAddToCart.isEnabled = true
    }

//    private fun setupToolbar() {
//        binding.btnBack.setOnClickListener {
//            findNavController().navigateUp()
//        }
//    }

    private fun setupRouteInfo() {
        binding.tvRoute.text = tourTitle
        binding.tvPricePerSeat.text = "${tourPrice.toInt()} ₽ за место"

        // Используем сохраненную дату или текущую
        val dateToShow = if (selectedDate.isNotEmpty()) {
            selectedDate
        } else {
            SimpleDateFormat("dd MMM, EEE", Locale.getDefault()).format(Date())
        }

        binding.tvDateTime.text = "$dateToShow • $selectedTime"
    }

    private fun setupClickListeners() {
        binding.btnAddToCart.setOnClickListener {
            if (selectedSeats.isNotEmpty()) {
                reserveSelectedSeats()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Пожалуйста, выберите место",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadOccupiedSeats() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Конвертируем дату в формат для хранения в Firebase
                val storageDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

                val dateForStorage = if (selectedDate.isNotEmpty()) {
                    try {
                        storageDateFormat.format(displayDateFormat.parse(selectedDate) ?: Date())
                    } catch (e: Exception) {
                        storageDateFormat.format(Date())
                    }
                } else {
                    storageDateFormat.format(Date())
                }

                val occupied = repository.getOccupiedSeats(tourId, dateForStorage, selectedTime)
                occupiedSeats.addAll(occupied)
                createSeatLayout()
            } catch (e: Exception) {
                createSeatLayout()
            }
        }
    }

    private fun createSeatLayout() {
        val seatsContainer = binding.root.findViewById<LinearLayout>(R.id.seatsGrid)
        seatsContainer?.removeAllViews()

        for (row in 0 until 12) {
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 4)
                }
            }

            for (col in 0 until 4) {
                val seatIndex = row * 4 + col
                if (seatIndex < busSeats.size) {
                    val seatNumber = busSeats[seatIndex]

                    val seatButton = Button(requireContext()).apply {
                        text = seatNumber
                        textSize = 10f
                        setPadding(8, 8, 8, 8)
                        minimumWidth = 0
                        minimumHeight = 0
                        layoutParams = LinearLayout.LayoutParams(
                            resources.getDimensionPixelSize(R.dimen.seat_width),
                            resources.getDimensionPixelSize(R.dimen.seat_height)
                        ).apply {
                            setMargins(4, 0, 4, 0)
                        }

                        updateSeatAppearance(this, seatNumber)

                        setOnClickListener {
                            if (!occupiedSeats.contains(seatNumber)) {
                                toggleSeatSelection(seatNumber, this)
                            }
                        }
                    }

                    rowLayout.addView(seatButton)
                }
            }

            seatsContainer?.addView(rowLayout)
        }
    }

    private fun updateSeatAppearance(seatButton: Button, seatNumber: String) {
        when {
            occupiedSeats.contains(seatNumber) -> {
                seatButton.setBackgroundResource(R.drawable.seat_occupied)
                seatButton.isEnabled = false
                seatButton.alpha = 0.5f
            }
            selectedSeats.contains(seatNumber) -> {
                seatButton.setBackgroundResource(R.drawable.seat_selected)
                seatButton.setTextColor(resources.getColor(android.R.color.white, null))
            }
            else -> {
                seatButton.setBackgroundResource(R.drawable.seat_available)
                seatButton.setTextColor(resources.getColor(R.color.green_primary, null))
            }
        }
    }

    private fun toggleSeatSelection(seatNumber: String, seatButton: Button) {
        if (selectedSeats.contains(seatNumber)) {
            selectedSeats.remove(seatNumber)
        } else {
            selectedSeats.add(seatNumber)
        }

        updateSeatAppearance(seatButton, seatNumber)
        updateSelectionInfo()
    }

    private fun updateSelectionInfo() {
        if (selectedSeats.isNotEmpty()) {
            binding.selectedSeatsCard.visibility = View.VISIBLE
            val seatsText = selectedSeats.sorted().joinToString(", ")
            binding.tvSelectedSeats.text = seatsText

            val totalPrice = selectedSeats.size * tourPrice
            binding.tvTotalPrice.text = "Итого: ${totalPrice.toInt()} ₽"
            binding.btnAddToCart.text = "ОПЛАТИТЬ (${selectedSeats.size})"
        } else {
            binding.selectedSeatsCard.visibility = View.GONE
            binding.btnAddToCart.text = "ОПЛАТИТЬ"
        }
    }

    private fun reserveSelectedSeats() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Конвертируем дату
                    val storageDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val displayDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                    val dateForStorage = if (selectedDate.isNotEmpty()) {
                        try {
                            storageDateFormat.format(displayDateFormat.parse(selectedDate) ?: Date())
                        } catch (e: Exception) {
                            storageDateFormat.format(Date())
                        }
                    } else {
                        storageDateFormat.format(Date())
                    }

                    // Резервируем места
                    repository.reserveSeats(
                        tourId = tourId,
                        date = dateForStorage,
                        time = selectedTime,
                        seats = selectedSeats.toList(),
                        userId = currentUser.uid
                    )

                    // Вычисляем общую цену
                    val calculatedTotalPrice = selectedSeats.size * tourPrice

                    // ✅ ИЗМЕНЕНО: Переход к оплате вместо сохранения
                    if (bookingId.isNotEmpty()) {
                        val updatedBooking = BookingModel(
                            id = bookingId,
                            userId = currentUser.uid,
                            tourId = tourId,
                            tourTitle = tourTitle,
                            tourDate = selectedDate,
                            tourTime = selectedTime,
                            seatNumbers = selectedSeats.toList(),
                            passengersCount = selectedSeats.size,
                            totalPrice = calculatedTotalPrice,
                            status = "pending", // Меняем на pending до оплаты
                            duration = "3h"
                        )
                        repository.updateBooking(updatedBooking)

                        // ✅ Переходим к оплате через Safe Args
                        val action = SeatSelectionFragmentDirections
                            .actionSeatSelectionFragmentToPaymentFragment(
                                bookingId = bookingId,
                                tourId = tourId,
                                tourTitle = tourTitle,
                                tourDate = selectedDate,
                                tourTime = selectedTime,
                                seatNumbers = selectedSeats.toTypedArray(),
                                totalPrice = calculatedTotalPrice.toFloat(),
                                passengersCount = selectedSeats.size
                            )
                        findNavController().navigate(action)
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.btnAddToCart.visibility = View.VISIBLE
        binding.btnAddToCart.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}