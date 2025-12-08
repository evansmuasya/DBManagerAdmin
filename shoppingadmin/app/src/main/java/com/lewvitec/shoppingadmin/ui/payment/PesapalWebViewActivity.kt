package com.lewvitec.shoppingadmin.ui.payment

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.lewvitec.shoppingadmin.R

class PesapalWebViewActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pesapal_webview)

        val pesapalUrl = intent.getStringExtra("pesapal_url") ?: return

        val webView = findViewById<WebView>(R.id.webView)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Check if payment completed
                if (url?.contains("pesapal_subscription_callback") == true) {
                    finish()
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl(pesapalUrl)
    }
}