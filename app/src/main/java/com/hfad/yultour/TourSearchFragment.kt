package com.hfad.yultour

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hfad.yultour.databinding.FragmentTourSearchBinding
import com.hfad.yultour.databinding.TourItemBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TourSearchFragment : Fragment() {

    private var _binding: FragmentTourSearchBinding? = null
    private val binding get() = _binding!!
    private val calendar = Calendar.getInstance()
    private var selectedDate: String = "Выберите дату"
    private var searchDate: String = ""
    private var selectedPeople: Int = 1
    private var fromCity: String = "Текущее местоположение"
    private var toCity: String = "Город назначения"
    private val repository = FirebaseRepository()

    // Список популярных городов
    private val popularCities = listOf(
        "Москва",
        "Санкт-Петербург",
        "Казань",
        "Сочи",
        "Екатеринбург",
        "Новосибирск",
        "Нижний Новгород",
        "Краснодар",
        "Владивосток",
        "Калининград",
        "Ростов-на-Дону",
        "Уфа",
        "Красноярск",
        "Пермь",
        "Воронеж",
        "Волгоград",
        "Саратов",
        "Тюмень",
        "Иркутск",
        "Ярославль",

        "Сергиев Посад",
        "Переславль-Залесский",
        "Ростов Великий",
        "Кострома",
        "Иваново",
        "Суздаль",
        "Владимир",

        "Приозерск",
        "Сортавала",
        "Рускеала",
        "Валаам",

        "Барнаул",
        "Чемал",
        "Листвянка",
        "Симферополь",
        "Ялта",
        "Алупка",
        "Ливадия",
        "Массандра",
        "Гурзуф",
        "Севастополь",
        "Балаклава",

        "Невьянск",
        "Нижний Тагил",

        "Ульяновск",
        "Самара",

        "Минеральные Воды",
        "Пятигорск",
        "Кисловодск",
        "Ессентуки",
        "Железноводск",
        "Домбай",

        "Зеленоградск",
        "Светлогорск",
        "Янтарный",
        "Балтийск",

        "Великий Новгород",

        "Петропавловск-Камчатский",

        "Махачкала",
        "Дербент",
        "Гуниб",

        "Архангельск",

        "Мурманск",
        "Кировск",
        "Апатиты"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTourSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupToursRecyclerView()
        updateNotificationBadge()

        // Увеличиваем размер текста на кнопках
        binding.findButton.textSize = 16f
        binding.seeAllButton.textSize = 14f
    }

    private fun setupClickListeners() {
        // Обработчик для выбора города отправления
        binding.fromSelection.setOnClickListener {
            showCityPicker(true) // true - для выбора города отправления
        }

        // Обработчик для выбора города назначения
        binding.toSelection.setOnClickListener {
            showCityPicker(false) // false - для выбора города назначения
        }

        // Обработчик для выбора даты
        binding.dateSelection.setOnClickListener {
            showDatePicker()
        }

        // Обработчик для выбора количества людей
        binding.peopleSelection.setOnClickListener {
            showPeoplePicker()
        }

        // Обработчик для кнопки поиска
        binding.findButton.setOnClickListener {
            performSearch()
        }

        // Обработчик для кнопки "See All"
        binding.seeAllButton.setOnClickListener {
            showAllTours()
        }

        // Обработчик для уведомлений
        binding.notificationIcon.setOnClickListener {
            showNotificationsDialog()
        }
    }

    private fun showCityPicker(isFromCity: Boolean) {
        val cities = popularCities.toTypedArray()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle(if (isFromCity) "Выберите текущее местоположение" else "Выберите город назначения")
            .setItems(cities) { dialog, which ->
                val selectedCity = cities[which]
                if (isFromCity) {
                    fromCity = selectedCity
                    binding.fromCityText.text = selectedCity
                } else {
                    toCity = selectedCity
                    binding.toCityText.text = selectedCity
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateNotificationBadge() {
        val unreadCount = NotificationModel.getSampleNotifications().count { !it.isRead }
        binding.notificationBadge.text = unreadCount.toString()
        binding.notificationBadge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
    }

    private fun showNotificationsDialog() {
        val notificationsDialog = NotificationsDialog()

        // Слушатель должен быть установлен после создания диалога
        notificationsDialog.show(parentFragmentManager, "notifications_dialog")

        // Обновляем бейдж когда диалог закрывается
        parentFragmentManager.setFragmentResultListener("notifications_dismissed", viewLifecycleOwner) { _, _ ->
            updateNotificationBadge()
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                binding.selectedDateText.text = selectedDate

                // Также сохраняем в формате для поиска
                val searchDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                searchDate = searchDateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Запрещаем выбор прошедших дат
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showPeoplePicker() {
        val peopleOptions = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10+")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Выберите количество пассажиров")
            .setItems(peopleOptions) { dialog, which ->
                selectedPeople = if (which == 9) 10 else which + 1
                binding.peopleCountText.text = if (which == 9) "10+" else selectedPeople.toString()
            }
            .show()
    }

    private fun performSearch() {
        val from = fromCity
        val to = toCity
        val date = selectedDate
        val peopleCount = selectedPeople

        if (to == "Город назначения") {
            // Показываем только ОДНО уведомление при ошибке
            android.widget.Toast.makeText(requireContext(), "Пожалуйста, выберите город назначения", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        if (date == "Выберите дату") {
            android.widget.Toast.makeText(requireContext(), "Пожалуйста, выберите дату", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                val searchResults = repository.searchTours(
                    fromCity = if (from != "Текущее местоположение") from else null,
                    toCity = if (to != "Город назначения") to else null,
                    date = if (date != "Выберите дату") searchDate else null,
                    peopleCount = peopleCount
                )

                updateSearchResults(searchResults, to)

            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Ошибка поиска: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showAllTours() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                val allTours = repository.getAllTours()
                updateSearchResults(allTours, "все направления")
                // УБИРАЕМ Toast при показе всех туров - лишняя информация
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Ошибка загрузки туров", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateSearchResults(tours: List<TourModel>, destination: String) {
        if (tours.isEmpty()) {
            // Показываем сообщение что данных нет
            binding.toursRecyclerView.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "Нет доступных туров\nПожалуйста, выберите другие направления"
            return
        }

        binding.toursRecyclerView.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        val adapter = ToursAdapter(tours) { tour ->
            val bundle = Bundle().apply {
                putString("tourId", tour.id)
            }
            findNavController().navigate(R.id.action_tourSearchFragment_to_tourDetailsFragment, bundle)
        }

        binding.toursRecyclerView.adapter = adapter

        // УБИРАЕМ Toast о количестве найденных туров - лишняя информация
        // Можно показывать количество в UI более ненавязчиво
        updateResultsCount(tours.size, destination)
    }

    private fun updateResultsCount(count: Int, destination: String) {
        // Вместо Toast показываем количество в UI более ненавязчиво
        // Например, можно добавить TextView для отображения количества
        val fromText = if (fromCity == "Текущее местоположение") "Ваше местоположение" else fromCity
        val toText = if (destination == "все направления") "все направления" else toCity

        // Если нужно, можно добавить TextView для отображения этой информации
        // Например: binding.tvResultsCount.text = "Найдено $count туров"
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.findButton.isEnabled = !show
        binding.findButton.text = if (show) "Поиск..." else "Найти"
    }

    private fun setupToursRecyclerView() {
        // Инициализируем с пустым списком
        val adapter = ToursAdapter(emptyList()) { tour ->
            val bundle = Bundle().apply {
                putString("tourId", tour.id)
            }
            findNavController().navigate(R.id.action_tourSearchFragment_to_tourDetailsFragment, bundle)
        }

        binding.toursRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        binding.toursRecyclerView.isNestedScrollingEnabled = false
        binding.toursRecyclerView.setPadding(16.dpToPx(requireContext()), 0, 16.dpToPx(requireContext()), 0)
        binding.toursRecyclerView.clipToPadding = false
        binding.toursRecyclerView.adapter = adapter

        // Загружаем начальные туры
        showAllTours()
    }

    // Extension функция для конвертации dp в px
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ToursAdapter(
    private val tours: List<TourModel>,
    private val onTourClick: (TourModel) -> Unit
) : RecyclerView.Adapter<ToursAdapter.TourViewHolder>() {

    inner class TourViewHolder(private val binding: TourItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tour: TourModel) {
            binding.tourTitle.text = tour.title
            binding.tourDescription.text = tour.description
            binding.tourPrice.text = "От ₽${tour.price.toInt()} • Бесплатная отмена"

            // Используем imageResource вместо imageUrl
            ImageLoader.loadImage(tour.imageResource, binding.tourImage)

            binding.root.setOnClickListener {
                onTourClick(tour)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TourViewHolder {
        val binding = TourItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TourViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TourViewHolder, position: Int) {
        holder.bind(tours[position])
    }

    override fun getItemCount(): Int = tours.size
}