package com.iiex.languageassistant.ui.activities

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.PostAdapter
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.ActivityAuthorProfileBinding
import com.iiex.languageassistant.viewmodels.PersonalViewModel

class AuthorProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthorProfileBinding
    private lateinit var viewModel: PersonalViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_author_profile)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_author_profile)
        this.setSupportActionBar(binding.toolbar)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }

        viewModel = ViewModelProvider(this).get(PersonalViewModel::class.java)

        if (intent.hasExtra("authorID")) {
            val authorID = intent.getSerializableExtra("authorID") as String?
            if (authorID != null) {
                viewModel.setAuthor(authorID)
                viewModel.setAuthorTopics(authorID)
            }
        }
        viewModel.user.observe(this) {
            if (it.avatarUrl != null) {
                Glide.with(this)
                    .load(it.avatarUrl) // Đường dẫn URL của hình ảnh
                    .into(binding.imageViewAvt)
            }
            binding.user = it
        }
        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        val postAdapter = PostAdapter(this, ArrayList())
        postAdapter.setIsProfile(true)
        recyclerView.adapter = postAdapter
        binding.recyclerView.isNestedScrollingEnabled = false

        viewModel.topics.observe(this){posts->
            postAdapter.updateData(posts)
        }

        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                viewModel.loadMoreData()
            }
        })

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