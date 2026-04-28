package com.hfad.yultour

import android.graphics.Color
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.hfad.yultour.databinding.FragmentTourDetailsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TourDetailsFragment : Fragment() {

    private var _binding: FragmentTourDetailsBinding? = null
    private val binding get() = _binding!!
    private var selectedTime: String = "09:00"
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()
    private var tourId: String = ""
    private var currentTour: TourModel? = null
    private var isFavorite: Boolean = false

    // Все доступные времена
    private val allAvailableTimes = listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTourDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем tourId из аргументов
        tourId = arguments?.getString("tourId") ?: ""

        // Устанавливаем заголовок в Activity Toolbar
        (requireActivity() as MainActivity).supportActionBar?.title = "Детали тура"

        setupFavoriteButton()
        setupAddToCartButton()
        loadTourDetails()
    }

    private fun setupFavoriteButton() {
        binding.fabFavorite.setOnClickListener {
            toggleFavorite()
        }
    }

    private fun setupAddToCartButton() {
        binding.btnSelectSeats.text = "Добавить в корзину"
        binding.btnSelectSeats.setOnClickListener {
            addToCart()
        }
    }

    private fun addToCart() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentTour != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Получаем текущую дату в нужном формате
                    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                    val currentDate = dateFormat.format(Date())

                    // Создаем booking с текущей датой и выбранным временем
                    val booking = BookingModel(
                        userId = currentUser.uid,
                        tourId = tourId,
                        tourTitle = currentTour?.title ?: "",
                        tourDate = currentDate, // Сохраняем текущую дату
                        tourTime = selectedTime, // Сохраняем выбранное время
                        seatNumbers = emptyList(),
                        passengersCount = 1,
                        totalPrice = currentTour?.price ?: 0.0,
                        status = "in_cart",
                        duration = currentTour?.duration ?: ""
                    )

                    val bookingId = repository.createBooking(booking)

                    Toast.makeText(
                        requireContext(),
                        "Тур '${currentTour?.title}' добавлен в корзину на $currentDate в $selectedTime!",
                        Toast.LENGTH_LONG
                    ).show()

                    requireActivity().onBackPressed()

                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка добавления в корзину: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Пожалуйста, войдите в систему",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toggleFavorite() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentTour != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (isFavorite) {
                        repository.removeFromFavorites(currentUser.uid, tourId)
                        binding.fabFavorite.setImageResource(R.drawable.ic_favorite_border)
                        isFavorite = false
                        Toast.makeText(requireContext(), "Удалено из избранного", Toast.LENGTH_SHORT).show()
                    } else {
                        repository.addToFavorites(currentUser.uid, tourId)
                        binding.fabFavorite.setImageResource(R.drawable.ic_favorite_filled)
                        isFavorite = true
                        Toast.makeText(requireContext(), "Добавлено в избранное", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkIfFavorite() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    isFavorite = repository.isTourFavorite(currentUser.uid, tourId)
                    val iconRes = if (isFavorite) {
                        R.drawable.ic_favorite_filled
                    } else {
                        R.drawable.ic_favorite_border
                    }
                    binding.fabFavorite.setImageResource(iconRes)
                } catch (e: Exception) {
                    binding.fabFavorite.setImageResource(R.drawable.ic_favorite_border)
                }
            }
        }
    }

    private fun loadTourDetails() {
        if (tourId.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val tour = repository.getTourById(tourId)
                    tour?.let { tourData ->
                        currentTour = tourData

                        // Устанавливаем заголовок тура
                        binding.tvTourTitle.text = tourData.title
                        binding.tourDescription.text = tourData.description
                        binding.startingPointText.text = tourData.startPoint
                        binding.stopsText.text = tourData.stops

                        // Настраиваем кнопки времени - показываем все доступные времена
                        setupTimeButtons()

                        if (tourData.highlights.isNotEmpty()) {
                            binding.highlight1.text = tourData.highlights.getOrElse(0) { "" }
                            binding.highlight2.text = tourData.highlights.getOrElse(1) { "" }
                            binding.highlight3.text = tourData.highlights.getOrElse(2) { "" }
                        }

                        checkIfFavorite()

                    } ?: run {
                        Toast.makeText(requireContext(), "Тур не найден", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressed()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка загрузки деталей тура", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "ID тура отсутствует", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
    }

    private fun setupTimeButtons() {
        val timeButtonsLayout = binding.timeButtonsLayout
        timeButtonsLayout.removeAllViews()

        // Уменьшаем количество кнопок в строке с 4 до 3
        val timesPerRow = 3
        val rows = allAvailableTimes.chunked(timesPerRow)

        for (rowTimes in rows) {
            val rowLayout = createTimeRowLayout()

            for (time in rowTimes) {
                val timeButton = createTimeButton(time)
                rowLayout.addView(timeButton)
            }

            timeButtonsLayout.addView(rowLayout)

            // НЕ добавляем пустые View для выравнивания!
        }

        // Выбираем первое время по умолчанию
        if (allAvailableTimes.isNotEmpty()) {
            selectedTime = allAvailableTimes[0]
            updateTimeButtonSelection()
        }
    }

    private fun createTimeRowLayout(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 8.dpToPx())
        }
    }

    private fun createTimeButton(time: String): Button {
        return Button(requireContext()).apply {
            text = time
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                marginEnd = 4.dpToPx()
                bottomMargin = 4.dpToPx()
            }

            // Простые настройки без Material Design
            setPadding(0, 0, 0, 0)
            minWidth = 0
            minimumWidth = 0
            gravity = Gravity.CENTER

            // Убираем все Material стили
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setBackgroundResource(if (time == selectedTime)
                    R.drawable.time_button_background_selected
                else R.drawable.time_button_background)
            } else {
                // Для старых версий
                if (time == selectedTime) {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_primary))
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                } else {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary))
                    // Добавляем границу для обычного состояния
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        backgroundTintList = null
                    }
                }
            }

            setOnClickListener {
                selectedTime = time
                updateTimeButtonSelection()
            }
        }
    }

    private fun MaterialButton.setSelectedTimeStyle() {
        // Меняем стиль для выбранной кнопки времени
        this.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            R.color.green_primary
        )
        this.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        this.strokeWidth = 0
        this.strokeColor = null
    }

    private fun MaterialButton.setUnselectedTimeStyle() {
        // Возвращаем к исходному стилю кнопки времени (контурной)
        this.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            android.R.color.transparent
        )
        this.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary))
        this.strokeWidth = 2.dpToPx()
        this.strokeColor = ContextCompat.getColorStateList(
            requireContext(),
            R.color.green_primary
        )
    }

    private fun updateTimeButtonSelection() {
        // Обновляем все кнопки времени
        val timeButtonsLayout = binding.timeButtonsLayout
        for (i in 0 until timeButtonsLayout.childCount) {
            val rowLayout = timeButtonsLayout.getChildAt(i) as LinearLayout
            for (j in 0 until rowLayout.childCount) {
                val button = rowLayout.getChildAt(j) as Button
                val buttonTime = button.text.toString()

                if (buttonTime == selectedTime) {
                    // Выбранная кнопка
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        button.setBackgroundResource(R.drawable.time_button_background_selected)
                    } else {
                        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_primary))
                    }
                    button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                } else {
                    // Невыбранная кнопка
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        button.setBackgroundResource(R.drawable.time_button_background)
                    } else {
                        button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary))
                }
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}