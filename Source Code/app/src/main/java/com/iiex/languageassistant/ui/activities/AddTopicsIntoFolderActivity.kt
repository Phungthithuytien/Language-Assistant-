package com.iiex.languageassistant.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.TopicAdapter
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.ActivityAddIntoFolderBinding
import com.iiex.languageassistant.databinding.ActivityAddTopicsIntoFolderBinding
import com.iiex.languageassistant.viewmodels.FolderViewModel

class AddTopicsIntoFolderActivity : AppCompatActivity() {
    private lateinit var viewModel: FolderViewModel
    private lateinit var binding: ActivityAddTopicsIntoFolderBinding
    private var folderID: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_topics_into_folder)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_topics_into_folder)
        viewModel = ViewModelProvider(this)[FolderViewModel::class.java]

        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
            this.onBackPressed()
        }

        val topicAdapterList = TopicAdapter(this)
        topicAdapterList.setViewModel(viewModel)
        val intent = intent
        if (intent.hasExtra("folderID")) {
            folderID = intent.getSerializableExtra("folderID") as String?
            folderID?.let {
                viewModel.loadTopic(it)
                viewModel.dataList.observe(this){
                    topicAdapterList.updateList(it)
                    topicAdapterList.setFolderID(folderID!!)
                }
            }
        }
        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                viewModel.loadMoreTopic()
            }
        })

        binding.recycleView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = topicAdapterList
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