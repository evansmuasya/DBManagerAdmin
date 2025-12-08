package com.lewvitec.shoppingadmin.ui.subscription

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.lewvitec.shoppingadmin.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.lewvitec.shoppingadmin.databinding.ActivitySubscriptionBinding
import com.lewvitec.shoppingadmin.models.SubscriptionPlan
import com.lewvitec.shoppingadmin.models.TenantSubscription
import com.lewvitec.shoppingadmin.ui.payment.PaymentActivity
import com.lewvitec.shoppingadmin.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SubscriptionActivity : AppCompatActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private lateinit var binding: ActivitySubscriptionBinding
    private val viewModel: SubscriptionViewModel by viewModels()

    private lateinit var plansAdapter: SubscriptionPlansAdapter
    private var selectedPlan: SubscriptionPlan? = null

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

        // Setup RecyclerView for plans
        plansAdapter = SubscriptionPlansAdapter { plan ->
            selectedPlan = plan
            // Highlight selected plan
            plansAdapter.setSelectedPlan(plan)
        }

        binding.rvPlans.layoutManager = LinearLayoutManager(this)
        binding.rvPlans.adapter = plansAdapter

        binding.btnSubscribe.setOnClickListener {
            selectedPlan?.let { plan ->
                // Navigate to payment
                val intent = Intent(this, PaymentActivity::class.java).apply {
                    putExtra("plan_id", plan.id)
                    putExtra("plan_name", plan.name)
                    putExtra("amount", plan.price)
                }
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Please select a plan first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.plans.observe(this) { plans ->
            binding.swipeRefresh.isRefreshing = false
            plansAdapter.submitList(plans ?: emptyList())

            if (plans.isNullOrEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvPlans.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvPlans.visibility = View.VISIBLE
            }
        }

        viewModel.currentSubscription.observe(this) { subscription ->
            updateCurrentSubscriptionUI(subscription)
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading == true
            binding.btnSubscribe.isEnabled = isLoading != true
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateCurrentSubscriptionUI(subscription: TenantSubscription?) {
        if (subscription != null) {
            binding.cardCurrentSubscription.visibility = View.VISIBLE
            binding.tvCurrentPlanName.text = "Plan ID: ${subscription.planId}"
            binding.tvSubscriptionStatus.text = "Status: ${subscription.status}"

            subscription.currentPeriodEnd?.let {
                binding.tvSubscriptionExpiry.text = "Renews: $it"
            }

            // Disable subscribe button if already subscribed
            binding.btnSubscribe.isEnabled = false
            binding.btnSubscribe.text = "Current Plan Active"
        } else {
            binding.cardCurrentSubscription.visibility = View.GONE
            binding.btnSubscribe.isEnabled = true
            binding.btnSubscribe.text = "Subscribe Now"
        }
    }

    private fun loadData() {
        val sessionId = preferenceManager.getSessionId()
        if (sessionId.isNotEmpty()) {
            viewModel.loadSubscriptionPlans(sessionId)
        } else {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

// Adapter for Subscription Plans
class SubscriptionPlansAdapter(
    private val onPlanSelected: (SubscriptionPlan) -> Unit
) : RecyclerView.Adapter<SubscriptionPlansAdapter.PlanViewHolder>() {

    private val plans = mutableListOf<SubscriptionPlan>()
    private var selectedPlanId: Int? = null

    fun submitList(newPlans: List<SubscriptionPlan>) {
        plans.clear()
        plans.addAll(newPlans)
        notifyDataSetChanged()
    }

    fun setSelectedPlan(plan: SubscriptionPlan) {
        selectedPlanId = plan.id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_subscription_plan, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = plans[position]
        holder.bind(plan, plan.id == selectedPlanId)

        holder.itemView.setOnClickListener {
            onPlanSelected(plan)
        }
    }

    override fun getItemCount() = plans.size

    class PlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardPlan: MaterialCardView = itemView.findViewById(R.id.cardPlan)
        private val tvPlanName: TextView = itemView.findViewById(R.id.tvPlanName)
        private val tvPlanPrice: TextView = itemView.findViewById(R.id.tvPlanPrice)
        private val tvPlanDescription: TextView = itemView.findViewById(R.id.tvPlanDescription)
        private val tvFeatures: TextView = itemView.findViewById(R.id.tvFeatures)
        private val btnSelect: MaterialButton = itemView.findViewById(R.id.btnSelect)

        fun bind(plan: SubscriptionPlan, isSelected: Boolean) {
            tvPlanName.text = plan.name
            tvPlanPrice.text = "KES ${plan.price}"
            tvPlanDescription.text = plan.description

            // Display features
            val featuresText = plan.features?.joinToString("\n• ", "• ") ?: ""
            tvFeatures.text = featuresText

            // Highlight selected plan
            if (isSelected) {
                cardPlan.strokeWidth = 4
                btnSelect.text = "Selected"
                btnSelect.isEnabled = false
            } else {
                cardPlan.strokeWidth = 0
                btnSelect.text = "Select"
                btnSelect.isEnabled = true
            }

            btnSelect.setOnClickListener {
                // Handle select button click
                // The parent adapter will handle this through the item click
            }
        }
    }
}