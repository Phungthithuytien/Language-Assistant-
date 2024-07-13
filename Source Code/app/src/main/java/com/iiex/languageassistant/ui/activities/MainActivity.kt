package com.iiex.languageassistant.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.ActivityMainBinding
import com.iiex.languageassistant.ui.fragments.*
import com.iiex.languageassistant.viewmodels.MainViewModel

class MainActivity : AppCompatActivity(R.layout.activity_main) {


    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var isUpdatingMenu = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    // Handle Home item selection
                    if (!isUpdatingMenu){
                        replaceFragment(HomePageFragment(), HomePageFragment::class.java.simpleName)
                    }
                    return@setOnItemSelectedListener true
                }
                R.id.menu_community -> {
                    if (!isUpdatingMenu){
                        replaceFragment(CommunityFragment(), CommunityFragment::class.java.simpleName)
                    }
                    return@setOnItemSelectedListener true
                }
                R.id.menu_library -> {
                    if(!isUpdatingMenu){
                        replaceFragment(LibraryFragment(), LibraryFragment::class.java.simpleName)
                    }
                    return@setOnItemSelectedListener true
                }
                R.id.menu_profile -> {
                    if(!isUpdatingMenu){
                        replaceFragment(PersonalFragment(), PersonalFragment::class.java.simpleName)
                    }
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
        replaceFragment(HomePageFragment(),HomePageFragment::class.java.simpleName)
    }
    private fun replaceFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view_tag, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }


    override fun onBackPressed() {
        var currentFragmentTag = supportFragmentManager.findFragmentById(R.id.fragment_container_view_tag)?.tag

        if (currentFragmentTag == HomePageFragment::class.java.simpleName){
            AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ -> finishAffinity() }
                .setNegativeButton("No", null)
                .show()

        }else{
            super.onBackPressed()
            currentFragmentTag = supportFragmentManager.findFragmentById(R.id.fragment_container_view_tag)?.tag
            updateBottomNavigationMenu(currentFragmentTag)
        }
    }

     fun updateBottomNavigationMenu(fragmentTag: String?) {
        val itemId = when (fragmentTag) {
            HomePageFragment::class.java.simpleName -> R.id.menu_home
            CommunityFragment::class.java.simpleName -> R.id.menu_community
            LibraryFragment::class.java.simpleName -> R.id.menu_library
            PersonalFragment::class.java.simpleName -> R.id.menu_profile
            else -> -1
        }

        if (itemId != -1) {
            isUpdatingMenu = true
            binding.bottomNavigationView.selectedItemId = itemId
            isUpdatingMenu = false
        }
    }
}