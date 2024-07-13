package com.iiex.languageassistant.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R

class SflagScreenActivity : AppCompatActivity(R.layout.activity_sflag_screen) {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(
            Runnable {
                nextActivity()
            },
            2000
        )
    }

    private fun nextActivity() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            val intent = Intent(this, AuthenticationActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}