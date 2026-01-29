package com.example.mesaitakip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.mesaitakip.MesaiApp
import com.example.mesaitakip.R
import com.example.mesaitakip.data.entities.OvertimeRecord
import com.example.mesaitakip.data.entities.Week
import com.example.mesaitakip.databinding.FragmentDashboardBinding
import com.example.mesaitakip.ui.adapter.WeekAdapter
import com.example.mesaitakip.ui.viewmodel.AuthViewModel
import com.example.mesaitakip.ui.viewmodel.AuthViewModelFactory
import com.example.mesaitakip.ui.viewmodel.MainViewModel
import com.example.mesaitakip.ui.viewmodel.MainViewModelFactory
import com.example.mesaitakip.util.DateUtils
import java.util.*

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory((requireActivity().application as MesaiApp).userRepository)
    }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory((requireActivity().application as MesaiApp).overtimeRepository)
    }

    private lateinit var adapter: WeekAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = authViewModel.currentUser.value
        if (user == null) {
            findNavController().navigate(R.id.action_dashboardFragment_to_loginFragment)
            return
        }

        binding.tvWelcome.text = "Hoşgeldin, ${user.adsoyad}"
        if (user.is_admin == 1) {
            binding.btnAdmin.visibility = View.VISIBLE
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_dashboardFragment_to_loginFragment)
        }

        binding.btnAdmin.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_adminFragment)
        }

        adapter = WeekAdapter(
            onClick = { week ->
                val bundle = Bundle().apply { putInt("weekId", week.id) }
                findNavController().navigate(R.id.action_dashboardFragment_to_weekDetailFragment, bundle)
            },
            onDelete = { week ->
                mainViewModel.deleteWeek(week)
            }
        )
        binding.rvWeeks.adapter = adapter

        mainViewModel.weeks.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        mainViewModel.loadWeeks(user.id)

        binding.btnAddWeek.setOnClickListener {
            addNewWeek(user.id, user.adsoyad)
        }
    }

    private fun addNewWeek(userId: Int, fullName: String) {
        val monday = DateUtils.getMondayOfCurrentWeek()
        val week = Week(
            hafta_baslangic = DateUtils.formatDateForDb(monday),
            hafta_araligi = DateUtils.getWeekRangeString(monday),
            calisan_adi = fullName,
            user_id = userId
        )

        mainViewModel.addWeek(week) { weekId ->
            // Pre-fill records for the 7 days of the week
            val cal = monday.clone() as Calendar
            val records = mutableListOf<OvertimeRecord>()
            for (i in 0..6) {
                records.add(OvertimeRecord(
                    hafta_id = weekId.toInt(),
                    tarih = DateUtils.formatDateForDb(cal),
                    aciklama = "",
                    saat = "0",
                    is_resmi_tatil = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 1 else 0
                ))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            mainViewModel.addRecords(records)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
