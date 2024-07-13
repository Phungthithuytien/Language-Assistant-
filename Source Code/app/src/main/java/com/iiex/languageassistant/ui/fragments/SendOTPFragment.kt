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
import androidx.navigation.fragment.findNavController
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.FragmentLoginBinding
import com.iiex.languageassistant.databinding.FragmentSendOTPBinding
import com.iiex.languageassistant.viewmodels.AuthenticationViewModel

class SendOTPFragment : Fragment() {
    private val authViewModel: AuthenticationViewModel by activityViewModels()
    private lateinit  var binding: FragmentSendOTPBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding  = FragmentSendOTPBinding.inflate(inflater,container,false)
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            forgotPasswordFragment = this@SendOTPFragment
        }
        binding.buttonSendOTP.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            authViewModel.sendOTPCode(email){
                isSuccess,mess->
                if (isSuccess){
                    Toast.makeText(requireContext(),"Đã gửi mã xác minh vào ${email}",Toast.LENGTH_LONG)
                }else{
                    binding.textlayoutEmail.error = "${email} không tồn tại!"
                }
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

        val progressDialog = createProgressDialog()
        authViewModel.loadingIndicator.observe(this){
            if (it){
                progressDialog.show()
            }else{
                progressDialog.dismiss()
            }
        }

        return binding.root
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

    fun login(){
        findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
    }
}