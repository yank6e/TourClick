package com.hfad.yultour

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.hfad.yultour.databinding.FragmentPurchasedListBinding
import com.hfad.yultour.databinding.ItemCartBinding
import kotlinx.coroutines.launch

class CartListFragment : Fragment() {

    private var _binding: FragmentPurchasedListBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseRepository()
    private lateinit var adapter: CartAdapter
    private var type: String = "cart"

    companion object {
        fun newInstance(type: String): CartListFragment {
            val fragment = CartListFragment()
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

        type = arguments?.getString("type") ?: "cart"
        setupRecyclerView()
        loadCartItems()
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(emptyList()) { booking ->
            if (type == "cart") {
                val bundle = Bundle().apply {
                    putString("tourId", booking.tourId)
                    putString("tourTitle", booking.tourTitle)
                    putFloat("tourPrice", booking.totalPrice.toFloat())
                    putString("bookingId", booking.id)
                    // Передаем сохраненное время из бронирования
                    putString("selectedTime", booking.tourTime)
                    putString("selectedDate", booking.tourDate)
                }
                findNavController().navigate(R.id.action_purchasedFragment_to_seatSelectionFragment, bundle)
            }
        }

        binding.rvBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookings.adapter = adapter
    }

    private fun loadCartItems() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val allBookings = repository.getUserBookings(currentUser.uid)

                    val cartBookings = if (type == "cart") {
                        allBookings.filter { it.status == "in_cart" }
                    } else {
                        allBookings.filter { it.status == "confirmed" }
                    }

                    adapter.updateBookings(cartBookings)

                    if (cartBookings.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvBookings.visibility = View.GONE
                        binding.tvEmptyState.text = if (type == "cart") {
                            "Корзина пуста\nДобавьте туры в корзину"
                        } else {
                            "У вас нет предстоящих туров"
                        }
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
        loadCartItems()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CartAdapter(
    private var bookings: List<BookingModel>,
    private val onBookingClick: (BookingModel) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: BookingModel) {
            binding.tvTourTitle.text = booking.tourTitle

            // Отображаем сохраненные дату и время
            val timeText = if (booking.tourDate.isNotEmpty() && booking.tourTime.isNotEmpty()) {
                "Дата: ${booking.tourDate} • Время: ${booking.tourTime} • ${booking.duration}"
            } else if (booking.tourTime.isNotEmpty()) {
                "Время: ${booking.tourTime} • ${booking.duration}"
            } else {
                "${booking.duration}"
            }

            binding.tvTourDate.text = timeText
            binding.tvTotalPrice.text = "Цена: ₽${booking.totalPrice}"

            // ПРЕОБРАЗОВАНИЕ СТАТУСА
            val russianStatus = getRussianStatus(booking.status)
            binding.tvStatus.text = russianStatus

            // УСТАНАВЛИВАЕМ ЦВЕТ СТАТУСА
            val statusColor = getStatusColor(booking.status, binding.root.context)
            binding.tvStatus.setTextColor(statusColor)

            // УСТАНАВЛИВАЕМ ФОН СТАТУСА
            val backgroundRes = getStatusBackground(booking.status)
            binding.tvStatus.setBackgroundResource(backgroundRes)

            if (booking.status == "in_cart") {
                binding.btnSelectSeats.visibility = View.VISIBLE
                binding.btnSelectSeats.text = "Выбрать места"
                binding.btnSelectSeats.setOnClickListener {
                    onBookingClick(booking)
                }
            } else {
                binding.btnSelectSeats.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onBookingClick(booking)
            }
        }
    }

    private fun getRussianStatus(status: String): String {
        return when (status.lowercase()) {
            "in_cart" -> "В корзине"
            "confirmed" -> "Подтвержден"
            "completed" -> "Завершен"
            "cancelled" -> "Отменен"
            else -> status
        }
    }

    private fun getStatusColor(status: String, context: Context): Int {
        return ContextCompat.getColor(context, when (status.lowercase()) {
            "in_cart" -> R.color.white
            "confirmed" -> R.color.white
            "completed" -> R.color.white
            "cancelled" -> R.color.white
            else -> R.color.white
        })
    }

    private fun getStatusBackground(status: String): Int {
        return when (status.lowercase()) {
            "in_cart" -> R.drawable.status_background_pending
            "confirmed" -> R.drawable.status_background_confirmed
            "completed" -> R.drawable.status_background_completed
            "cancelled" -> R.drawable.status_background_cancelled
            else -> R.drawable.status_background
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<BookingModel>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}