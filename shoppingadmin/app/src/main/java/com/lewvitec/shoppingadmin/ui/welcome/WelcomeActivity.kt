package com.lewvitec.shoppingadmin.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lewvitec.shoppingadmin.databinding.ActivityWelcomeBinding
import com.lewvitec.shoppingadmin.ui.business.BusinessSetupActivity
import com.lewvitec.shoppingadmin.ui.login.LoginActivity
import com.lewvitec.shoppingadmin.utils.TenantManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeActivity : AppCompatActivity() {

    @Inject
    lateinit var tenantManager: TenantManager

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        checkExistingTenant()
    }

    private fun checkExistingTenant() {
        // If tenant is already setup, go directly to login
        if (tenantManager.isTenantSetup()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateStore.setOnClickListener {
            startActivity(Intent(this, BusinessSetupActivity::class.java))
        }

        binding.btnLoginExisting.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}