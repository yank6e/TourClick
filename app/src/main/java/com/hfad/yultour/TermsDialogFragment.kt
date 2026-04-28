package com.hfad.yultour

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TermsDialogFragment : DialogFragment() {

    private lateinit var legalDocumentManager: LegalDocumentManager
    private var documentId: String = "offer" // По умолчанию оферта
    private var requireAcceptance: Boolean = false
    private var onAccepted: (() -> Unit)? = null

    companion object {
        private const val ARG_DOCUMENT_ID = "document_id"
        private const val ARG_REQUIRE_ACCEPTANCE = "require_acceptance"

        fun newInstance(
            documentId: String = "offer",
            requireAcceptance: Boolean = false,
            onAccepted: (() -> Unit)? = null
        ): TermsDialogFragment {
            val fragment = TermsDialogFragment()
            val args = Bundle()
            args.putString(ARG_DOCUMENT_ID, documentId)
            args.putBoolean(ARG_REQUIRE_ACCEPTANCE, requireAcceptance)
            fragment.arguments = args
            fragment.onAccepted = onAccepted
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            documentId = it.getString(ARG_DOCUMENT_ID) ?: "offer"
            requireAcceptance = it.getBoolean(ARG_REQUIRE_ACCEPTANCE, false)
        }
        legalDocumentManager = LegalDocumentManager(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_terms, null)
        val tvContent = view.findViewById<TextView>(R.id.tvContent)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)

        // Получаем заголовок
        val title = when (documentId) {
            "offer" -> "Публичная оферта"
            "privacy" -> "Политика конфиденциальности"
            "rules" -> "Правила бронирования"
            else -> "Документ"
        }
        tvTitle.text = title

        // Загружаем содержимое
        val content = legalDocumentManager.getDocumentContentSync(documentId)
        tvContent.text = content

        // Строим диалог
        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setView(view)

        if (requireAcceptance) {
            dialogBuilder
                .setPositiveButton("Принимаю") { dialog, _ ->
                    onAccepted?.invoke()
                    dialog.dismiss()
                }
                .setNegativeButton("Отклонить") { dialog, _ ->
                    dialog.dismiss()
                }
        } else {
            dialogBuilder.setPositiveButton("Закрыть") { dialog, _ ->
                dialog.dismiss()
            }
        }

        return dialogBuilder.create()
    }
}