package com.lewvitec.shoppingadmin.ui.products.lowstock

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.lewvitec.shoppingadmin.databinding.ActivityProductsBinding
import com.lewvitec.shoppingadmin.ui.products.ProductsAdapter
import com.lewvitec.shoppingadmin.ui.products.EditProductActivity
import com.lewvitec.shoppingadmin.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LowStockProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductsBinding
    private val viewModel: LowStockProductsViewModel by viewModels()
    private lateinit var pref: PreferenceManager
    private lateinit var adapter: ProductsAdapter

    @Inject
    lateinit var tenantManager: com.lewvitec.shoppingadmin.utils.TenantManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pref = PreferenceManager(this)

        setupToolbar()
        setupRecycler()
        setupObservers()

        loadLowStockProducts()

        binding.swipeRefresh.setOnRefreshListener { loadLowStockProducts() }

        // Hide FAB for low stock activity since we're only viewing
        binding.fabAdd.visibility = android.view.View.GONE
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Low Stock Products"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecycler() {
        val tenantId = tenantManager.getCurrentTenantId()
        adapter = ProductsAdapter(
            onEdit = { product ->
                val intent = Intent(this, EditProductActivity::class.java)
                intent.putExtra("product_id", product.id)
                startActivity(intent)
            },
            onDelete = { product ->
                confirmDelete(product.id, product.productName)
            },
            tenantId = tenantId // Add tenantId parameter
        )

        binding.recyclerViewProducts.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewProducts.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.products.observe(this) { list ->
            binding.swipeRefresh.isRefreshing = false
            adapter.submitList(list)

            // Update empty text for low stock context
            if (list.isEmpty()) {
                binding.tvEmpty.text = "No low stock products"
                binding.tvEmpty.visibility = android.view.View.VISIBLE
            } else {
                binding.tvEmpty.visibility = android.view.View.GONE
            }
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.error.observe(this) { err ->
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
        }

        viewModel.deleteSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLowStockProducts() {
        viewModel.loadLowStockProducts(pref.getSessionId())
    }

    private fun confirmDelete(productId: Int, productName: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete \"$productName\"? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteProduct(pref.getSessionId(), productId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadLowStockProducts()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            com.lewvitec.shoppingadmin.R.id.action_refresh -> loadLowStockProducts()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.lewvitec.shoppingadmin.R.menu.menu_products, menu)
        return true
    }
}