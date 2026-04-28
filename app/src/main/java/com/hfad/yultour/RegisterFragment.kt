package com.hfad.yultour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hfad.yultour.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private var isOfferAccepted = false
    private var isPrivacyAccepted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeAuthState()
        setupCheckboxes()
    }

    private fun setupCheckboxes() {
        binding.cbOffer.setOnCheckedChangeListener { _, isChecked ->
            isOfferAccepted = isChecked
            updateRegisterButton()
        }

        binding.cbPrivacy.setOnCheckedChangeListener { _, isChecked ->
            isPrivacyAccepted = isChecked
            updateRegisterButton()
        }
    }

    private fun updateRegisterButton() {
        binding.btnRegister.isEnabled = isOfferAccepted && isPrivacyAccepted
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(firstName, lastName, email, phone, password, confirmPassword)) {
                authViewModel.register(firstName, lastName, email, phone, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // Ссылка на публичную оферту
        binding.tvOfferLink.setOnClickListener {
            val dialog = TermsDialogFragment.newInstance("offer")
            dialog.show(parentFragmentManager, "offer_dialog")
        }

        // Ссылка на политику конфиденциальности
        binding.tvPrivacyLink.setOnClickListener {
            val dialog = TermsDialogFragment.newInstance("privacy")
            dialog.show(parentFragmentManager, "privacy_dialog")
        }

        // Ссылка на правила бронирования
        binding.tvRulesLink.setOnClickListener {
            val dialog = TermsDialogFragment.newInstance("rules")
            dialog.show(parentFragmentManager, "rules_dialog")
        }
    }

    private fun validateInput(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        // Проверка согласия с документами
        if (!isOfferAccepted) {
            Snackbar.make(binding.root, "Необходимо принять публичную оферту", Snackbar.LENGTH_SHORT).show()
            return false
        }

        if (!isPrivacyAccepted) {
            Snackbar.make(binding.root, "Необходимо принять политику конфиденциальности", Snackbar.LENGTH_SHORT).show()
            return false
        }

        // Проверка полей ввода
        if (firstName.isEmpty()) {
            binding.etFirstName.error = "Введите имя"
            return false
        }

        if (lastName.isEmpty()) {
            binding.etLastName.error = "Введите фамилию"
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Введите email"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Введите корректный email"
            return false
        }

        if (phone.isEmpty()) {
            binding.etPhone.error = "Введите телефон"
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Введите пароль"
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "Пароль должен содержать минимум 6 символов"
            return false
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Пароли не совпадают"
            return false
        }

        return true
    }

    private fun observeAuthState() {
        lifecycleScope.launchWhenStarted {
            authViewModel.authState.collectLatest { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.btnRegister.isEnabled = false
                        binding.btnRegister.text = "Регистрация..."
                    }
                    is AuthState.Authenticated -> {
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = "Зарегистрироваться"
                        // Успешная регистрация - переходим на главный экран
                        findNavController().navigate(R.id.action_registerFragment_to_tourSearchFragment)
                    }
                    is AuthState.Error -> {
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = "Зарегистрироваться"
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    else -> {
                        binding.btnRegister.isEnabled = true
                        binding.btnRegister.text = "Зарегистрироваться"
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // При возврате на экран регистрации сбрасываем состояние
        updateRegisterButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}