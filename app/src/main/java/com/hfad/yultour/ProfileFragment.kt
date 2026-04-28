package com.hfad.yultour

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.hfad.yultour.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(), ProfileEditDialog.OnProfileUpdatedListener,
    DocumentEditDialog.OnDocumentUpdatedListener {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseRepository()
    private val authViewModel: AuthViewModel by viewModels()

    private var currentProfile: UserProfile? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadUserData()
    }

    private fun setupClickListeners() {
        binding.btnSupport.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_supportChatFragment)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Кнопки редактирования персональной информации
        binding.btnEditFirstName.setOnClickListener {
            showEditDialog("firstName", binding.tvFirstName.text.toString())
        }
        binding.btnEditLastName.setOnClickListener {
            showEditDialog("lastName", binding.tvLastName.text.toString())
        }
        binding.btnEditPhone.setOnClickListener {
            showEditDialog("phone", binding.tvPhone.text.toString())
        }
        binding.btnEditEmail.setOnClickListener {
            showEditDialog("email", binding.tvEmail.text.toString())
        }

        // Кнопки редактирования документов
        binding.btnReplacePassport.setOnClickListener {
            val passportDoc = currentProfile?.documents?.find { it.type == "passport" }
            val dialog = PassportEditDialog.newInstance(passportDoc)
            dialog.setOnPassportUpdatedListener(object : PassportEditDialog.OnPassportUpdatedListener {
                override fun onPassportUpdated(passportData: UserDocument) {
                    saveDocument(passportData)
                }
            })
            dialog.show(parentFragmentManager, "passport_dialog")
        }
        binding.btnReplaceLicense.setOnClickListener {
            val licenseDoc = currentProfile?.documents?.find { it.type == "driver_license" }
            showDocumentEditDialog("driver_license", licenseDoc)
        }
        binding.btnReplaceInsurance.setOnClickListener {
            val insuranceDoc = currentProfile?.documents?.find { it.type == "insurance" }
            showDocumentEditDialog("insurance", insuranceDoc)
        }

        // Кнопка добавления документа
        binding.btnAddDocument.setOnClickListener {
            showAddDocumentDialog()
        }

        // Кнопки редактирования настроек
        binding.btnEditNotifications.setOnClickListener {
            showNotificationsDialog()
        }
        binding.btnEditLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.btnLegalDocuments.setOnClickListener {
            showLegalDocumentsDialog()
        }

    }

    private fun showLogoutConfirmation() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Выход из системы")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLogout() {
        (requireActivity() as MainActivity).logout()
    }

    private fun showLegalDocumentsDialog() {
        val items = arrayOf(
            "📄 Публичная оферта",
            "🔒 Политика конфиденциальности",
            "📋 Правила бронирования"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Юридические документы")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showTermsDialog("offer")
                    1 -> showTermsDialog("privacy")
                    2 -> showTermsDialog("rules")
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showTermsDialog(documentId: String) {
        val dialog = TermsDialogFragment.newInstance(documentId)
        dialog.show(parentFragmentManager, "${documentId}_dialog")
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                try {
                    currentProfile = repository.getUserProfile(currentUser.uid)
                    currentProfile?.let { profile ->
                        updateUI(profile)
                    } ?: run {
                        // Создаем начальный профиль если нет
                        createInitialProfile(currentUser)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveDocument(document: UserDocument) {
        val user = auth.currentUser ?: return
        currentProfile?.let { profile ->
            val updatedDocuments = profile.documents.toMutableList()
            val existingIndex = updatedDocuments.indexOfFirst { it.type == document.type }

            // Если это паспорт, форматируем данные для отображения
            if (document.type == "passport") {
                // Создаем форматированный номер для отображения
                val formattedNumber = if (document.passportSeries.isNotEmpty() && document.passportNumber.isNotEmpty()) {
                    "${document.passportSeries} ${document.passportNumber}"
                } else {
                    document.number
                }

                // Обновляем номер для общего отображения
                val updatedDocument = document.copy(number = formattedNumber)

                if (existingIndex != -1) {
                    updatedDocuments[existingIndex] = updatedDocument
                } else {
                    updatedDocuments.add(updatedDocument)
                }
            } else {
                if (existingIndex != -1) {
                    updatedDocuments[existingIndex] = document
                } else {
                    updatedDocuments.add(document)
                }
            }

            val updatedProfile = profile.copy(documents = updatedDocuments)
            currentProfile = updatedProfile
            updateUI(updatedProfile)

            lifecycleScope.launch {
                try {
                    repository.updateUserProfile(user.uid, updatedProfile)
                    Toast.makeText(requireContext(), "Документ сохранен", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI(profile: UserProfile) {
        binding.tvUserName.text = "${profile.firstName} ${profile.lastName}"
        binding.tvFirstName.text = profile.firstName
        binding.tvLastName.text = profile.lastName
        binding.tvPhone.text = profile.phone
        binding.tvEmail.text = profile.email
        binding.tvMemberInfo.text = "Пользователь с ${profile.memberSince} • Подтвержден"

        // Обновляем документы
        updateDocumentsUI(profile.documents)
    }

    private fun updateDocumentsUI(documents: List<UserDocument>) {
        // Паспорт
        val passport = documents.find { it.type == "passport" }
        binding.tvPassport.text = passport?.let {
            // Форматируем данные паспорта
            val seriesNumber = if (it.passportSeries.isNotEmpty() && it.passportNumber.isNotEmpty()) {
                "${it.passportSeries} ${it.passportNumber}"
            } else {
                it.number
            }

            val issuedBy = if (it.passportIssuedBy.isNotEmpty()) " • ${it.passportIssuedBy}" else ""
            val issueDate = if (it.passportIssueDate.isNotEmpty()) " • Выдан: ${it.passportIssueDate}" else ""

            "Паспорт: $seriesNumber$issuedBy$issueDate"
        } ?: "Паспорт не указан"

        // Водительское удостоверение
        val license = documents.find { it.type == "driver_license" }
        binding.tvDriverLicense.text = license?.let {
            val categories = if (it.licenseCategory.isNotEmpty()) " • Категории: ${it.licenseCategory}" else ""
            val expiry = if (it.expiryDate.isNotEmpty()) " • Истекает: ${it.expiryDate}" else ""
            "Водительское удостоверение: ${it.number}$categories$expiry"
        } ?: "Водительское удостоверение не указано"

        // Страховка
        val insurance = documents.find { it.type == "insurance" }
        binding.tvInsurance.text = insurance?.let {
            val company = if (it.insuranceCompany.isNotEmpty()) "${it.insuranceCompany} • " else ""
            val expiry = if (it.expiryDate.isNotEmpty()) " • Истекает: ${it.expiryDate}" else ""
            "Страховка: $company${it.insurancePolicyNumber}$expiry"
        } ?: "Страховка не указана"
    }

    private fun createInitialProfile(user: com.google.firebase.auth.FirebaseUser) {
        val nameParts = user.displayName?.split(" ") ?: listOf("Пользователь")
        currentProfile = UserProfile(
            userId = user.uid,
            firstName = nameParts.getOrElse(0) { "Пользователь" },
            lastName = nameParts.getOrElse(1) { "" },
            email = user.email ?: "",
            phone = "",
            memberSince = 2025,
            documents = emptyList()
        )

        updateUI(currentProfile!!)

        // Сохраняем начальный профиль
        lifecycleScope.launch {
            try {
                repository.updateUserProfile(user.uid, currentProfile!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showEditDialog(field: String, currentValue: String) {
        val dialog = ProfileEditDialog.newInstance(field, currentValue)
        dialog.setOnProfileUpdatedListener(this)
        dialog.show(parentFragmentManager, "edit_profile_dialog")
    }

    private fun showDocumentEditDialog(documentType: String, currentData: UserDocument?) {
        val dialog = DocumentEditDialog.newInstance(documentType, currentData)
        dialog.setOnDocumentUpdatedListener(this)
        dialog.show(parentFragmentManager, "edit_document_dialog")
    }

    private fun showAddDocumentDialog() {
        val documentTypes = arrayOf("Паспорт", "Водительское удостоверение", "Страховка")

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Добавить документ")
            .setItems(documentTypes) { dialog, which ->
                val type = when (which) {
                    0 -> "passport"
                    1 -> "driver_license"
                    2 -> "insurance"
                    else -> "passport"
                }
                showDocumentEditDialog(type, null)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showNotificationsDialog() {
        android.widget.Toast.makeText(
            requireContext(),
            "Настройки уведомлений будут реализованы позже",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun showLanguageDialog() {
        android.widget.Toast.makeText(
            requireContext(),
            "Выбор языка будет реализован позже",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // Реализация интерфейсов для обработки обновлений

    override fun onProfileUpdated(field: String, value: String) {
        val user = auth.currentUser ?: return
        currentProfile?.let { profile ->
            val updatedProfile = when (field) {
                "firstName" -> profile.copy(firstName = value)
                "lastName" -> profile.copy(lastName = value)
                "phone" -> profile.copy(phone = value)
                "email" -> profile.copy(email = value)
                else -> profile
            }

            currentProfile = updatedProfile
            updateUI(updatedProfile)

            // Сохраняем в Firebase
            lifecycleScope.launch {
                try {
                    repository.updateUserProfile(user.uid, updatedProfile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Ошибка сохранения: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDocumentUpdated(documentType: String, number: String, expiryDate: String, details: String) {
        val user = auth.currentUser ?: return
        currentProfile?.let { profile ->
            val newDocument = UserDocument(
                type = documentType,
                number = number,
                expiryDate = expiryDate,
                details = details
            )

            // Обновляем или добавляем документ
            val updatedDocuments = profile.documents.toMutableList()
            val existingIndex = updatedDocuments.indexOfFirst { it.type == documentType }

            if (existingIndex != -1) {
                updatedDocuments[existingIndex] = newDocument
            } else {
                updatedDocuments.add(newDocument)
            }

            val updatedProfile = profile.copy(documents = updatedDocuments)
            currentProfile = updatedProfile
            updateUI(updatedProfile)

            // Сохраняем в Firebase
            lifecycleScope.launch {
                try {
                    repository.updateUserProfile(user.uid, updatedProfile)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Документ сохранен",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Ошибка сохранения: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}