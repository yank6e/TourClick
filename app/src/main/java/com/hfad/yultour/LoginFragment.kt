package com.hfad.yultour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hfad.yultour.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeAuthState()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                authViewModel.login(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.btnGoogleLogin.setOnClickListener {
            // TODO: Реализовать Google Sign-in
            Snackbar.make(binding.root, "Google Sign-in будет реализован позже", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnAppleLogin.setOnClickListener {
            // TODO: Реализовать Apple Sign-in
            Snackbar.make(binding.root, "Apple Sign-in будет реализован позже", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeAuthState() {
        // Используем viewLifecycleOwner.lifecycleScope для безопасной работы с корутинами
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.authState.collectLatest { state ->
                // Проверяем, что фрагмент еще активен
                if (!isAdded || _binding == null) return@collectLatest

                when (state) {
                    is AuthState.Loading -> {
                        binding.btnLogin.isEnabled = false
                        binding.btnLogin.text = "Загрузка..."
                    }
                    is AuthState.Authenticated -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Войти"
                        // Успешная авторизация - переходим на главный экран
                        // Проверяем, что мы еще на экране логина
                        if (findNavController().currentDestination?.id == R.id.loginFragment) {
                            findNavController().navigate(R.id.action_loginFragment_to_tourSearchFragment)
                        }
                    }
                    is AuthState.Error -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Войти"
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    is AuthState.PasswordResetSent -> {
                        Snackbar.make(binding.root, "Письмо для сброса пароля отправлено", Snackbar.LENGTH_LONG).show()
                    }
                    else -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Войти"
                    }
                }
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Введите email"
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

        return true
    }

    private fun showForgotPasswordDialog() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Snackbar.make(binding.root, "Введите email для сброса пароля", Snackbar.LENGTH_SHORT).show()
            return
        }

        authViewModel.resetPassword(email)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}