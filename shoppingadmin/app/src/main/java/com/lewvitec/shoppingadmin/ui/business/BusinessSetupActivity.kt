package com.lewvitec.shoppingadmin.ui.business

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.lewvitec.shoppingadmin.databinding.ActivityBusinessSetupBinding
import com.lewvitec.shoppingadmin.ui.login.LoginActivity
import com.lewvitec.shoppingadmin.utils.TenantManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BusinessSetupActivity : AppCompatActivity() {

    @Inject
    lateinit var tenantManager: TenantManager

    private lateinit var binding: ActivityBusinessSetupBinding
    private val viewModel: BusinessSetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupTextWatchers() {
        binding.etBusinessName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etSubdomain.text.isNullOrEmpty()) {
                generateSubdomainFromBusinessName()
            }
        }
    }

    private fun generateSubdomainFromBusinessName() {
        val businessName = binding.etBusinessName.text.toString().trim()
        if (businessName.isNotEmpty()) {
            val subdomain = businessName
                .lowercase()
                .replace(" ", "-")
                .replace("[^a-z0-9-]".toRegex(), "")
            binding.etSubdomain.setText(subdomain)
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateStore.setOnClickListener {
            val businessName = binding.etBusinessName.text.toString().trim()
            val subdomain = binding.etSubdomain.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()

            if (businessName.isEmpty()) {
                showError("Please enter business name")
                return@setOnClickListener
            }

            if (subdomain.isEmpty()) {
                showError("Please enter subdomain")
                return@setOnClickListener
            }

            if (!subdomain.matches(Regex("^[a-z0-9-]+\$"))) {
                showError("Subdomain can only contain lowercase letters, numbers, and hyphens")
                return@setOnClickListener
            }

            if (password.length < 6) {
                showError("Password must be at least 6 characters long")
                return@setOnClickListener
            }

            if (password != confirm) {
                showError("Passwords do not match")
                return@setOnClickListener
            }

            viewModel.createStore(businessName, subdomain, password)
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility =
                if (loading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnCreateStore.isEnabled = !loading
        }

        viewModel.error.observe(this) { error ->
            error?.let { showError(it) }
        }

        viewModel.storeCreated.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Store created successfully!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActivity::class.java)
                val email = "${viewModel.createdSubdomain}@lewvitec.co.ke"
                intent.putExtra("auto_fill_email", email)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = android.view.View.VISIBLE
    }
}
