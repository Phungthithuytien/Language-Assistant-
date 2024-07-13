package com.iiex.languageassistant.viewmodels

import android.content.ContentValues.TAG
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.sql.Date
import  com.iiex.languageassistant.data.model.User
import com.iiex.languageassistant.data.repository.UserRepository

class AuthenticationViewModel : ViewModel() {
    var email = MutableLiveData<String>()
    var password = MutableLiveData<String>()
    var passwordConfirm = MutableLiveData<String>()
    var name = MutableLiveData<String>()

    private  val userRepository = UserRepository()


    private val _loadingIndicator = MutableLiveData<Boolean>()
    val loadingIndicator: LiveData<Boolean>
        get() = _loadingIndicator



    fun login(onComplete: (Boolean,String?)->Unit) {
        _loadingIndicator.value = true
        val emailValue = email.value
        val passwordValue = password.value
        if (!emailValue.isNullOrEmpty() && !passwordValue.isNullOrEmpty()) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(emailValue, passwordValue)
                .addOnSuccessListener { task ->
                    onComplete(true,null)
                    _loadingIndicator.postValue(false)
                }
                .addOnFailureListener {
                    excep ->
                    onComplete(false,excep.message)
                    _loadingIndicator.postValue(false)
                }
        } else {
            onComplete(false,"Email hoặc mật khẩu không hợp lệ!")
            _loadingIndicator.postValue(false)
        }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        _loadingIndicator.postValue(false)
    }

    fun changePassword(odlPassword:String, newPassword: String, onComplete: (Boolean, String?) -> Unit){
        _loadingIndicator.value = true
        val user = FirebaseAuth.getInstance().currentUser
        if (user!= null){
            val email = user.email
            val credential = EmailAuthProvider.getCredential(email?:"", odlPassword)
            user.reauthenticate(credential)
                .addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { passwordChangeTask ->
                                if (passwordChangeTask.isSuccessful) {
                                    onComplete(true,"Đã đổi mật khẩu thành công!")
                                } else {
                                    onComplete(false, passwordChangeTask.exception?.message)
                                }
                                _loadingIndicator.postValue(false)
                            }
                    } else {
                        onComplete(false,"Mật khẩu cũ chưa đúng!")
                        _loadingIndicator.postValue(false)
                    }
                }
        }else{
            _loadingIndicator.postValue(false)
        }



    }

    fun updateName(userID:String, name: String){
        _loadingIndicator.value = true
        userRepository.updateName(userID,name){
            _loadingIndicator.postValue(false)
        }
    }

    fun updateDayOfBirth(userID:String, dateOfBirth: String){
        _loadingIndicator.value = true
        userRepository.updateDayOfBirth(userID,dateOfBirth){
            _loadingIndicator.postValue(false)
        }
    }

    fun sendOTPCode(email: String, onComplete: (Boolean, String?) -> Unit){
        _loadingIndicator.value = true
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task->
            if (task.isSuccessful){
                onComplete(true,null)
            }else{
                onComplete(false, task.exception?.message)
            }
            _loadingIndicator.postValue(false)
        }
    }

    // Register
    fun registerUser(onComplete: (Boolean, String?)->Unit) {
        _loadingIndicator.value = true
        var emailValue = email.value
        var passwordValue = password.value
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(emailValue!!, passwordValue!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful){
                    val userReturned = task.result.user
                    val user = User(
                        userReturned?.uid,
                        emailValue,
                        name.value
                    )
                    userRepository.addUser(user){ success ->
                        onComplete(true,null)
                    }
                }
                else{
                    onComplete(false,task.exception?.message)
                }
                _loadingIndicator.postValue(false)
            }
    }
}