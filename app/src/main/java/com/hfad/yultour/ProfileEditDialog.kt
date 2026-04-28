package com.hfad.yultour

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment

class ProfileEditDialog : DialogFragment() {

    interface OnProfileUpdatedListener {
        fun onProfileUpdated(field: String, value: String)
    }

    private var listener: OnProfileUpdatedListener? = null
    private var fieldName: String = ""
    private var currentValue: String = ""

    companion object {
        private const val ARG_FIELD = "field"
        private const val ARG_VALUE = "value"

        fun newInstance(field: String, currentValue: String): ProfileEditDialog {
            val fragment = ProfileEditDialog()
            val args = Bundle()
            args.putString(ARG_FIELD, field)
            args.putString(ARG_VALUE, currentValue)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            fieldName = it.getString(ARG_FIELD) ?: ""
            currentValue = it.getString(ARG_VALUE) ?: ""
        }

        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_edit_profile, null)
        val etValue = view.findViewById<EditText>(R.id.etValue)

        // Устанавливаем текущее значение
        etValue.setText(currentValue)
        etValue.setSelection(etValue.text.length)

        // Определяем заголовок диалога
        val title = when (fieldName) {
            "firstName" -> "Редактировать имя"
            "lastName" -> "Редактировать фамилию"
            "phone" -> "Редактировать телефон"
            "email" -> "Редактировать email"
            else -> "Редактировать $fieldName"
        }

        // Определяем тип ввода
        when (fieldName) {
            "phone" -> etValue.inputType = android.text.InputType.TYPE_CLASS_PHONE
            "email" -> etValue.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton("Сохранить") { dialog, which ->
                val newValue = etValue.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    listener?.onProfileUpdated(fieldName, newValue)
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
    }

    fun setOnProfileUpdatedListener(listener: OnProfileUpdatedListener) {
        this.listener = listener
    }
}