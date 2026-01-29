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
import com.example.mesaitakip.R
import com.example.mesaitakip.databinding.FragmentLoginBinding
import com.example.mesaitakip.ui.viewmodel.AuthViewModel
import com.example.mesaitakip.ui.viewmodel.AuthViewModelFactory

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        AuthViewModelFactory((requireActivity().application as MesaiApp).userRepository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(username, password)
            } else {
                Toast.makeText(context, "Lütfen alanları doldurun", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
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
