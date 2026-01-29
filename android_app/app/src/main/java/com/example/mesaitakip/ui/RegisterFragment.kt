package com.example.mesaitakip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mesaitakip.MesaiApp
import com.example.mesaitakip.databinding.FragmentRegisterBinding
import com.example.mesaitakip.ui.viewmodel.AuthViewModel
import com.example.mesaitakip.ui.viewmodel.AuthViewModelFactory

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory((requireActivity().application as MesaiApp).userRepository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString()
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()

            if (fullName.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.register(username, password, fullName)
            } else {
                Toast.makeText(context, "Lütfen alanları doldurun", Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Kayıt başarılı, giriş yapabilirsiniz.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }.onFailure {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
