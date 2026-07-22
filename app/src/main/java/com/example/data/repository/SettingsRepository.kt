package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "developer_branding_prefs")
private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
private val INSTANSI_NAME_KEY = stringPreferencesKey("instansi_name")
private val OFFICER_NAME_KEY = stringPreferencesKey("officer_name")
private val OFFICER_NIP_KEY = stringPreferencesKey("officer_nip")

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gudang_settings", Context.MODE_PRIVATE)

    init {
        // Clear old dummy data if found, so fallback is empty list as requested
        val currentMerekAlat = prefs.getString("merek_alat", null)
        if (currentMerekAlat == "Sony|#|Logitech|#|Canon|#|Epson|#|HP") {
            prefs.edit().remove("merek_alat").apply()
        }
        val currentMerekBahan = prefs.getString("merek_bahan", null)
        if (currentMerekBahan == "Sinar Dunia|#|PaperOne|#|Joyko|#|Kenko|#|Faber-Castell") {
            prefs.edit().remove("merek_bahan").apply()
        }
        val currentRuang = prefs.getString("ruang", null)
        if (currentRuang == "Lab Komputer|#|Ruang Guru|#|Gudang Utama|#|Kelas X-A|#|Kelas XI-B") {
            prefs.edit().remove("ruang").apply()
        }
        val currentSumberDana = prefs.getString("sumber_dana", null)
        if (currentSumberDana == "BOS|#|Dana Komite|#|Bantuan Pemerintah|#|BOP") {
            prefs.edit().remove("sumber_dana").apply()
        }
        val currentKondisi = prefs.getString("kondisi", null)
        if (currentKondisi == "Normal|#|Perbaikan|#|Rusak|#|Expired") {
            prefs.edit().remove("kondisi").apply()
        }
    }

    companion object {
        private const val KEY_SHEETS_URL = "sheets_url"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_DEFAULT_OFFICER = "default_officer"
        private const val KEY_OFFICER_NIP = "officer_nip"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_INSTANSI_NAME = "instansi_name"
        private const val KEY_INSTANSI_LOGO_PATH = "instansi_logo_path"
        private const val KEY_USER_PROFILE_PHOTO = "user_profile_photo"
    }

    fun isDemoFinished(): Boolean {
        return prefs.getBoolean("is_demo_finished", false)
    }

    fun setDemoFinished(finished: Boolean) {
        prefs.edit().putBoolean("is_demo_finished", finished).apply()
    }

    fun checkAndInitializeBranding() {
        runBlocking {
            val isFirst = context.dataStore.data.first()[IS_FIRST_LAUNCH] ?: true
            if (isFirst) {
                context.dataStore.edit { preferences ->
                    preferences[IS_FIRST_LAUNCH] = false
                    preferences[INSTANSI_NAME_KEY] = "Pradipta Graha Digital"
                    preferences[OFFICER_NAME_KEY] = "Kevin Ricky Utama, S.Kom."
                    preferences[OFFICER_NIP_KEY] = "19980419202511035"
                }
                setInstansiName("Pradipta Graha Digital")
                setDefaultOfficer("Kevin Ricky Utama, S.Kom.")
                setOfficerNip("19980419202511035")
            }
        }
    }

    fun getInstansiName(): String {
        return prefs.getString(KEY_INSTANSI_NAME, "SMAN 1 Bobotsari") ?: "SMAN 1 Bobotsari"
    }

    fun setInstansiName(name: String) {
        prefs.edit().putString(KEY_INSTANSI_NAME, name).apply()
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[INSTANSI_NAME_KEY] = name
            }
        }
    }

    fun getInstansiLogoPath(): String {
        return prefs.getString(KEY_INSTANSI_LOGO_PATH, "") ?: ""
    }

    fun setInstansiLogoPath(path: String) {
        prefs.edit().putString(KEY_INSTANSI_LOGO_PATH, path).apply()
    }

    fun getUserProfilePhoto(): String {
        return prefs.getString(KEY_USER_PROFILE_PHOTO, "") ?: ""
    }

    fun setUserProfilePhoto(path: String) {
        prefs.edit().putString(KEY_USER_PROFILE_PHOTO, path).apply()
    }

    // Helper functions to save/get lists from SharedPreferences
    private fun getList(key: String, defaults: List<String>): List<String> {
        val raw = prefs.getString(key, null)
        if (raw == null) {
            // Save defaults first time
            saveList(key, defaults)
            return defaults
        }
        if (raw.isEmpty()) return emptyList()
        return raw.split("|#|")
    }

    private fun saveList(key: String, list: List<String>) {
        val serialized = list.joinToString("|#|")
        prefs.edit().putString(key, serialized).apply()
    }

    fun getMerekAlat(): List<String> {
        return getList("merek_alat", emptyList()).sorted()
    }

    fun saveMerekAlat(list: List<String>) {
        saveList("merek_alat", list.sorted())
    }

    fun getMerekBahan(): List<String> {
        return getList("merek_bahan", emptyList()).sorted()
    }

    fun saveMerekBahan(list: List<String>) {
        saveList("merek_bahan", list.sorted())
    }

    fun getRuang(): List<String> {
        return getList("ruang", emptyList()).sorted()
    }

    fun saveRuang(list: List<String>) {
        saveList("ruang", list.sorted())
    }

    fun getSumberDana(): List<String> {
        return getList("sumber_dana", emptyList()).sorted()
    }

    fun saveSumberDana(list: List<String>) {
        saveList("sumber_dana", list.sorted())
    }

    fun getKondisi(): List<String> {
        return getList("kondisi", emptyList()).sorted()
    }

    fun saveKondisi(list: List<String>) {
        saveList("kondisi", list.sorted())
    }

    fun getGuruMapel(): List<String> {
        return getList("guru_mapel", emptyList()).sorted()
    }

    fun saveGuruMapel(list: List<String>) {
        saveList("guru_mapel", list.sorted())
    }

    fun getStaf(): List<String> {
        return getList("staf", emptyList()).sorted()
    }

    fun saveStaf(list: List<String>) {
        saveList("staf", list.sorted())
    }

    fun getSheetsUrl(): String {
        return prefs.getString(KEY_SHEETS_URL, "") ?: ""
    }

    fun setSheetsUrl(url: String) {
        prefs.edit().putString(KEY_SHEETS_URL, url).apply()
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun getLastSyncTime(): String {
        return prefs.getString(KEY_LAST_SYNC, "-") ?: "-"
    }

    fun setLastSyncTime(time: String) {
        prefs.edit().putString(KEY_LAST_SYNC, time).apply()
    }

    fun getDefaultOfficer(): String {
        return prefs.getString(KEY_DEFAULT_OFFICER, "") ?: ""
    }

    fun setDefaultOfficer(officer: String) {
        prefs.edit().putString(KEY_DEFAULT_OFFICER, officer).apply()
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[OFFICER_NAME_KEY] = officer
            }
        }
    }

    fun getOfficerNip(): String {
        return prefs.getString(KEY_OFFICER_NIP, "") ?: ""
    }

    fun setOfficerNip(nip: String) {
        prefs.edit().putString(KEY_OFFICER_NIP, nip).apply()
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[OFFICER_NIP_KEY] = nip
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", loggedIn).apply()
    }

    fun getLoggedInUser(): String {
        return prefs.getString("logged_in_user", "") ?: ""
    }

    fun setLoggedInUser(username: String) {
        prefs.edit().putString("logged_in_user", username).apply()
    }

    fun getUserRole(): String {
        return prefs.getString("user_role", "siswa") ?: "siswa"
    }

    fun setUserRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("logged_in_user", "")
            .putString("user_role", "siswa")
            .apply()
    }

    fun getAppTheme(): String {
        return prefs.getString(KEY_APP_THEME, "auto") ?: "auto"
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString(KEY_APP_THEME, theme).apply()
    }

    fun getRecentMenus(): List<String> {
        return getList("recent_menus", listOf("Peminjaman", "Alat", "Laporan"))
    }

    fun saveRecentMenus(list: List<String>) {
        saveList("recent_menus", list)
    }

    fun getStudentPermissions(): Map<String, Boolean> {
        val defaults = mapOf(
            "peminjaman" to false,
            "peminjaman_form" to false,
            "peminjaman_riwayat" to false,

            "pengembalian" to false,
            "pengembalian_normal" to false,
            "pengembalian_parsial" to false,

            "qr_group" to false,
            "scan_qr" to false,
            "generate_qr" to false,

            "log_transaksi" to false,
            "log_transaksi_view" to false,
            "log_transaksi_export" to false,
            "log_sirkulasi" to false,
            "log_bahan_habis" to false,
            "log_stok" to false,
            "log_pemeliharaan" to false,
            "log_aktivitas" to false,

            "alat" to false,
            "alat_view" to false,
            "alat_detail" to false,
            "alat_import" to false,
            "alat_export" to false,

            "kondisi_alat" to false,
            "kondisi_alat_catat" to false,
            "kondisi_alat_view" to false,
            "kondisi_alat_report" to false,

            "alat_rusak" to false,
            "alat_rusak_submit" to false,
            "alat_rusak_view" to false,

            "pemeliharaan" to false,
            "pemeliharaan_tambah" to false,
            "pemeliharaan_view" to false,
            "pemeliharaan_history" to false,

            "bahan" to false,
            "bahan_view" to false,
            "bahan_detail" to false,
            "bahan_import" to false,
            "bahan_export" to false,

            "pemakaian_bahan" to false,
            "pemakaian_bahan_form" to false,
            "pemakaian_bahan_log" to false,

            "bahan_afkir" to false,
            "bahan_afkir_submit" to false,
            "bahan_afkir_view" to false,
            "bahan_afkir_report" to false,

            "master_data" to false,
            "master_data_view" to false,
            "master_data_manage" to false,

            "stok_opname" to false,
            "stok_opname_audit" to false,
            "stok_opname_reconcile" to false,

            "laporan" to false,
            "laporan_view" to false,
            "laporan_export" to false,
            "laporan_ringkasan" to false,
            "laporan_alat" to false,
            "laporan_bahan" to false,
            "laporan_afkir" to false,
            "laporan_peminjaman" to false,
            "laporan_pengembalian" to false,
            "laporan_alat_rusak" to false,
            "laporan_pemeliharaan" to false,
            "laporan_export_excel" to false,
            "laporan_print_pdf" to false
        )
        val map = mutableMapOf<String, Boolean>()
        defaults.forEach { (key, defaultVal) ->
            map[key] = prefs.getBoolean("perm_student_$key", defaultVal)
        }
        return map
    }

    fun saveStudentPermissions(map: Map<String, Boolean>) {
        val editor = prefs.edit()
        map.forEach { (key, value) ->
            editor.putBoolean("perm_student_$key", value)
        }
        editor.apply()
    }

    fun clearAllSettings() {
        prefs.edit().clear().apply()
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
}
