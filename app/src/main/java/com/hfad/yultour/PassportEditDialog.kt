package com.hfad.yultour

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.Locale

class PassportEditDialog : DialogFragment() {

    interface OnPassportUpdatedListener {
        fun onPassportUpdated(passportData: UserDocument)
    }

    private var listener: OnPassportUpdatedListener? = null
    private var currentData: UserDocument? = null

    companion object {
        fun newInstance(currentData: UserDocument?): PassportEditDialog {
            val fragment = PassportEditDialog()
            val args = Bundle()
            if (currentData != null) {
                args.putParcelable("passport_data", currentData)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        currentData = arguments?.getParcelable("passport_data")

        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_edit_passport, null)

        // Получаем все поля
        val etSeries = view.findViewById<EditText>(R.id.etSeries)
        val etNumber = view.findViewById<EditText>(R.id.etNumber)
        val etIssuedBy = view.findViewById<EditText>(R.id.etIssuedBy)
        val etIssueDate = view.findViewById<EditText>(R.id.etIssueDate)
        val etDepartmentCode = view.findViewById<EditText>(R.id.etDepartmentCode)
        val etBirthDate = view.findViewById<EditText>(R.id.etBirthDate)
        val etBirthPlace = view.findViewById<EditText>(R.id.etBirthPlace)

        // Заполняем текущие значения
        currentData?.let {
            etSeries.setText(it.passportSeries)
            etNumber.setText(it.passportNumber)
            etIssuedBy.setText(it.passportIssuedBy)
            etIssueDate.setText(it.passportIssueDate)
            etDepartmentCode.setText(it.passportDepartmentCode)
            etBirthDate.setText(it.passportBirthDate)
            etBirthPlace.setText(it.passportBirthPlace)
        }

        // Настраиваем маски ввода
        setupInputMasks(etSeries, etNumber, etIssuedBy, etIssueDate, etDepartmentCode, etBirthDate, etBirthPlace)

        return AlertDialog.Builder(requireContext())
            .setTitle("Паспортные данные")
            .setView(view)
            .setPositiveButton("Сохранить") { dialog, which ->
                if (validateInput(etSeries, etNumber, etIssueDate, etDepartmentCode, etBirthDate)) {
                    val passportData = UserDocument(
                        type = "passport",
                        passportSeries = etSeries.text.toString().trim(),
                        passportNumber = etNumber.text.toString().trim(),
                        passportIssuedBy = etIssuedBy.text.toString().trim(),
                        passportIssueDate = etIssueDate.text.toString().trim(),
                        passportDepartmentCode = etDepartmentCode.text.toString().trim(),
                        passportBirthDate = etBirthDate.text.toString().trim(),
                        passportBirthPlace = etBirthPlace.text.toString().trim(),
                        number = "${etSeries.text.toString().trim()} ${etNumber.text.toString().trim()}"
                    )
                    listener?.onPassportUpdated(passportData)
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
    }

    private fun setupInputMasks(
        etSeries: EditText,
        etNumber: EditText,
        etIssuedBy: EditText,
        etIssueDate: EditText,
        etDepartmentCode: EditText,
        etBirthDate: EditText,
        etBirthPlace: EditText
    ) {
        // Серия паспорта - 4 цифры
        etSeries.addTextChangedListener(createNumberMask(4, etSeries) { text ->
            if (text.length == 4) {
                etNumber.requestFocus()
            }
        })

        // Номер паспорта - 6 цифр
        etNumber.addTextChangedListener(createNumberMask(6, etNumber) { text ->
            if (text.length == 6) {
                etIssuedBy.requestFocus()
            }
        })

        // Дата выдачи - ДД.ММ.ГГГГ
        etIssueDate.addTextChangedListener(createDateMask(etIssueDate) { text ->
            if (text.length == 10) {
                etDepartmentCode.requestFocus()
            }
        })

        // Код подразделения - XXX-XXX (6 цифр)
        etDepartmentCode.addTextChangedListener(createDepartmentCodeMask(etDepartmentCode) { text ->
            if (text.replace("-", "").length == 6) {
                etBirthDate.requestFocus()
            }
        })

        // Дата рождения - ДД.ММ.ГГГГ
        etBirthDate.addTextChangedListener(createDateMask(etBirthDate) { text ->
            if (text.length == 10) {
                etBirthPlace.requestFocus()
            }
        })

        // Настройка действия "Готово" на клавиатуре
        etBirthPlace.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateAndSave(
                    etSeries, etNumber, etIssueDate, etDepartmentCode, etBirthDate,
                    etIssuedBy, etBirthPlace
                )
                true
            } else {
                false
            }
        }
    }

    private fun validateInput(
        etSeries: EditText,
        etNumber: EditText,
        etIssueDate: EditText,
        etDepartmentCode: EditText,
        etBirthDate: EditText
    ): Boolean {
        var isValid = true

        // Проверка серии (4 цифры)
        if (etSeries.text.length != 4) {
            etSeries.error = "Серия должна содержать 4 цифры"
            isValid = false
        } else {
            etSeries.error = null
        }

        // Проверка номера (6 цифр)
        if (etNumber.text.length != 6) {
            etNumber.error = "Номер должен содержать 6 цифр"
            isValid = false
        } else {
            etNumber.error = null
        }

        // Проверка даты выдачи
        if (!isValidDate(etIssueDate.text.toString())) {
            etIssueDate.error = "Некорректная дата (формат: ДД.ММ.ГГГГ)"
            isValid = false
        } else {
            etIssueDate.error = null
        }

        // Проверка кода подразделения (6 цифр)
        val code = etDepartmentCode.text.toString().replace("-", "")
        if (code.length != 6) {
            etDepartmentCode.error = "Код должен содержать 6 цифр (XXX-XXX)"
            isValid = false
        } else {
            etDepartmentCode.error = null
        }

        // Проверка даты рождения
        if (!isValidDate(etBirthDate.text.toString())) {
            etBirthDate.error = "Некорректная дата (формат: ДД.ММ.ГГГГ)"
            isValid = false
        } else {
            etBirthDate.error = null
        }

        if (!isValid) {
            Toast.makeText(requireContext(), "Пожалуйста, исправьте ошибки", Toast.LENGTH_SHORT).show()
        }

        return isValid
    }

    private fun validateAndSave(
        etSeries: EditText,
        etNumber: EditText,
        etIssueDate: EditText,
        etDepartmentCode: EditText,
        etBirthDate: EditText,
        etIssuedBy: EditText,
        etBirthPlace: EditText
    ) {
        // Логика для валидации и сохранения
        if (validateInput(etSeries, etNumber, etIssueDate, etDepartmentCode, etBirthDate)) {
            val passportData = UserDocument(
                type = "passport",
                passportSeries = etSeries.text.toString().trim(),
                passportNumber = etNumber.text.toString().trim(),
                passportIssuedBy = etIssuedBy.text.toString().trim(),
                passportIssueDate = etIssueDate.text.toString().trim(),
                passportDepartmentCode = etDepartmentCode.text.toString().trim(),
                passportBirthDate = etBirthDate.text.toString().trim(),
                passportBirthPlace = etBirthPlace.text.toString().trim(),
                number = "${etSeries.text.toString().trim()} ${etNumber.text.toString().trim()}"
            )
            listener?.onPassportUpdated(passportData)
            dismiss()
        }
    }

    private fun isValidDate(date: String): Boolean {
        if (date.length != 10) return false
        try {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            format.isLenient = false
            format.parse(date)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    // Встроенные функции для масок ввода (чтобы не зависеть от TextWatcherUtils)

    private fun createNumberMask(maxLength: Int, editText: EditText, onComplete: ((String) -> Unit)? = null): TextWatcher {
        return object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    editText.removeTextChangedListener(this)

                    // Удаляем все не-цифры
                    val cleaned = s.toString().filter { it.isDigit() }

                    // Ограничиваем длину
                    val limited = if (cleaned.length > maxLength) cleaned.substring(0, maxLength) else cleaned

                    current = limited
                    editText.setText(limited)
                    editText.setSelection(limited.length)

                    editText.addTextChangedListener(this)

                    onComplete?.invoke(limited)
                }
            }
        }
    }

    private fun createDateMask(editText: EditText, onComplete: ((String) -> Unit)? = null): TextWatcher {
        return object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    editText.removeTextChangedListener(this)

                    val cleaned = s.toString().filter { it.isDigit() }
                    var formatted = ""

                    for (i in cleaned.indices) {
                        when (i) {
                            1, 3 -> formatted += "${cleaned[i]}."
                            else -> formatted += cleaned[i]
                        }
                    }

                    // Ограничиваем длину 10 символами (ДД.ММ.ГГГГ)
                    val limited = if (formatted.length > 10) formatted.substring(0, 10) else formatted

                    current = limited
                    editText.setText(limited)
                    editText.setSelection(limited.length)

                    editText.addTextChangedListener(this)

                    onComplete?.invoke(limited)
                }
            }
        }
    }

    private fun createDepartmentCodeMask(editText: EditText, onComplete: ((String) -> Unit)? = null): TextWatcher {
        return object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    editText.removeTextChangedListener(this)

                    val cleaned = s.toString().filter { it.isDigit() }
                    var formatted = ""

                    for (i in cleaned.indices) {
                        if (i == 3) formatted += "-"
                        if (i < 6) formatted += cleaned[i]
                    }

                    // Ограничиваем длину 7 символами (XXX-XXX)
                    val limited = if (formatted.length > 7) formatted.substring(0, 7) else formatted

                    current = limited
                    editText.setText(limited)
                    editText.setSelection(limited.length)

                    editText.addTextChangedListener(this)

                    onComplete?.invoke(limited)
                }
            }
        }
    }

    fun setOnPassportUpdatedListener(listener: OnPassportUpdatedListener) {
        this.listener = listener
    }
}