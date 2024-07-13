package com.iiex.languageassistant.ui.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.data.model.User
import com.iiex.languageassistant.databinding.ActivitySettingBinding
import com.iiex.languageassistant.viewmodels.AuthenticationViewModel
import com.iiex.languageassistant.viewmodels.MainViewModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class SettingActivity : AppCompatActivity() {


    private lateinit var binding: ActivitySettingBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var authenticationViewModel: AuthenticationViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        authenticationViewModel = ViewModelProvider(this)[AuthenticationViewModel::class.java]
        val user = intent.getSerializableExtra("user") as? User
        if (user != null) {
            binding.user = user
        }

        // setup tool bar
        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        val customTypeface = ResourcesCompat.getFont(this, R.font.roboto_bold)  // Replace with your font resource ID
        val spannableString = SpannableString(this.getString(R.string.personal))
        spannableString.setSpan(customTypeface?.let { TypefaceSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = spannableString
        val upArrow: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_back_toolbar)
        supportActionBar?.setHomeAsUpIndicator(upArrow)
        binding.toolbar.setNavigationOnClickListener{
            this.onBackPressed()
        }
        binding.buttonResetPassword.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }
        val dialog = createProgressDialog()
        binding.buttonLogout.setOnClickListener(View.OnClickListener {
            authenticationViewModel.logout()
            dialog.show()
            authenticationViewModel.loadingIndicator.observe(this, Observer { success ->
                Toast.makeText(this, "Đăng xuất thành công.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AuthenticationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                dialog.dismiss()
            })
        })


        //editflag
        var editNameFlag = false
        var editBirthFlag = false

        binding.buttonEditName.setOnClickListener {
            if (!editNameFlag) {
                enableEditMode(binding.editTextName, binding.buttonEditName)
                editNameFlag = true
            } else {
                val name = binding.editTextName.text.toString()
                if (validateName(name)) {
                    FirebaseAuth.getInstance().uid?.let { userID ->
                        authenticationViewModel.updateName(userID, name)
                    }
                    disableEditMode(binding.editTextName, binding.buttonEditName)
                    editNameFlag = false
                } else {
                    Toast.makeText(this, "Vui lòng nhập tên hợp lệ!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.buttonEditBirth.setOnClickListener {
            if (!editBirthFlag) {
                showDatePickerDialog()
                enableEditMode(binding.editTextBirth, binding.buttonEditBirth)
                editBirthFlag = true
            } else {
                val birthDate = binding.editTextBirth.text.toString()
                if (validateDateOfBirth(birthDate)) {
                    FirebaseAuth.getInstance().uid?.let { userID ->
                        authenticationViewModel.updateDayOfBirth(userID, birthDate)
                    }
                    disableEditMode(binding.editTextBirth, binding.buttonEditBirth)
                    editBirthFlag = false
                } else {
                    Toast.makeText(this, "Vui lòng nhập ngày hợp lệ!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        authenticationViewModel.loadingIndicator.observe(this){isLoading->
            if (isLoading){
                dialog.show()
            }else{
                dialog.dismiss()
            }
        }
    }

    private fun enableEditMode(editText: EditText?, buttonEdit: AppCompatImageButton) {

        if (editText != null) {
            editText.isEnabled = true
            editText.requestFocus()
        }
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        buttonEdit.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.ic_save_blue))
    }

    private fun disableEditMode(editText: EditText, buttonEdit: AppCompatImageButton) {
        editText.isEnabled = false
        buttonEdit.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pen))
    }

    private fun showDatePickerDialog() {
        val calendar: Calendar = Calendar.getInstance()
        val year: Int = calendar.get(Calendar.YEAR)
        val month: Int = calendar.get(Calendar.MONTH)
        val day: Int = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(this, { view, year1, month1, dayOfMonth ->
            val selectedDate: String = dayOfMonth.toString() + "/" + (month1 + 1) + "/" + year1
            binding.editTextBirth.setText(selectedDate)
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun createProgressDialog(): AlertDialog {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    private fun validateName(name: String): Boolean {
        return name.isNotBlank()
    }

    private fun validateDateOfBirth(date: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.isLenient = false
            val parsedDate = dateFormat.parse(date)
            parsedDate != null && !parsedDate.after(Date())
        } catch (e: ParseException) {
            false
        }
    }
}