package com.lewvitec.shoppingadmin.ui.footer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.lewvitec.shoppingadmin.databinding.ActivityFooterSettingsBinding
import com.lewvitec.shoppingadmin.utils.PreferenceManager
import com.lewvitec.shoppingadmin.utils.TenantManager
import com.lewvitec.shoppingadmin.models.TenantFooter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FooterSettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var tenantManager: TenantManager

    private lateinit var binding: ActivityFooterSettingsBinding
    private val viewModel: FooterSettingsViewModel by viewModels()

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFooterSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setupClickListeners()
        setupObservers()
        loadFooterSettings()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveFooterSettings()
        }
    }

    private fun setupObservers() {
        viewModel.footerData.observe(this) { footer ->
            footer?.let { populateForm(it) }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
        }

        viewModel.saveResult.observe(this) { result ->
            result?.let {
                if (it.isSuccess) {
                    Snackbar.make(binding.root, "Footer settings saved successfully", Snackbar.LENGTH_LONG)
                        .setAction("OK") { }
                        .show()
                } else {
                    val error = it.exceptionOrNull()?.message ?: "Failed to save footer settings"
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                        .setAction("RETRY") { saveFooterSettings() }
                        .show()
                }
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                if (!it.contains("No footer found")) { // Don't show error for first-time setup
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                }
                viewModel.clearError()
            }
        }
    }

    private fun loadFooterSettings() {
        val sessionId = preferenceManager.getSessionId()
        if (sessionId.isNotEmpty()) {
            viewModel.loadFooterSettings(sessionId)
        } else {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun populateForm(footer: TenantFooter) {
        binding.etBusinessName.setText(footer.businessName ?: "")
        binding.etAboutText.setText(footer.aboutText ?: "")
        binding.etAddress.setText(footer.address ?: "")
        binding.etPhone1.setText(footer.phone1 ?: "")
        binding.etPhone2.setText(footer.phone2 ?: "")
        binding.etEmail.setText(footer.email ?: "")
        binding.etMonFri.setText(footer.monFri ?: "08:00 To 19:00")
        binding.etSaturday.setText(footer.saturday ?: "09:00 To 19:00")
        binding.etSunday.setText(footer.sunday ?: "Closed")
        binding.etFacebook.setText(footer.facebook ?: "")
        binding.etTwitter.setText(footer.twitter ?: "")
        binding.etInstagram.setText(footer.instagram ?: "")
        binding.etLinkedin.setText(footer.linkedin ?: "")
        binding.etPinterest.setText(footer.pinterest ?: "")
    }

    private fun saveFooterSettings() {
        val businessName = binding.etBusinessName.text.toString().trim()
        val aboutText = binding.etAboutText.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val phone1 = binding.etPhone1.text.toString().trim()
        val phone2 = binding.etPhone2.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val monFri = binding.etMonFri.text.toString().trim()
        val saturday = binding.etSaturday.text.toString().trim()
        val sunday = binding.etSunday.text.toString().trim()
        val facebook = binding.etFacebook.text.toString().trim()
        val twitter = binding.etTwitter.text.toString().trim()
        val instagram = binding.etInstagram.text.toString().trim()
        val linkedin = binding.etLinkedin.text.toString().trim()
        val pinterest = binding.etPinterest.text.toString().trim()

        // Validate required fields
        if (businessName.isEmpty()) {
            binding.etBusinessName.error = "Business name is required"
            return
        }

        if (phone1.isEmpty()) {
            binding.etPhone1.error = "Primary phone is required"
            return
        }

        val sessionId = preferenceManager.getSessionId()
        if (sessionId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel.saveFooterSettings(
            sessionId = sessionId,
            businessName = businessName,
            aboutText = aboutText,
            address = address,
            phone1 = phone1,
            phone2 = phone2,
            email = email,
            monFri = monFri.ifEmpty { "08:00 To 19:00" },
            saturday = saturday.ifEmpty { "09:00 To 19:00" },
            sunday = sunday.ifEmpty { "Closed" },
            facebook = facebook,
            twitter = twitter,
            instagram = instagram,
            linkedin = linkedin,
            pinterest = pinterest
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.clearSaveResult()
    }
}