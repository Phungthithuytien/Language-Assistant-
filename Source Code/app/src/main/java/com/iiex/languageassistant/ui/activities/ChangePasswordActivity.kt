package com.iiex.languageassistant.ui.activities

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.ActivityChangePasswordBinding
import com.iiex.languageassistant.viewmodels.AuthenticationViewModel

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding : ActivityChangePasswordBinding
    private lateinit var authViewModel: AuthenticationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_change_password)
        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.changepassword)
        binding.toolbar.setNavigationOnClickListener{
            this.onBackPressed()
        }

        authViewModel = ViewModelProvider(this).get(AuthenticationViewModel::class.java)
        validateInput()
        binding.buttonChangePassword.setOnClickListener {
            if (isFormValid()){
                val passwordOld = binding.editTextOldPassword.text.toString()
                val passwordNew = binding.editTextPassword.text.toString()
                authViewModel.changePassword(passwordOld,passwordNew){
                    isSuccess, mess->
                        if (isSuccess){
                            Toast.makeText(this,"Đã đổi mật khẩu thành công!",Toast.LENGTH_LONG).show()
                            finish()
                        }else{
                            binding.textlayoutOldPassword.error = mess
                        }

                }
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
    }

    fun validateInput(){
        binding.editTextOldPassword.setOnFocusChangeListener { view, hasFocus ->
            if(!hasFocus){
                val enteredInput = binding.editTextPassword.text.toString()
                if (enteredInput.length < 6 || enteredInput.isEmpty()) {
                    binding.textlayoutPassword.error = "Mật khẩu phải từ 6 ký tự trở lên!"
                }else{
                    binding.textlayoutPassword.error =null
                }
            }else{
                binding.textlayoutOldPassword.error = null
            }
        }

        binding.editTextPassword.setOnFocusChangeListener { view, hasFocus ->
            if(!hasFocus){
                val passwordOld = binding.editTextOldPassword.text.toString()
                if (passwordOld.isEmpty() || passwordOld.isBlank()) {
                    binding.textlayoutPassword.error = "Mật khẩu cũ không được bỏ trống!"
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
        val passwordOld = binding.editTextOldPassword.text.toString()
        if (passwordOld.isEmpty() || passwordOld.isBlank()) {
            binding.textlayoutPassword.error = "Mật khẩu cũ không được bỏ trống!"
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

    private fun createProgressDialog(): AlertDialog {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_progress,null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return  dialog
    }
}