package com.iiex.languageassistant.ui.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.TopicRankingAdapter
import com.iiex.languageassistant.data.model.Topic
import com.iiex.languageassistant.data.model.User
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.model.ref.WordStatus
import com.iiex.languageassistant.databinding.ActivityTopicDetailPersonalBinding
import com.iiex.languageassistant.databinding.ItemTableRowTopicBinding
import com.iiex.languageassistant.util.DateTimeUtil
import com.google.android.material.bottomsheet.BottomSheetDialog

import com.iiex.languageassistant.viewmodels.TopicDetailPersonalViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class TopicDetailPersonalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopicDetailPersonalBinding
    private lateinit var rowBinding: ItemTableRowTopicBinding
    private lateinit var createFileLauncher: ActivityResultLauncher<Intent>

    private lateinit var viewModel: TopicDetailPersonalViewModel
    private lateinit var tts: TextToSpeech
    private val rankingTopicAdapter by lazy { TopicRankingAdapter() }
    private var topicItem: DataItem.Topic? = null
    private var markedCounnter = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = DataBindingUtil.setContentView(this, R.layout.activity_topic_detail_personal)
        viewModel = ViewModelProvider(this)[TopicDetailPersonalViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this


        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts.setLanguage(Locale.US)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Lỗi TTS!", Toast.LENGTH_LONG).show()
                }
                tts.setSpeechRate(1.0f)
            }
        }

        val intent = intent
        if (intent.hasExtra("key")) {
            this.topicItem = intent.getSerializableExtra("key") as DataItem.Topic?
            topicItem?.authorID?.let { viewModel.setAuthor(it) }
            topicItem!!.topicID?.let { topicID ->
                viewModel.setTopic(topicID)

                FirebaseAuth.getInstance().uid?.let { userID ->
                    viewModel.setWord(
                        userID,
                        topicID, WordStatus.ALL
                    )
                    viewModel.setTopicItem(userID, topicID)
                }
            }
        }

        binding.seekBarWords.setPadding(10, 0, 0, 0)
        binding.seekBarWords.isEnabled = false

        viewModel.topicItem.observe(this){
            topicItem->
            if (topicItem!=null){
                var word =
                    topicItem.wordLearned.toString() + "/" + topicItem.wordCount.toString() + " words"
                binding.textViewWords.setText(word)
                binding.seekBarWords.progress =
                    ((topicItem.wordLearned.toDouble() / topicItem.wordCount.toDouble()) * 100).toInt()
            }
        }
        //set author
        viewModel.author.observe(this, Observer<User> { author ->
            Glide.with(this)
                .load(author.avatarUrl) // Đường dẫn URL của hình ảnh
                .into(binding.imageViewAvt)
        })
        //set topic
        viewModel.topic.observe(this, Observer<Topic> { topic ->
            var create = "Creative time :" + DateTimeUtil.getDateFromTimestamp(topic.createTime)
            binding.textViewCreatedAt.setText(create)
            viewModel.generateRankItems()
        })
        // set up data AutoCompleteViewOption
        val data = arrayOf("Tất cả", "Đã học", "Chưa học", "Đã thành thạo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, data)
        binding.autoCompleteTextViewOption.setAdapter(adapter)
        binding.autoCompleteTextViewOption.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = data[position]
            binding.tableLayout.removeAllViews()
            when (selectedItem) {
                "Tất cả" -> {
                    FirebaseAuth.getInstance().uid?.let { viewModel.setWord(it, WordStatus.ALL) }
                }
                "Đã học" -> {
                    FirebaseAuth.getInstance().uid?.let {
                        viewModel.setWord(
                            it,
                            WordStatus.LEARNED
                        )
                    }
                }
                "Chưa học" -> {
                    FirebaseAuth.getInstance().uid?.let {
                        viewModel.setWord(
                            it,
                            WordStatus.NOT_LEARNED
                        )
                    }
                }
                "Đã thành thạo" -> {
                    FirebaseAuth.getInstance().uid?.let {
                        viewModel.setWord(
                            it,
                            WordStatus.MASTERED
                        )
                    }
                }
            }
        }
        //set default value
        binding.autoCompleteTextViewOption.setText("Tất cả", false)

        // set up data table about Term and Dine
        val tableLayout = binding.tableLayout
        viewModel.word.observe(this, Observer<List<Word>> { words ->
            binding.tableLayout.removeAllViews() // Xóa tất cả các dòng hiện có trước khi thêm dòng mới
            var i = 0
            markedCounnter = 0
            for (rowData in words) {
                // Inflate the layout for each row
                rowBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(this),
                    R.layout.item_table_row_topic,
                    null,
                    false
                )
                rowBinding.rowData = rowData
                if (i % 2 != 0) {
                    val backgroundColor = ColorDrawable(resources.getColor(R.color.black_200_2))
                    rowBinding.root.background = backgroundColor
                } else {
                    val backgroundColor = ColorDrawable(resources.getColor(R.color.blue_400_10))
                    rowBinding.root.background = backgroundColor
                }
                rowBinding.buttonSpeaker.setOnClickListener(View.OnClickListener {
                    tts.speak(rowData.english, TextToSpeech.QUEUE_ADD, null, null)
                })
                rowBinding.buttonStar.setOnClickListener(View.OnClickListener {
                    FirebaseAuth.getInstance().uid?.let { userID ->
                        rowData.id?.let { wordID ->
                            viewModel.mark(userID, wordID, !rowData.isMarked)
                        }
                    }
                })
                if (rowData.isMarked) {
                    markedCounnter++
                    rowBinding.buttonStar.setImageResource(R.drawable.ic_star_marked)
                } else {
                    rowBinding.buttonStar.setImageResource(R.drawable.ic_star)
                }
                i += 1
                // Add the root view of the binding to the tableLayout
                binding.tableLayout.addView(rowBinding.root)
            }
        })


        // set up data for ranking recycleView

        viewModel.leaderBoard.observe(this){
            rankingTopicAdapter.updateList(it)
        }
        // Replace with your data
        binding.recyclerViewRanking.apply {
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL, false
            )
        }
        binding.recyclerViewRanking.adapter = rankingTopicAdapter

        // set up toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }
        binding.menu.setOnClickListener(
            View.OnClickListener {
                showMenuDialog()
            }
        )
        binding.lifecycleOwner = this

        //buttonMulchoice setup
        binding.buttonMulchoice.setOnClickListener(View.OnClickListener {
            multipleChoiceStart()
        })

        //buttonWordType
        binding.buttonWordType.setOnClickListener(View.OnClickListener {
            WordTypeStart()
        })
        //buttonFlashCard
        binding.buttonFlashCard.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)
            dialog.setContentView(R.layout.dialog_flashcard_setting)
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val wlp = dialog.window?.attributes
            if (wlp != null) {
                wlp.gravity = Gravity.CENTER_HORIZONTAL
                dialog.window!!.attributes = wlp
            }

            var buttonLearnAll = dialog.window?.findViewById<Button>(R.id.buttonLearnAll);
            var buttonLearnTicked = dialog.window?.findViewById<Button>(R.id.buttonLearnTicked);
            buttonLearnAll?.setOnClickListener {
                FlashCardStart("all")
                dialog.dismiss()
                // set data here
            }
            buttonLearnTicked?.setOnClickListener {
                if (markedCounnter>0){
                    FlashCardStart("marked")
                }else{
                    Toast.makeText(this, "Bạn chưa đánh dấu từ nào!", Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            dialog.show()
        }

        binding.layoutAuthor.setOnClickListener {
            val intent = Intent(this, AuthorProfileActivity::class.java)
            intent.putExtra("authorID", topicItem?.authorID)
            startActivity(intent)
        }

        //
        createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.exportCSVFile(uri,contentResolver, viewModel.word){
                        if (it){
                            Toast.makeText(this,"Đã lưu file!",Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        val progressDialog = createProgressDialog()
        viewModel.loadingIndicator.observe(this, Observer { isLoading ->
            if (isLoading == true) progressDialog.show() else progressDialog.dismiss()
        })

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
    fun multipleChoiceStart(){
        viewModel.topic.value?.id?.let { viewModel.incrementViewCount(it) }
        val intent = Intent(this, MultipleChoiceActivity::class.java)
        intent.putExtra("topicID", viewModel.topic.value?.id)
        intent.putExtra("topicTitle", viewModel.topic.value?.title)
        startActivity(intent)
    }
    fun WordTypeStart(){
        viewModel.topic.value?.id?.let { viewModel.incrementViewCount(it) }
        val intent = Intent(this, WordTypeActivity::class.java)
        intent.putExtra("topicID", viewModel.topic.value?.id)
        intent.putExtra("topicTitle", viewModel.topic.value?.title)
        startActivity(intent)
    }
    fun FlashCardStart(isMarked: String){
        viewModel.topic.value?.id?.let { viewModel.incrementViewCount(it) }
        val intent = Intent(this, FlashCardActivity::class.java)
        intent.putExtra("topicID", viewModel.topic.value?.id)
        intent.putExtra("isMarked", isMarked)
        intent.putExtra("topicTitle", viewModel.topic.value?.title)
        startActivity(intent)
    }
    private fun showMenuDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_topicpersonal_option)
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
        val exportCSVTextView = dialog.findViewById<TextView>(R.id.textViewExportCSV)
        val switchPublicMode = dialog.findViewById<Switch>(R.id.switchPublicMode)

        if (viewModel.author.value?.id != FirebaseAuth.getInstance().uid){
            editTextView.visibility = View.GONE
            deleteTextView.visibility = View.GONE
            switchPublicMode.isEnabled = false
        }
        editTextView.setOnClickListener {
            val intent = Intent(this,AddTopicActivity::class.java)
            intent.putExtra("topicID", viewModel.topic.value?.id)
            startActivityForResult(intent,102)
            dialog.dismiss()
        }

        exportCSVTextView.setOnClickListener {
            if (checkPermissions()) {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    val currentDateTime = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(
                        LocalDateTime.now())
                    putExtra(Intent.EXTRA_TITLE, "words_$currentDateTime.csv")
                }
                createFileLauncher.launch(intent)
            } else {
                requestPermissions()
            }
        }
        deleteTextView.setOnClickListener {
            // Xử lý khi người dùng nhấn vào "Xóa"
            dialog.dismiss()
            showDeleteConfirm()
        }

        addIntoFolderTextView.setOnClickListener {
            val intent = Intent(this, AddIntoFolderActivity::class.java)
            intent.putExtra("topic", topicItem)
            startActivity(intent)
        }

        if (viewModel.topic.value?.isPublic == true){
            switchPublicMode.isChecked = true
        }


        switchPublicMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setPublic(true)
            } else {
                viewModel.setPublic(false)
            }
        }

        dialog.show()
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
            dialog.dismiss()
            viewModel.deleteTopic(){
                if (it){
                    finish()
                }
            }
        }
        buttonBack.setOnClickListener {
            dialog.dismiss()
            showMenuDialog()
        }
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 102 && resultCode == Activity.RESULT_OK) {
            val resultData = data?.getIntExtra("newWordCount",0)
            if (resultData != 0) {
                var word =
                    topicItem?.wordLearned.toString() + "/" + resultData.toString() + " words"
                binding.textViewWords.setText(word)
            }
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