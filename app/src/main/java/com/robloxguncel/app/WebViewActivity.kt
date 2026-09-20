package com.robloxguncel.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * APKMirror release sayfasini uygulama icinde acar. Indirme linkine
 * basildiginda DownloadManager APK'yi indirir, indirme bitince kurulum
 * ekranini acar.
 */
class WebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_VERSION = "version"
    }

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private var versionName = "roblox"
    private var fallbackUsed = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id != -1L) InstallHelper.tryInstall(context, id)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        versionName = intent.getStringExtra(EXTRA_VERSION) ?: "roblox"
        val url = intent.getStringExtra(EXTRA_URL)
            ?: "https://www.apkmirror.com/apk/roblox-corporation/roblox/"

        progress = findViewById(R.id.webProgress)
        webView = findViewById(R.id.webView)
        setupWebView()
        webView.loadUrl(url)

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) webView.goBack() else finish()
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, filter)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
        }
        CookieManager.getInstance().setAcceptCookie(true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                progress.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                // Surum linki degistiyse (URL 404 vb.) Roblox kategori sayfasina dus.
                if (request.isForMainFrame && !fallbackUsed) {
                    fallbackUsed = true
                    view.loadUrl("https://www.apkmirror.com/apk/roblox-corporation/roblox/")
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress >= 90) progress.visibility = View.GONE
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val looksLikeApk =
                url.endsWith(".apk", true) ||
                    url.contains("download.php") ||
                    url.contains("apkmirror.com/wp-content") ||
                    url.contains("downloadr2.apkmirror.com") ||
                    contentDisposition.contains("attachment", true) ||
                    mimeType == "application/vnd.android.package-archive"
            if (looksLikeApk) {
                startDownload(url, userAgent, contentDisposition, mimeType)
            } else if (url.startsWith("http")) {
                webView.loadUrl(url)
            }
        }
    }

    private fun startDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String
    ) {
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                .let { if (it.endsWith(".bin")) "roblox-$versionName.apk" else it }

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                addRequestHeader("User-Agent", userAgent)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
                CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, R.string.download_started, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.download_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(downloadReceiver)
        webView.destroy()
        super.onDestroy()
    }
}
