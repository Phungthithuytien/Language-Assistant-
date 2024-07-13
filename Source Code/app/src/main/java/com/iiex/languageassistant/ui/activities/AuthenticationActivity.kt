package com.iiex.languageassistant.ui.activities

import androidx.appcompat.app.AppCompatActivity

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController

import com.iiex.languageassistant.R
import com.iiex.languageassistant.viewmodels.AuthenticationViewModel

class AuthenticationActivity : AppCompatActivity(R.layout.activity_authentication) {
    private lateinit var viewModel: AuthenticationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AuthenticationViewModel::class.java)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
    }

}