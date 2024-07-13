package com.iiex.languageassistant.ui.activities

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.FolderAdapter
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.ActivityAddIntoFolderBinding
import com.iiex.languageassistant.viewmodels.FolderViewModel

class AddIntoFolderActivity : AppCompatActivity() {

    private val folderAdapterList by lazy { FolderAdapter(this) }
    private lateinit var viewModel: FolderViewModel
    private lateinit var binding: ActivityAddIntoFolderBinding
    private var topic: DataItem.Topic? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_into_folder)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_into_folder)
        viewModel = ViewModelProvider(this)[FolderViewModel::class.java]

        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        val customTypeface = ResourcesCompat.getFont(this, R.font.roboto_bold)  // Replace with your font resource ID
        val spannableString = SpannableString("Thêm vào folder")
        spannableString.setSpan(customTypeface?.let { TypefaceSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = spannableString
        val upArrow: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_back_toolbar)
        supportActionBar?.setHomeAsUpIndicator(upArrow)
        binding.toolbar.setNavigationOnClickListener{
            this.onBackPressed()
        }
        val intent = intent
        if (intent.hasExtra("topic")) {
            topic = intent.getSerializableExtra("topic") as DataItem.Topic?
            topic?.topicID?.let {
                viewModel.setFolder(it)
                folderAdapterList.setViewModel(viewModel)
                folderAdapterList.setTopicID(it)
            }
        }

        viewModel.folders.observe(this){
            folderAdapterList.updateList(it)
        }

        binding.recycleView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = folderAdapterList
        }

        val progressDialog = createProgressDialog()
        viewModel.loadingIndicator.observe(this){
            if (it){
                progressDialog.show()
            }else{
                progressDialog.dismiss()
            }
        }
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