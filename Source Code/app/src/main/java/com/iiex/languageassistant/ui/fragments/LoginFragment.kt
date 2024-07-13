package com.iiex.languageassistant.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.FragmentLoginBinding
import com.iiex.languageassistant.viewmodels.AuthenticationViewModel


class LoginFragment : Fragment(R.layout.fragment_login) {
    private val authViewModel: AuthenticationViewModel by activityViewModels()
    private lateinit  var binding: FragmentLoginBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding  = FragmentLoginBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //binding?.viewModel = this
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            authenticationViewModel = authViewModel
            loginFragment = this@LoginFragment
        }
        binding.editTextEmail.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                val enteredInput = binding.editTextEmail.text.toString()
                if (!isValidEmail(enteredInput)) {
                    binding.textlayoutEmail.error = "Email không hợp lệ!"
                }else {
                    binding.textlayoutEmail.error = null
                }
            }else{
                binding.textlayoutEmail.error = null
            }
        }

        binding.editTextPassword.setOnFocusChangeListener { view, hasFocus ->
            binding.textlayoutPassword.error = null
        }

        val progressDialog = createProgressDialog()
        authViewModel.loadingIndicator.observe(this){
            if (it){
                progressDialog.show()
            }else{
                progressDialog.dismiss()
            }
        }
    }
    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()){
            return false
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return email.matches(emailRegex.toRegex())
    }
    fun login(){
        authViewModel.login(){
            isSuccess,mess->
            if(isSuccess){
                Toast.makeText(this.context, "Đăng nhập thành công.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_mainActivity)
            }else{
                binding.textlayoutEmail.error = "Tài khoản hoặc mật khẩu không đúng!"
                binding.textlayoutPassword.error = "Tài khoản hoặc mật khẩu không đúng!"
            }
        }

    }
    private fun createProgressDialog(): AlertDialog {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_progress,null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return  dialog
    }

    fun register(){
        findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
    }

    fun forgotPassword(){
        findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
    }
}