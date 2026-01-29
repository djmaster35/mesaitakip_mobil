package com.example.mesaitakip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.mesaitakip.MesaiApp
import com.example.mesaitakip.databinding.FragmentAdminBinding
import com.example.mesaitakip.ui.adapter.UserAdapter
import com.example.mesaitakip.ui.viewmodel.AdminViewModel
import com.example.mesaitakip.ui.viewmodel.AdminViewModelFactory

class AdminFragment : Fragment() {
    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    private val adminViewModel: AdminViewModel by viewModels {
        AdminViewModelFactory((requireActivity().application as MesaiApp).userRepository)
    }

    private lateinit var adapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UserAdapter(
            onBanToggle = { adminViewModel.toggleBan(it) },
            onAdminToggle = { adminViewModel.toggleAdmin(it) },
            onDelete = { adminViewModel.deleteUser(it) }
        )
        binding.rvUsers.adapter = adapter

        adminViewModel.allUsers.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        adminViewModel.loadAllUsers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
