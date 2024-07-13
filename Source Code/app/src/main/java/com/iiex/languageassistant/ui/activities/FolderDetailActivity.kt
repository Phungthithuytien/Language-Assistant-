package com.iiex.languageassistant.ui.activities

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.TopicAdapter
import com.iiex.languageassistant.data.model.Folder
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.model.ref.WordStatus
import com.iiex.languageassistant.databinding.ActivityFolderDetailBinding
import com.iiex.languageassistant.viewmodels.FolderViewModel
import com.iiex.languageassistant.viewmodels.TopicDetailPersonalViewModel

class FolderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderDetailBinding
    private lateinit var viewModel: FolderViewModel
    private var folderID: String? = null
    private val requestCode = 102
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_detail)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_folder_detail)
        viewModel = ViewModelProvider(this)[FolderViewModel::class.java]

        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }
        binding.toolbar.title = "Thư mục"
        binding.menu.setOnClickListener(
            View.OnClickListener {
                showMenuDialog()
            }
        )
        val topicAdapterList = TopicAdapter(this)
        val intent = intent
        if (intent.hasExtra("folderID")) {
            folderID = intent.getSerializableExtra("folderID") as String?
            folderID?.let {
                viewModel.initFolder(it)
                viewModel.dataList.observe(this){
                    topicAdapterList.updateList(it)
                }
            }
        }
        viewModel.folder.observe(this){
            binding.folder = it
        }

        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                viewModel.loadMoreTopicData()
            }
        })


        binding.recycleView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = topicAdapterList
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestCode && resultCode == Activity.RESULT_OK) {
            folderID?.let {
                viewModel.initFolder(it)
            }
        }
    }


    private fun showMenuDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_folder_option)
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

        val editTextView = dialog.findViewById<TextView>(R.id.textViewEdit)
        val deleteTextView = dialog.findViewById<TextView>(R.id.textViewDelete)
        val addIntoFolderTextView = dialog.findViewById<TextView>(R.id.textViewAddIntoFolder)

        editTextView.setOnClickListener {
            dialog.dismiss()
            showEditDidlog(viewModel.folder.value as DataItem.Folder?)
        }


        deleteTextView.setOnClickListener {
            dialog.dismiss()
            showDeleteConfirm()
        }

        addIntoFolderTextView.setOnClickListener {
            val intent = Intent(this, AddTopicsIntoFolderActivity::class.java)
            intent.putExtra("folderID", folderID)
            startActivityForResult(intent, requestCode)
        }

        dialog.show()
    }

    private fun showEditDidlog(folder: DataItem.Folder?){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_make_folder)
        dialog.show()

        val buttonSave = dialog.findViewById<AppCompatButton>(R.id.buttonMakeFolder)
        val textViewAddFolder = dialog.findViewById<TextView>(R.id.textViewAddFolder)
        val editTextFolderTitle = dialog.findViewById<TextView>(R.id.editTextTitle)
        textViewAddFolder.setText("Chỉnh Sửa Folder")
        if (folder != null) {
            editTextFolderTitle.setText(folder.title)
        }

        buttonSave.setOnClickListener {
            val title = editTextFolderTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                folderID?.let { folderID ->
                    viewModel.editTitle(folderID,title){
                        binding.textViewFolderTitle.setText(title)
                    }
                }
            }
            dialog.dismiss()
        }
    }


    private fun showDeleteConfirm(){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_delete_topic)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val wlp = dialog.window?.attributes
        if (wlp != null) {
            wlp.gravity = Gravity.CENTER
            dialog.window!!.attributes = wlp
        }


        val buttonConfirmDelete = dialog.findViewById<TextView>(R.id.buttonConfirmDelete)
        val buttonBack = dialog.findViewById<TextView>(R.id.buttonBack)

        buttonConfirmDelete.setOnClickListener {
            folderID?.let { it1 ->
                viewModel.deleteFolder(it1){
                    if (it){
                        dialog.dismiss()
                        finish()
                    }
                }
            }
        }
        buttonBack.setOnClickListener {
            showMenuDialog()
            dialog.dismiss()
        }
        dialog.show()

    }
}