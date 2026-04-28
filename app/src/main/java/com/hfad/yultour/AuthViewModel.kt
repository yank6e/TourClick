package com.hfad.yultour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        // Проверяем, авторизован ли пользователь при создании ViewModel
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = auth.currentUser
        _currentUser.value = user
        if (user != null) {
            _authState.value = AuthState.Authenticated(user)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            } ?: run {
                _authState.value = AuthState.Error("Пользователь не найден")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Ошибка авторизации")
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String
    ) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                // Обновляем профиль
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName("$firstName $lastName")
                    .build()
                user.updateProfile(profileUpdates).await()

                // Сохраняем дополнительную информацию в Firestore
                val userData = hashMapOf(
                    "userId" to user.uid,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email,
                    "phone" to phone,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "memberSince" to 2022
                )

                db.collection("users").document(user.uid)
                    .set(userData)
                    .await()

                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.Error("Не удалось создать пользователя")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Ошибка регистрации")
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    fun resetPassword(email: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            auth.sendPasswordResetEmail(email).await()
            _authState.value = AuthState.PasswordResetSent
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Ошибка сброса пароля")
        }
    }

    // Функция для проверки авторизации без перехода
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
    object PasswordResetSent : AuthState()
}