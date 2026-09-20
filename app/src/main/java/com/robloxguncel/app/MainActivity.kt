package com.robloxguncel.app

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var installedValue: TextView
    private lateinit var latestValue: TextView
    private lateinit var sourceValue: TextView
    private lateinit var statusBadge: TextView
    private lateinit var progress: ProgressBar
    private lateinit var downloadBtn: Button
    private lateinit var browserBtn: Button
    private lateinit var refreshBtn: Button
    private lateinit var errorText: TextView

    private var latest: VersionFetcher.Latest? = null
    private var installedVersion: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        installedValue = findViewById(R.id.installedValue)
        latestValue = findViewById(R.id.latestValue)
        sourceValue = findViewById(R.id.sourceValue)
        statusBadge = findViewById(R.id.statusBadge)
        progress = findViewById(R.id.progress)
        downloadBtn = findViewById(R.id.downloadBtn)
        browserBtn = findViewById(R.id.browserBtn)
        refreshBtn = findViewById(R.id.refreshBtn)
        errorText = findViewById(R.id.errorText)

        onBackPressedDispatcher.addCallback(this) { finish() }

        installedVersion = try {
            packageManager.getPackageInfo("com.roblox.client", 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
        installedValue.text = installedVersion ?: getString(R.string.not_installed_value)

        downloadBtn.setOnClickListener { openDownload(inBrowser = false) }
        browserBtn.setOnClickListener { openDownload(inBrowser = true) }
        refreshBtn.setOnClickListener { refresh() }
        findViewById<TextView>(R.id.fallbackLink).setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.apkmirror.com/apk/roblox-corporation/roblox/")
                )
            )
        }

        refresh()
    }

    private fun refresh() {
        progress.visibility = View.VISIBLE
        statusBadge.visibility = View.GONE
        errorText.visibility = View.GONE
        downloadBtn.isEnabled = false
        browserBtn.isEnabled = false
        thread {
            val result = try {
                VersionFetcher.fetchLatest()
            } catch (e: Exception) {
                null
            }
            runOnUiThread { onFetched(result) }
        }
    }

    private fun onFetched(result: VersionFetcher.Latest?) {
        progress.visibility = View.GONE
        latest = result

        if (result == null) {
            showBadge(R.string.status_error, R.color.badge_error)
            errorText.visibility = View.VISIBLE
            return
        }

        latestValue.text = result.version
        sourceValue.text = getString(R.string.source_label, result.source)
        browserBtn.isEnabled = true
        downloadBtn.isEnabled = true

        val inst = installedVersion
        when {
            inst == null -> showBadge(R.string.not_installed, R.color.badge_neutral)
            VersionFetcher.isNewer(result.version, inst) ->
                showBadge(R.string.update_available, R.color.badge_update)
            else -> showBadge(R.string.up_to_date, R.color.badge_ok)
        }
    }

    private fun showBadge(textRes: Int, colorRes: Int) {
        statusBadge.setText(textRes)
        statusBadge.backgroundTintList = ColorStateList.valueOf(
            getColor(colorRes)
        )
        statusBadge.visibility = View.VISIBLE
    }

    private fun openDownload(inBrowser: Boolean) {
        val v = latest ?: return
        val url = v.releaseUrl ?: VersionFetcher.releaseUrlFor(v.version)
        if (inBrowser) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } else {
            startActivity(
                Intent(this, WebViewActivity::class.java).apply {
                    putExtra(WebViewActivity.EXTRA_URL, url)
                    putExtra(WebViewActivity.EXTRA_VERSION, v.version)
                }
            )
        }
    }
}
