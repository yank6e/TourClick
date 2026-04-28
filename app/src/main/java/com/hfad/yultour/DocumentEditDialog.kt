package com.hfad.yultour

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment

class DocumentEditDialog : DialogFragment() {

    interface OnDocumentUpdatedListener {
        fun onDocumentUpdated(documentType: String, number: String, expiryDate: String, details: String)
    }

    private var listener: OnDocumentUpdatedListener? = null
    private var documentType: String = ""
    private var currentData: UserDocument? = null

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_NUMBER = "number"
        private const val ARG_EXPIRY = "expiry"
        private const val ARG_DETAILS = "details"

        fun newInstance(documentType: String, currentData: UserDocument?): DocumentEditDialog {
            val fragment = DocumentEditDialog()
            val args = Bundle()
            args.putString(ARG_TYPE, documentType)

            currentData?.let {
                args.putString(ARG_NUMBER, it.number)
                args.putString(ARG_EXPIRY, it.expiryDate)
                args.putString(ARG_DETAILS, it.details)
            }

            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let {
            documentType = it.getString(ARG_TYPE) ?: ""
            val number = it.getString(ARG_NUMBER) ?: ""
            val expiryDate = it.getString(ARG_EXPIRY) ?: ""
            val details = it.getString(ARG_DETAILS) ?: ""

            if (number.isNotEmpty() || expiryDate.isNotEmpty() || details.isNotEmpty()) {
                currentData = UserDocument(
                    type = documentType,
                    number = number,
                    expiryDate = expiryDate,
                    details = details
                )
            }
        }

        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_edit_document, null)

        val etNumber = view.findViewById<EditText>(R.id.etNumber)
        val etExpiryDate = view.findViewById<EditText>(R.id.etExpiryDate)
        val etDetails = view.findViewById<EditText>(R.id.etDetails)

        // Заполняем текущие значения
        currentData?.let {
            etNumber.setText(it.number)
            etExpiryDate.setText(it.expiryDate)
            etDetails.setText(it.details)
        }

        // Определяем заголовок и подсказки
        val title = when (documentType) {
            "passport" -> "Редактировать паспорт"
            "driver_license" -> "Редактировать водительское удостоверение"
            "insurance" -> "Редактировать страховку"
            else -> "Редактировать документ"
        }

        // Устанавливаем подсказки
        when (documentType) {
            "passport" -> {
                etNumber.hint = "Серия и номер (например: 45 67 123456)"
                etDetails.hint = "Кем и когда выдан"
            }
            "driver_license" -> {
                etNumber.hint = "Номер удостоверения"
                etDetails.hint = "Категории"
            }
            "insurance" -> {
                etNumber.hint = "Номер полиса"
                etDetails.hint = "Страховая компания"
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton("Сохранить") { dialog, which ->
                val number = etNumber.text.toString().trim()
                val expiryDate = etExpiryDate.text.toString().trim()
                val details = etDetails.text.toString().trim()

                if (number.isNotEmpty()) {
                    listener?.onDocumentUpdated(documentType, number, expiryDate, details)
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
    }

    fun setOnDocumentUpdatedListener(listener: OnDocumentUpdatedListener) {
        this.listener = listener
    }
}