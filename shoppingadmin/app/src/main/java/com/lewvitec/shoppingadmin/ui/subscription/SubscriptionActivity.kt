package com.lewvitec.shoppingadmin.ui.subscription

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.lewvitec.shoppingadmin.databinding.ActivitySubscriptionBinding
import com.lewvitec.shoppingadmin.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SubscriptionActivity : AppCompatActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private lateinit var binding: ActivitySubscriptionBinding
    private val viewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        loadData()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupObservers() {
        viewModel.plans.observe(this) { plans ->
            binding.swipeRefresh.isRefreshing = false
            // Update plans RecyclerView
        }

        viewModel.currentSubscription.observe(this) { subscription ->
            // Update current subscription UI
        }

        viewModel.loading.observe(this) { isLoading ->
            // Show/hide loading
        }
    }

    private fun loadData() {
        val sessionId = preferenceManager.getSessionId()
        viewModel.loadSubscriptionPlans(sessionId)
    }
}