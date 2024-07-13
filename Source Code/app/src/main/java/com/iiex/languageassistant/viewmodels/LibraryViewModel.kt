package com.iiex.languageassistant.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.data.model.Folder
import com.iiex.languageassistant.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LibraryViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _loadingIndicator = MutableLiveData<Boolean>()
    val loadingIndicator: LiveData<Boolean>
        get() = _loadingIndicator

    fun addFolder(folder: Folder, context: Context) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            _loadingIndicator.value = true
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.addFolder(user.uid, folder) { isSuccess ->
                    val message = if (isSuccess) {
                        "Thêm thành công"
                    } else {
                        "Có lỗi xảy ra"
                    }
                    _loadingIndicator.postValue(false)
                    // Since Toasts need to be shown on the main thread
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
