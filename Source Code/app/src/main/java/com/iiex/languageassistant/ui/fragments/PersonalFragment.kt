package com.iiex.languageassistant.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.PostAdapter
import com.iiex.languageassistant.databinding.FragmentPersonalBinding
import com.iiex.languageassistant.ui.activities.SettingActivity
import com.iiex.languageassistant.viewmodels.PersonalViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class PersonalFragment : Fragment() {
    private lateinit var binding: FragmentPersonalBinding
    private lateinit var viewModel: PersonalViewModel
    val CAMERA_REQUEST_CODE = 102
    val GALLERY_REQUEST_CODE = 103
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_personal, container, false
        )
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val customTypeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_bold)  // Replace with your font resource ID
        val spannableString = SpannableString(requireActivity().getString(R.string.personal))
        spannableString.setSpan(customTypeface?.let { TypefaceSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = spannableString
        val upArrow: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.ic_back_toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(upArrow)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.imageSetting.setOnClickListener {
            if (viewModel.user.value != null){
                val intent = Intent(activity, SettingActivity::class.java)
                intent.putExtra("user",viewModel.user.value)
                startActivity(intent)
            }
        }

        viewModel = ViewModelProvider(this).get(PersonalViewModel::class.java)
        FirebaseAuth.getInstance().uid?.let {
            viewModel.setAuthor(it)
            viewModel.setTopics(it)
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
        recyclerView.layoutManager = LinearLayoutManager(activity)

        val postAdapter = PostAdapter(requireContext(), ArrayList())
        recyclerView.adapter = postAdapter
        viewModel.topics.observe(this){posts->
            postAdapter.updateData(posts)
        }

        binding.imageCamera.setOnClickListener(View.OnClickListener {
            showImagePickerOptions()
        })

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
        return binding.root
    }

    private fun createProgressDialog(): AlertDialog {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_progress,null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return  dialog
    }
    fun showImagePickerOptions() {
        // You can use an AlertDialog to let the user choose between camera and gallery
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(requireContext())
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> takePhotoFromCamera()
                1 -> choosePhotoFromGallery()
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    fun takePhotoFromCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
    }

    fun choosePhotoFromGallery() {
        // Intent to pick photo from gallery
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val photo = data?.extras?.get("data") as Bitmap
                    val photoUri = bitmapToUri(requireContext(), photo)
                    photoUri?.let {
                        viewModel.upLoadAvatar(it)
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    val selectedImageUri = data?.data
                    if (selectedImageUri != null) {
                        viewModel.upLoadAvatar(selectedImageUri)
                    }
                }
            }
        }
    }

    fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
        val filename = "temp_image"
        val file = File(context.cacheDir, filename)
        file.createNewFile()

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos)
        val bitmapData = bos.toByteArray()

        try {
            FileOutputStream(file).apply {
                write(bitmapData)
                flush()
                close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        // Get the Uri from FileProvider
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

}