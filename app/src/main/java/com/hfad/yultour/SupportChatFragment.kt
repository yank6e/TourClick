package com.hfad.yultour

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.hfad.yultour.databinding.FragmentSupportChatBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SupportChatFragment : Fragment() {

    private var _binding: FragmentSupportChatBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseRepository()
    private lateinit var adapter: MessagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupportChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SupportChat", "onViewCreated - binding: ${binding != null}")
        Log.d("SupportChat", "onViewCreated - rvMessages: ${binding.rvMessages}")

        // Проверьте видимость RecyclerView
        binding.rvMessages.post {
            Log.d("SupportChat", "RecyclerView dimensions: ${binding.rvMessages.width}x${binding.rvMessages.height}")
            Log.d("SupportChat", "RecyclerView isShown: ${binding.rvMessages.isShown}")
            Log.d("SupportChat", "RecyclerView visibility: ${binding.rvMessages.visibility}")
        }

        setupRecyclerView()
        setupClickListeners()
        observeMessages()
    }

    private fun setupRecyclerView() {
        adapter = MessagesAdapter(emptyList())

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true // Важно для чата

        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter

        // Добавьте это для автоматической прокрутки
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == adapter.itemCount - 1) {
                    binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }

        // Отправка по нажатию Enter
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                val message = binding.etMessage.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessage(message)
                }
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage(message: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Блокируем кнопку отправки
            binding.btnSend.isEnabled = false
            binding.etMessage.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Отправляем сообщение пользователя
                    repository.sendSupportMessage(currentUser.uid, message, true)

                    // Очищаем поле ввода
                    binding.etMessage.setText("")

                    // Автоматический ответ от поддержки (симуляция)
                    simulateSupportResponse(currentUser.uid)

                } catch (e: Exception) {
                    // Показываем ошибку
                    showError("Не удалось отправить сообщение: ${e.message}")
                } finally {
                    // Разблокируем кнопку и поле
                    binding.btnSend.isEnabled = true
                    binding.etMessage.isEnabled = true
                    binding.etMessage.requestFocus()
                }
            }
        } else {
            showError("Пожалуйста, войдите в систему")
        }
    }

    private fun simulateSupportResponse(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Задержка для имитации ответа поддержки
                kotlinx.coroutines.delay(1500)

                // Ответы в зависимости от времени суток
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val greeting = when {
                    hour < 6 -> "Доброй ночи!"
                    hour < 12 -> "Доброе утро!"
                    hour < 18 -> "Добрый день!"
                    else -> "Добрый вечер!"
                }

                val responses = listOf(
                    "$greeting Спасибо за ваше сообщение! Наша команда поддержки свяжется с вами в течение 15 минут.",
                    "$greeting Мы получили ваше сообщение. Наш специалист уже изучает ваш вопрос.",
                    "Благодарим за обращение! Мы ответим вам как можно скорее.",
                    "Ваше сообщение доставлено. Среднее время ответа - 10-15 минут."
                )

                val randomResponse = responses.random()
                repository.sendSupportMessage(userId, randomResponse, false)

            } catch (e: Exception) {
                // Не показываем ошибку пользователю, если авто-ответ не отправился
                android.util.Log.e("SupportChat", "Error sending auto-response: ${e.message}")
            }
        }
    }

    private fun observeMessages() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                // Подписываемся на поток сообщений в реальном времени
                repository.getSupportMessagesFlow(currentUser.uid).collectLatest { messages ->
                    adapter.updateMessages(messages)

                    // Прокручиваем к последнему сообщению
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.postDelayed({
                            binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                        }, 100)
                    }

                    // Показываем/скрываем состояние пустого чата
                    if (messages.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                    }
                }
            }
        } else {
            showEmptyState()
            binding.etMessage.isEnabled = false
            binding.btnSend.isEnabled = false
            showError("Войдите в систему для использования чата")
        }
    }

    private fun showEmptyState() {
        // Можно добавить TextView для пустого состояния
        binding.rvMessages.visibility = View.GONE
        // binding.tvEmptyChat.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        binding.rvMessages.visibility = View.VISIBLE
        // binding.tvEmptyChat.visibility = View.GONE
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Фокусируемся на поле ввода при открытии чата
        binding.etMessage.requestFocus()

        // Показываем клавиатуру
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etMessage, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onPause() {
        super.onPause()
        // Скрываем клавиатуру при уходе с экрана
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etMessage.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}