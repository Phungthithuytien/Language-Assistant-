package com.iiex.languageassistant.ui.fragments

import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.TabLayoutPagerAdapter
import com.iiex.languageassistant.data.model.Folder
import com.iiex.languageassistant.databinding.FragmentLibraryBinding
import com.iiex.languageassistant.ui.activities.AddTopicActivity
import com.iiex.languageassistant.viewmodels.LibraryViewModel


class LibraryFragment : Fragment() {

    private lateinit var viewModel: LibraryViewModel

    private lateinit var binding: FragmentLibraryBinding
    companion object {
        private const val DEFAULT_TAB_INDEX = "default_tab_index"

        fun newInstance(defaultTabIndex: Int): LibraryFragment {
            val fragment = LibraryFragment()
            val args = Bundle()
            args.putInt(DEFAULT_TAB_INDEX, defaultTabIndex)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_library, container, false
        )
        viewModel = ViewModelProvider(this)[LibraryViewModel::class.java]
        // Set up TabLayout and ViewPager
        val viewPager = binding.viewPager
        val adapter = TabLayoutPagerAdapter(requireActivity())
        val topicFragment = TopicFragment()
        val folderFragment = FolderFragment()
        adapter.addFragment(topicFragment, "Topic")
        adapter.addFragment(folderFragment, "Folder")
        viewPager.adapter = adapter
        val tabTextColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_selected),
                intArrayOf() // Trạng thái mặc định
            ),
            intArrayOf(
                Color.WHITE, // Màu khi được chọn
                ContextCompat.getColor(requireContext(), R.color.blue_400) // Màu mặc định
            )
        )
        val tabLayout = binding.tabLayout
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val customTabView = LayoutInflater.from(context).inflate(R.layout.item_librarytablayout, null)
            val textViewTabItem = customTabView.findViewById<TextView>(R.id.textViewTabItem)
            textViewTabItem.text = adapter.getPageTitle(position)
            textViewTabItem.setTextColor(tabTextColor)
            tab.tag = textViewTabItem.text
            tab.customView = customTabView
        }.attach()
        // set tool bar
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val customTypeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_bold)  // Replace with your font resource ID
        val spannableString = SpannableString(requireActivity().getString(R.string.library))
        spannableString.setSpan(customTypeface?.let { TypefaceSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = spannableString
        val upArrow: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.ic_back_toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(upArrow)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        val progressDialog = createProgressDialog()
        viewModel.loadingIndicator.observe(this){
            if (it){
                progressDialog.show()
            }else{
                progressDialog.dismiss()
            }
        }

        val defaultTabIndex = arguments?.getInt(DEFAULT_TAB_INDEX, 0) ?: 0

        viewPager.setCurrentItem(defaultTabIndex, false)

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Check if the query is not null or empty

                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                when(binding.tabLayout.selectedTabPosition){
                    0 -> {
                        topicFragment.filter(newText)
                    }
                    1 -> {
                        folderFragment.filter(newText)

                    }
                }
                return false
            }
        })

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
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    // Handle menu item selections in onOptionsItemSelected method
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add -> {
                Log.d(TAG, "onOptionsItemSelected: " + binding.tabLayout.selectedTabPosition)
                when(binding.tabLayout.selectedTabPosition){
                    0 -> {
                        val intent = Intent(requireContext(),AddTopicActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        val dialog = Dialog(requireContext())
                        dialog.setContentView(R.layout.dialog_make_folder)
                        dialog.show()

                        val buttonSave = dialog.findViewById<AppCompatButton>(R.id.buttonMakeFolder)

                        buttonSave.setOnClickListener {
                            val editText = dialog.findViewById<EditText>(R.id.editTextTitle)
                            val title = editText.text.toString().trim()
                            if (title.isNotEmpty()) {
                                val folder = Folder(title = title)
                                viewModel.addFolder(folder,requireContext())
                            } else {
                                // Handle the empty input case
                            }
                            dialog.dismiss()
                        }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}