package com.hfad.yultour

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.hfad.yultour.databinding.FragmentFavoritesBinding
import com.hfad.yultour.databinding.TourItemBinding
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.hfad.yultour.utils.applyBottomPadding

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseRepository()
    private lateinit var adapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadFavorites()
    }

    private fun setupRecyclerView() {
        adapter = FavoritesAdapter(emptyList()) { tour ->
            val bundle = Bundle().apply {
                putString("tourId", tour.id)
            }
            findNavController().navigate(R.id.action_favoritesFragment_to_tourDetailsFragment, bundle)
        }

        binding.rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFavorites.adapter = adapter

        binding.rvFavorites.addItemDecoration(object : RecyclerView.ItemDecoration() {
            private val spacing = (10 * resources.displayMetrics.density).toInt()

            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.top = spacing
                outRect.bottom = spacing
                outRect.left = spacing
                outRect.right = spacing
            }
        })
    }

    private fun loadFavorites() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Log.d("FavoritesFragment", "Загрузка избранного для пользователя: ${currentUser.uid}")
                    val favorites = repository.getUserFavorites(currentUser.uid)
                    Log.d("FavoritesFragment", "Найдено ${favorites.size} избранных туров")

                    // Проверяем, что фрагмент еще активен
                    if (!isAdded || _binding == null) return@launch

                    adapter.updateTours(favorites)

                    if (favorites.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvFavorites.visibility = View.GONE
                        binding.tvEmptyState.text = "Пока нет избранных туров\nДобавьте туры в избранное, чтобы увидеть их здесь"
                        Log.d("FavoritesFragment", "Избранные туры не найдены")
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvFavorites.visibility = View.VISIBLE
                        Log.d("FavoritesFragment", "Избранные туры успешно загружены")
                    }
                } catch (e: Exception) {
                    Log.e("FavoritesFragment", "Ошибка загрузки избранного", e)
                    e.printStackTrace()
                }
            }
        } else {
            Log.d("FavoritesFragment", "Пользователь не авторизован")
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvFavorites.visibility = View.GONE
            binding.tvEmptyState.text = "Войдите в систему, чтобы увидеть избранное"
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class FavoritesAdapter(
    private var tours: List<TourModel>,
    private val onTourClick: (TourModel) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.TourViewHolder>() {

    inner class TourViewHolder(private val binding: TourItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tour: TourModel) {
            binding.tourTitle.text = tour.title
            binding.tourDescription.text = tour.description
            // ИЗМЕНИЛ: перевел на русский и заменил доллары на рубли
            binding.tourPrice.text = "От ${tour.price.toInt()}₽ • Бесплатная отмена"

            // ИЗМЕНИЛ: используем imageResource вместо imageUrl
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

    fun updateTours(newTours: List<TourModel>) {
        tours = newTours
        notifyDataSetChanged()
    }
}