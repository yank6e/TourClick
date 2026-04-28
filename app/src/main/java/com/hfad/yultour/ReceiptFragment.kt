package com.hfad.yultour

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.hfad.yultour.databinding.FragmentReceiptBinding

class ReceiptFragment : Fragment() {

    private var _binding: FragmentReceiptBinding? = null
    private val binding get() = _binding!!

    // ✅ ИСПРАВЛЕНО: правильное получение аргументов
    private val args: ReceiptFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ ИСПРАВЛЕНО: проверяем что view существуют перед использованием
        binding.tvReceiptText.text = args.receiptText
        binding.tvTransactionId.text = "Транзакция: ${args.transactionId}"

        // ✅ ИСПРАВЛЕНО: правильное преобразование типа
        val amountValue = args.amount
        binding.tvAmount.text = "${args.amount.toInt()} ₽"

        binding.tvTourTitle.text = args.tourTitle

        binding.btnDone.setOnClickListener {
            findNavController().navigate(R.id.action_receiptFragment_to_tourSearchFragment)
        }

        binding.btnShare.setOnClickListener {
            // TODO: Реализовать partage чека
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}