package com.iiex.languageassistant.ui.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.WordItemAdapter
import com.iiex.languageassistant.data.model.Topic
import com.iiex.languageassistant.databinding.ActivityAddTopicBinding
import com.iiex.languageassistant.viewmodels.WordItemViewModel
import java.io.Serializable

class AddTopicActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTopicBinding
    private val viewModel: WordItemViewModel by viewModels()
    private val PICK_CSV_FILE = 101
    private var isEdit = false
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_topic)
        viewModel.setBinding(binding)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        val wordItemAdapter = WordItemAdapter(viewModel)

        binding.recyclerViewWord.apply {
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL, false
            )
            adapter = wordItemAdapter
        }
        binding.recyclerViewWord.isNestedScrollingEnabled = false


        if (intent.hasExtra("topicID")) {
            val topic = intent.getStringExtra("topicID")
            isEdit = true
            topic?.let {
                viewModel.setTopic(it)
                viewModel.setWord(it)
            }
        }
        viewModel.topic.observe(this){
            binding.topic = viewModel.topic.value
        }


        viewModel.itemListLiveData.observe(this, Observer { itemList ->
            wordItemAdapter.updateList(itemList)
        })
        val progressDialog = createProgressDialog()
        viewModel.loadingIndicator.observe(this, Observer { isLoading ->
            if (isLoading == true) progressDialog.show() else progressDialog.dismiss()
        })

        binding.buttonSave.setOnClickListener {
            val topic = viewModel.topic.value
            val filteredList = viewModel.itemList.filter { word ->
                word.english.isNullOrBlank() || word.vietnamese.isNullOrBlank()
            }

            if (topic == null || topic.title.isNullOrEmpty() || viewModel.itemListLiveData.value?.size!! < 4) {
                Toast.makeText(
                    applicationContext,
                    "Vui lòng nhập tiêu đề và ít nhất 4 từ!",
                    Toast.LENGTH_LONG
                ).show()
            } else if (filteredList.size > 0) {
                Toast.makeText(applicationContext, "Vui lòng không bỏ trống!", Toast.LENGTH_LONG)
                    .show()
            } else {
                if (isEdit){
                    viewModel.updateTopic {
                        Toast.makeText(this, "Lưu thành công!", Toast.LENGTH_LONG).show()
                        val returnIntent = Intent()
                        returnIntent.putExtra("newWordCount", viewModel.itemList.size)
                        setResult(Activity.RESULT_OK, returnIntent)
                        finish()
                    }
                }else{
                    viewModel.createTopic {
                        Toast.makeText(this, "Lưu thành công!", Toast.LENGTH_LONG).show()
                        viewModel.handleTopicWithUser(){
                            topicRef ->
                            if (topicRef != null){
                                val intent = Intent(
                                    this,
                                    TopicDetailPersonalActivity::class.java
                                )
                                intent.putExtra("key", topicRef as Serializable)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }
            }
        }
        viewModel.addItem()
        wordItemAdapter.onHeightAvailable={
                itemHeight-> viewModel.itemHeight = itemHeight
            binding.recyclerViewWord.layoutParams.height = itemHeight*viewModel.itemList.size
        }

        for (i in 1..3){
            viewModel.addItem()
        }

        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        val customTypeface = ResourcesCompat.getFont(this, R.font.roboto_bold)  // Replace with your font resource ID
        val spannableString = SpannableString("Thêm Topic")
        spannableString.setSpan(customTypeface?.let { TypefaceSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = spannableString
        val upArrow: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_back_toolbar)
        supportActionBar?.setHomeAsUpIndicator(upArrow)
        binding.toolbar.setNavigationOnClickListener{
            this.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.just_opiton_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.option -> {
                val dialog = Dialog(this)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setCancelable(true)
                dialog.setCanceledOnTouchOutside(true)
                dialog.setContentView(R.layout.dialog_add_by_csv)
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
                val textViewAddByCSV =
                    dialog.findViewById<TextView>(R.id.textViewAddByCSV)
                textViewAddByCSV?.setOnClickListener {
                    if(checkPermissions()){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (Environment.isExternalStorageManager()) {
                                val intent = Intent().apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, true)
                                    action = Intent.ACTION_GET_CONTENT
                                }
                                startActivityForResult(
                                    Intent.createChooser(intent, "Select CSV File "),
                                    PICK_CSV_FILE
                                )
                            } else {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                val uri = Uri.fromParts("package", packageName, null)
                                startActivity(intent)
                            }
                        } else {
                            val intent = Intent().apply {
                                type = "*/*"
                                putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, true)
                                action = Intent.ACTION_GET_CONTENT
                            }
                            startActivityForResult(
                                Intent.createChooser(intent, "Select CSV File "),
                                PICK_CSV_FILE
                            )
                        }
                    }else{
                        requestPermissions()
                    }
                    dialog.dismiss()
                }
                dialog.show()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CSV_FILE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val path = data.data!!
            viewModel.readCSVFile(path, contentResolver)
        }
    }

    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above, check for MANAGE_EXTERNAL_STORAGE permission
            return Environment.isExternalStorageManager()
        } else {
            // For Android 10 and below, check for read and write permissions
            val readPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val writePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        } else {
            val REQUEST_PERMISSION_CODE = 200
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSION_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
            }
        }
    }
}
