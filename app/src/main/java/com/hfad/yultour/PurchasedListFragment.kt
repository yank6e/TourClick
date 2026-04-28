package com.hfad.yultour

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.hfad.yultour.databinding.FragmentPurchasedListBinding
import com.hfad.yultour.databinding.ItemBookingBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.hfad.yultour.utils.applyBottomPadding

class PurchasedListFragment : Fragment() {

    private var _binding: FragmentPurchasedListBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseRepository()
    private lateinit var adapter: BookingsAdapter
    private var type: String = "upcoming"

    companion object {
        fun newInstance(type: String): PurchasedListFragment {
            val fragment = PurchasedListFragment()
            val args = Bundle()
            args.putString("type", type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPurchasedListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        type = arguments?.getString("type") ?: "upcoming"
        setupRecyclerView()
        loadBookings()
    }

    private fun setupRecyclerView() {
        adapter = BookingsAdapter(emptyList()) { booking ->
            // TODO: Show booking details
        }

        binding.rvBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookings.adapter = adapter
    }

    private fun loadBookings() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val allBookings = repository.getUserBookings(currentUser.uid)

                    if (!isAdded || _binding == null) return@launch

                    val filteredBookings = if (type == "upcoming") {
                        allBookings.filter { it.status == "confirmed" }
                    } else {
                        allBookings.filter { it.status == "completed" || it.status == "cancelled" }
                    }

                    adapter.updateBookings(filteredBookings)

                    if (filteredBookings.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvBookings.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvBookings.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class BookingsAdapter(
    private var bookings: List<BookingModel>,
    private val onBookingClick: (BookingModel) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    inner class BookingViewHolder(private val binding: ItemBookingBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: BookingModel) {
            binding.tvTourTitle.text = booking.tourTitle

            // ИСПОЛЬЗУЕМ СОХРАНЕННЫЕ ВРЕМЯ И ДАТУ
            val dateTimeText = buildString {
                if (booking.tourDate.isNotEmpty()) {
                    append("Дата: ${booking.tourDate}")
                }
                if (booking.tourTime.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append("Время: ${booking.tourTime}")
                }
                if (booking.duration.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(booking.duration)
                }
            }

            binding.tvTourDate.text = if (dateTimeText.isNotEmpty()) dateTimeText else "Дата и время не указаны"
            binding.tvSeats.text = "Места: ${booking.seatNumbers.joinToString(", ")}"

            // ПРЕОБРАЗОВАНИЕ СТАТУСА НА РУССКИЙ ЯЗЫК И УСТАНОВКА ФОНА
            val russianStatus = getRussianStatus(booking.status)
            binding.tvStatus.text = russianStatus

            // УСТАНАВЛИВАЕМ ФОН В ЗАВИСИМОСТИ ОТ СТАТУСА
            val backgroundRes = getStatusBackground(booking.status)
            binding.tvStatus.setBackgroundResource(backgroundRes)

            binding.tvOrderNumber.text = "Заказ #${booking.id.take(8).uppercase()}"
            binding.tvPurchaseDate.text = "Куплен ${dateFormat.format(booking.bookingDate.toDate())}"
            binding.tvTotalPrice.text = "Оплачено ₽${booking.totalPrice}"
            binding.tvPassengers.text = "Пассажиры: ${booking.passengersCount}"

            binding.root.setOnClickListener {
                onBookingClick(booking)
            }
        }
    }

    private fun getStatusBackground(status: String): Int {
        return when (status.lowercase()) {
            "in_cart" -> R.drawable.status_background_pending
            "confirmed" -> R.drawable.status_background_confirmed
            "completed" -> R.drawable.status_background_completed
            "cancelled" -> R.drawable.status_background_cancelled
            "pending" -> R.drawable.status_background_pending
            "processing" -> R.drawable.status_background_processing
            "refunded" -> R.drawable.status_background_refunded
            else -> R.drawable.status_background
        }
    }

    // ФУНКЦИЯ ДЛЯ ПРЕОБРАЗОВАНИЯ СТАТУСА
    private fun getRussianStatus(status: String): String {
        return when (status.lowercase()) {
            "in_cart" -> "В корзине"
            "confirmed" -> "Подтвержден"
            "completed" -> "Завершен"
            "cancelled" -> "Отменен"
            "pending" -> "Ожидает"
            "processing" -> "В обработке"
            "refunded" -> "Возвращен"
            else -> status
        }
    }

    // ФУНКЦИЯ ДЛЯ ПОЛУЧЕНИЯ ЦВЕТА СТАТУСА
    private fun getStatusColor(status: String, context: Context): Int {
        return ContextCompat.getColor(context, when (status.lowercase()) {
            "in_cart" -> R.color.blue_primary
            "confirmed" -> R.color.green_primary
            "completed" -> R.color.green_dark
            "cancelled" -> R.color.red_primary
            "pending" -> R.color.orange_primary
            "processing" -> R.color.blue_primary
            "refunded" -> R.color.purple_primary
            else -> R.color.text_secondary
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<BookingModel>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}