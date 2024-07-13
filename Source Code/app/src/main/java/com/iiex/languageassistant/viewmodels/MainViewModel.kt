package com.iiex.languageassistant.viewmodels

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.data.model.ref.DataItem

class MainViewModel : ViewModel() {

    private val _dataList = MutableLiveData<List<DataItem>>()
    val dataList: LiveData<List<DataItem>> get() = _dataList
}