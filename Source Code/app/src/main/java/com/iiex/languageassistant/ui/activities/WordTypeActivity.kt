package com.iiex.languageassistant.ui.activities

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.ActivityMultipleChoiceBinding
import com.iiex.languageassistant.databinding.ActivityWordTypeBinding
import com.iiex.languageassistant.ui.fragments.MultipleChoiceFragment
import com.iiex.languageassistant.ui.fragments.WordTypeFragment
import com.iiex.languageassistant.viewmodels.MultipleChoiceViewModel
import com.iiex.languageassistant.viewmodels.WordTypeViewModel

class WordTypeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordTypeBinding
    private lateinit var viewModel: WordTypeViewModel
    private var topicID: String? = null
    private var topicTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_word_type)
         viewModel = ViewModelProvider(this)[WordTypeViewModel::class.java]
        binding.viewModel = viewModel


        topicID = intent.extras?.getString("topicID")
        topicTitle = intent.extras?.getString("topicTitle")

        if (topicID == null || topicTitle == null) {
            Toast.makeText(this, "No topic ID provided", Toast.LENGTH_LONG).show()
            finish()
        }else{
            // set toolbar
            val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Điền từ"
            binding.toolbar.setNavigationOnClickListener{
                this.onBackPressed()
            }
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(binding.frameWordType.id,WordTypeFragment(topicID!!, topicTitle!!),WordTypeFragment::class.java.simpleName)
                    .commit()
            }
        }
        binding.menu.setOnClickListener {
            showMenuDialog()
        }
    }
    fun hideMennu() {
        binding.menu.visibility = View.GONE
    }
    private fun showMenuDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_learning_options)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val wlp = dialog.window?.attributes
        if (wlp != null) {
            wlp.gravity = Gravity.BOTTOM
            dialog.window!!.attributes = wlp
        }

        val textViewRandom = dialog.findViewById<TextView>(R.id.textViewRandom)
        val switchAudiocMode = dialog.findViewById<Switch>(R.id.switchAudioMode)
        val fragment = supportFragmentManager.findFragmentByTag(WordTypeFragment::class.java.simpleName)
        if (fragment is WordTypeFragment) {
            switchAudiocMode.isChecked = fragment.audioMode
        }

        textViewRandom.setOnClickListener {
            if (fragment is WordTypeFragment) {
                fragment.shuffleList()
            }
        }

        switchAudiocMode.setOnCheckedChangeListener { _, isChecked ->
            if (fragment is WordTypeFragment) {
                fragment.audioMode = isChecked
            }
        }

        dialog.show()
    }
}