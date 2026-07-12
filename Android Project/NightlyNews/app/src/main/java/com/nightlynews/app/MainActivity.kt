package com.nightlynews.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.nightlynews.app.databinding.ActivityMainBinding
import androidx.browser.customtabs.CustomTabsIntent
import android.net.Uri
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: NewsViewModel by viewModels()
    private lateinit var adapter: NewsAdapter

    private var currentCategory = NewsRepository.categories.first()
    private var isStateTab = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupTabs()
        setupStateDropdown()
        observeViewModel()

        viewModel.loadCategory(currentCategory)
    }

    // ── RecyclerView + pull-to-refresh ────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = NewsAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(false)
        }

        binding.swipeRefresh.setColorSchemeColors(
            getColor(R.color.accent)
        )
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(
            getColor(R.color.bg_card)
        )
        binding.swipeRefresh.setOnRefreshListener {
            if (isStateTab) {
                val pos = binding.spinnerState.selectedItemPosition
                if (pos > 0) viewModel.loadState(NewsRepository.states[pos - 1])
                else binding.swipeRefresh.isRefreshing = false
            } else {
                viewModel.loadCategory(currentCategory)
            }
        }
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private fun setupTabs() {
        NewsRepository.categories.forEach { cat ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(cat))
        }
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🗺 State News"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                isStateTab = (tab.position == NewsRepository.categories.size)
                if (isStateTab) {
                    binding.stateContainer.visibility = View.VISIBLE
                    val pos = binding.spinnerState.selectedItemPosition
                    if (pos > 0) viewModel.loadState(NewsRepository.states[pos - 1])
                    else showMessage("👆 Pick a state or union territory above")
                } else {
                    binding.stateContainer.visibility = View.GONE
                    currentCategory = NewsRepository.categories[tab.position]
                    viewModel.loadCategory(currentCategory)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Re-tap the active tab = refresh
                if (isStateTab) {
                    val pos = binding.spinnerState.selectedItemPosition
                    if (pos > 0) viewModel.loadState(NewsRepository.states[pos - 1])
                } else {
                    viewModel.loadCategory(currentCategory)
                }
            }
        })
    }

    // ── State spinner ─────────────────────────────────────────────────────────

    private fun setupStateDropdown() {
        val options = listOf("Select State / Union Territory") + NewsRepository.states

        val spinnerAdapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, options
        ) {
            override fun getView(pos: Int, convertView: android.view.View?, parent: ViewGroup)
                = super.getView(pos, convertView, parent).also { v ->
                    (v as? android.widget.TextView)?.setTextColor(getColor(R.color.text_primary))
                }

            override fun getDropDownView(pos: Int, convertView: android.view.View?, parent: ViewGroup)
                = super.getDropDownView(pos, convertView, parent).also { v ->
                    val bg = if (pos == binding.spinnerState.selectedItemPosition)
                        getColor(R.color.accent) else getColor(R.color.bg_card)
                    v.setBackgroundColor(bg)
                    (v as? android.widget.TextView)?.setTextColor(getColor(R.color.text_primary))
                }
        }
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerState.adapter = spinnerAdapter

        binding.spinnerState.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos > 0 && isStateTab) {
                    viewModel.loadState(NewsRepository.states[pos - 1])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is UiState.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                    binding.tvMessage.visibility = View.GONE
                    binding.recyclerView.visibility = View.GONE
                    binding.tvUpdatedAt.text = ""
                }
                is UiState.Success -> {
                    binding.tvMessage.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(state.items)
                    binding.recyclerView.scrollToPosition(0)
                    binding.tvUpdatedAt.text = state.updatedAt
                }
                is UiState.Error -> {
                    showMessage(state.message)
                    binding.tvUpdatedAt.text = ""
                }
            }
        }
    }

    private fun showMessage(msg: String) {
        binding.tvMessage.text = msg
        binding.tvMessage.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        adapter.submitList(emptyList())
    }
}
