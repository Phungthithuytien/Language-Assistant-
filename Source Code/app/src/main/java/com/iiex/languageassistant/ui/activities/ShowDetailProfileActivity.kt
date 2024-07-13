package com.iiex.languageassistant.ui.activities

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.ActivityFlashCardBinding
import com.iiex.languageassistant.databinding.ActivityShowDetailProfileBinding

class ShowDetailProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShowDetailProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_show_detail_profile)
        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        val customTypeface = ResourcesCompat.getFont(this, R.font.roboto_bold)  // Replace with your font resource ID
        val spannableString = SpannableString("")
        spannableString.setSpan(customTypeface?.let { TypefaceSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = spannableString
        val upArrow: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_back_toolbar)
        supportActionBar?.setHomeAsUpIndicator(upArrow)
        binding.toolbar.setNavigationOnClickListener{
            this.onBackPressed()
        }
    }
}