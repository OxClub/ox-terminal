package com.nexus.terminal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

data class SshProfile(val name: String, val host: String, val port: Int, val user: String, val keyPath: String)

/**
 * Sensitive data lives in EncryptedSharedPreferences (AES-256, key in Android Keystore).
 * Passwords are never stored. Only a *path* to a key inside the app sandbox is kept.
 * This store is never part of configuration export.
 */
object SecureStore {
    private fun prefs(ctx: Context): SharedPreferences {
        val key = MasterKey.Builder(ctx.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            ctx.applicationContext, "nexus_secure", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val safe = Regex("[A-Za-z0-9._@:-]+")
    fun validHost(s: String) = s.isNotBlank() && safe.matches(s)
    fun validUser(s: String) = s.isNotBlank() && Regex("[A-Za-z0-9._-]+").matches(s)

    fun profiles(ctx: Context): List<SshProfile> {
        val raw = prefs(ctx).getString("ssh_profiles", "[]") ?: "[]"
        val a = JSONArray(raw)
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            SshProfile(o.getString("name"), o.getString("host"), o.getInt("port"), o.getString("user"), o.optString("key"))
        }
    }

    fun saveProfiles(ctx: Context, list: List<SshProfile>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("name", it.name).put("host", it.host).put("port", it.port)
                .put("user", it.user).put("key", it.keyPath))
        }
        prefs(ctx).edit().putString("ssh_profiles", a.toString()).apply()
    }

    /** Builds the ssh command shown to (and run for) the user. */
    fun command(p: SshProfile): String {
        val key = if (p.keyPath.isNotBlank()) " -i '" + p.keyPath.replace("'", "'\\''") + "'" else ""
        return "ssh -p ${p.port}$key ${p.user}@${p.host}"
    }
}
