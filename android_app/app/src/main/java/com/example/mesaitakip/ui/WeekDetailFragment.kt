package com.example.mesaitakip.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.mesaitakip.MesaiApp
import com.example.mesaitakip.data.entities.OvertimeRecord
import com.example.mesaitakip.databinding.FragmentWeekDetailBinding
import com.example.mesaitakip.ui.adapter.RecordAdapter
import com.example.mesaitakip.ui.viewmodel.MainViewModel
import com.example.mesaitakip.ui.viewmodel.MainViewModelFactory

class WeekDetailFragment : Fragment() {
    private var _binding: FragmentWeekDetailBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory((requireActivity().application as MesaiApp).overtimeRepository)
    }

    private lateinit var adapter: RecordAdapter
    private var weekId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeekDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weekId = arguments?.getInt("weekId") ?: -1
        if (weekId == -1) return

        adapter = RecordAdapter { record ->
            mainViewModel.updateRecord(record)
            android.widget.Toast.makeText(context, "Kaydedildi", android.widget.Toast.LENGTH_SHORT).show()
        }
        binding.rvRecords.adapter = adapter

        mainViewModel.currentWeekRecords.observe(viewLifecycleOwner) { records ->
            adapter.submitList(records)
        }

        mainViewModel.loadRecords(weekId)

        binding.btnGenerateReport.setOnClickListener {
            generateAndShareReport()
        }
    }

    private fun generateAndShareReport() {
        val records = mainViewModel.currentWeekRecords.value ?: return
        if (records.isEmpty()) return

        val report = StringBuilder()
        report.append("MESAI RAPORU\n")
        report.append("----------------\n")

        var totalNormal = 0.0
        var totalHoliday = 0.0

        records.forEach { record ->
            val hours = record.saat.toDoubleOrNull() ?: 0.0
            if (record.is_resmi_tatil == 1) {
                totalHoliday += hours
            } else {
                totalNormal += hours
            }
            report.append("${record.tarih}: ${record.saat} Saat")
            if (record.is_resmi_tatil == 1) report.append(" (Tatil)")
            if (record.aciklama.isNotEmpty()) report.append(" - ${record.aciklama}")
            report.append("\n")
        }

        report.append("----------------\n")
        report.append("Toplam Normal Mesai: $totalNormal Saat\n")
        report.append("Toplam Tatil Mesaisi: $totalHoliday Saat\n")
        report.append("Genel Toplam: ${totalNormal + totalHoliday} Saat\n")

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, report.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Raporu Paylaş")
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
