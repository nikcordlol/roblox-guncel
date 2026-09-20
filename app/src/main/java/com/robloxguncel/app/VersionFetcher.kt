package com.robloxguncel.app

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * Roblox'un Android surumunu birkac farkli kaynaktan bulur.
 *
 * Kaynaklar (sirayla denenir, biri basarisiz olursa digerine gecer):
 *  1. AppBrain       -> "The latest version available is 2.739.691"
 *  2. APKCombo       -> meta aciklamada "Roblox APK 2.739.691"
 *  3. APKMirror RSS  -> dogrudan release sayfasi linki (son ~60 yukleme)
 *  4. Google Play    -> ham HTML icindeki surum adi (son yedek)
 *
 * APKMirror'a indirme icin WebView uzerinden gidilir; APK hicbir zaman
 * baska bir yerden barindirilmaz.
 */
object VersionFetcher {

    data class Latest(
        val version: String,      // ornek: "2.739.691"
        val source: String,       // ornek: "AppBrain"
        val releaseUrl: String?   // biliniyorsa APKMirror release sayfasi
    )

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

    private const val APKMIRROR_ROBLOX_BASE =
        "https://www.apkmirror.com/apk/roblox-corporation/roblox/"

    private val VERSION_IN_TAG = Pattern.compile(
        ">\\s*([0-9]{1,2}\\.[0-9]{1,3}\\.[0-9]{1,5})\\s*<"
    )

    fun fetchLatest(): Latest? {
        fetchFromAppBrain()?.let { return it }
        fetchFromApkCombo()?.let { return it }
        fetchFromApkMirrorRss()?.let { return it }
        return fetchFromPlayStore()
    }

    /** "2.739.691" -> "https://www.apkmirror.com/apk/roblox-corporation/roblox/roblox-2-739-691-release/" */
    fun releaseUrlFor(version: String): String =
        APKMIRROR_ROBLOX_BASE + "roblox-" + version.replace('.', '-') + "-release/"

    /** a.b.c surumlerini sayisal karsilastirir; a daha yeniyse true. */
    fun isNewer(a: String, b: String): Boolean {
        val pa = a.split('.').map { it.toLongOrNull() ?: 0L }
        val pb = b.split('.').map { it.toLongOrNull() ?: 0L }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0L }
            val y = pb.getOrElse(i) { 0L }
            if (x != y) return x > y
        }
        return false
    }

    // --- kaynaklar ----------------------------------------------------------

    private fun fetchFromAppBrain(): Latest? {
        return try {
            val html = httpGet("https://www.appbrain.com/app/roblox/com.roblox.client")
                ?: return null
            val v = firstMatch(
                html,
                Pattern.compile(
                    "latest version available is ([0-9]{1,2}\\.[0-9]{1,3}\\.[0-9]{1,5})",
                    Pattern.CASE_INSENSITIVE
                ),
                VERSION_IN_TAG
            ) ?: return null
            Latest(v, "AppBrain", releaseUrlFor(v))
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromApkCombo(): Latest? {
        return try {
            val html = httpGet("https://apkcombo.com/roblox/com.roblox.client/")
                ?: return null
            val v = firstMatch(
                html,
                Pattern.compile(
                    "Roblox APK ([0-9]{1,2}\\.[0-9]{1,3}\\.[0-9]{1,5})",
                    Pattern.CASE_INSENSITIVE
                ),
                VERSION_IN_TAG
            ) ?: return null
            Latest(v, "APKCombo", releaseUrlFor(v))
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromApkMirrorRss(): Latest? {
        return try {
            for (page in 1..3) {
                val xml = httpGet("https://www.apkmirror.com/feed/?paged=$page") ?: continue
                val m = Pattern.compile(
                    "apk/roblox-corporation/roblox/roblox-([0-9]{1,2})-([0-9]{1,4})-([0-9]{1,6})-release/"
                ).matcher(xml)
                if (m.find()) {
                    val v = "${m.group(1)}.${m.group(2)}.${m.group(3)}"
                    val url = APKMIRROR_ROBLOX_BASE + m.group().removePrefix("apk/")
                    return Latest(v, "APKMirror", url)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromPlayStore(): Latest? {
        return try {
            val html = httpGet(
                "https://play.google.com/store/apps/details?id=com.roblox.client&hl=en"
            ) ?: return null
            val v = firstMatch(
                html,
                Pattern.compile("\"([0-9]{1,2}\\.[0-9]{1,3}\\.[0-9]{1,5})\"")
            ) ?: return null
            Latest(v, "Google Play", releaseUrlFor(v))
        } catch (e: Exception) {
            null
        }
    }

    // --- yardimcilar ----------------------------------------------------------

    private fun firstMatch(text: String, vararg patterns: Pattern): String? {
        for (p in patterns) {
            val m = p.matcher(text)
            if (m.find()) return m.group(1)
        }
        return null
    }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
