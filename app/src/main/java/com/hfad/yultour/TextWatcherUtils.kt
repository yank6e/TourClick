package com.hfad.yultour

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

object TextWatcherUtils {

    fun createNumberMask(maxLength: Int, editText: EditText, onComplete: ((String) -> Unit)? = null): TextWatcher {
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

    fun createDateMask(editText: EditText, onComplete: ((String) -> Unit)? = null): TextWatcher {
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

    fun createDepartmentCodeMask(editText: EditText): TextWatcher {
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
                        formatted += cleaned[i]
                    }

                    // Ограничиваем длину 7 символами (XXX-XXX)
                    val limited = if (formatted.length > 7) formatted.substring(0, 7) else formatted

                    current = limited
                    editText.setText(limited)
                    editText.setSelection(limited.length)

                    editText.addTextChangedListener(this)
                }
            }
        }
    }
}