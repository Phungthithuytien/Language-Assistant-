package com.iiex.languageassistant.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.iiex.languageassistant.R
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.iiex.languageassistant.databinding.FragmentRegisterBinding
import com.iiex.languageassistant.viewmodels.AuthenticationViewModel


class RegisterFragment : Fragment(R.layout.fragment_register) {
    private val authViewModel: AuthenticationViewModel by activityViewModels()
    private lateinit var binding: FragmentRegisterBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding  = FragmentRegisterBinding.inflate(inflater,container,false)
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //binding?.viewModel = this
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            authenticationViewModel = authViewModel
            registerFragment = this@RegisterFragment
        }
        val progressDialog = createProgressDialog()
        authViewModel.loadingIndicator.observe(this){
            if (it){
                progressDialog.show()
            }else{
                progressDialog.dismiss()
            }
        }

        validateInput()
    }

    fun registerUser(){
        if(isFormValid()){
            authViewModel.registerUser { isSucces, mess ->
                if(isSucces){
                    Toast.makeText(this.context, "Đăng ký thành công.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }else{
                    //Toast.makeText(this.context, "${mess}", Toast.LENGTH_SHORT).show()
                    if (mess != null) {
                        if (mess.contains("email")){
                            binding.textlayoutEmail.error = "Email đã tồn tại!"
                        }
                    }
                }
            }
        }
    }

    fun login(){
        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
    }
    fun validateInput(){
        binding.editTextName.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                val enteredInput = binding.editTextName.text.toString()
                if (enteredInput.isEmpty()){
                    binding.textlayoutName.error = "Họ tên không hợp lệ!"
                }else{
                    binding.textlayoutName.error = null
                }
            }else{
                binding.textlayoutName.error = null
            }
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
            if(!hasFocus){
                val enteredInput = binding.editTextPassword.text.toString()
                if (enteredInput.length < 6 || enteredInput.isEmpty()) {
                    binding.textlayoutPassword.error = "Mật khẩu phải từ 6 ký tự trở lên!"
                }else{
                    binding.textlayoutPassword.error =null
                }
            }else{
                binding.textlayoutPassword.error = null
            }
        }


        binding.editTextConfirmPassword.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                val enteredInput = binding.editTextConfirmPassword.text.toString()
                if (!enteredInput.equals(binding.editTextPassword.text.toString())) {
                    binding.textlayoutPasswordConfirm.error = "Mật khẩu không khớp!"
                }else{
                    binding.textlayoutPasswordConfirm.error =null
                }
            }else{
                binding.textlayoutPasswordConfirm.error = null
            }
        }
    }
    fun isFormValid(): Boolean {
        // Check Name
        val name = binding.editTextName.text.toString()
        if (name.isEmpty()) {
            binding.textlayoutName.error = "Họ tên không hợp lệ!"
            return false
        }

        if (binding.textlayoutEmail.error != null) {
            return false
        }

        val password = binding.editTextPassword.text.toString()
        if (password.length < 6) {
            binding.textlayoutPassword.error = "Mật khẩu phải từ 6 ký tự trở lên!"
            return false
        }

        // Check Password Confirmation
        val passwordConfirm = binding.editTextConfirmPassword.text.toString()
        if (passwordConfirm != password) {
            binding.textlayoutPasswordConfirm.error = "Mật khẩu không khớp!"
            return false
        }

        return true
    }


    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()){
            return false
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return email.matches(emailRegex.toRegex())
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
}