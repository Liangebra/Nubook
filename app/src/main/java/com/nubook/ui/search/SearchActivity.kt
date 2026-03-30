package com.nubook.ui.search

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.nubook.databinding.ActivitySearchBinding
import com.nubook.ui.ledger.TransactionAdapter

import com.nubook.ui.base.BaseActivity

/**
 * 搜索 Activity
 * 实时搜索备注、标签内容
 */
class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ledgerId = intent.getStringExtra("ledger_id")

        viewModel = ViewModelProvider(
            this,
            SearchViewModel.Factory(application, ledgerId)
        )[SearchViewModel::class.java]

        setupRecyclerView()
        setupSearchInput()
        observeData()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener { binding.etSearch.text.clear() }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onItemClick = { /* 搜索页点击 */ },
            onItemLongClick = { /* 搜索页长按 */ },
            onSelectionChanged = { /* 搜索页多选 */ }
        )
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter
    }

    private fun setupSearchInput() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            viewModel.updateQuery(query)
            binding.btnClear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun observeData() {
        viewModel.searchResults.observe(this) { results ->
            adapter.submitList(results)
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty() && results.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.layoutEmpty.visibility = View.GONE
            }
        }
    }
}
