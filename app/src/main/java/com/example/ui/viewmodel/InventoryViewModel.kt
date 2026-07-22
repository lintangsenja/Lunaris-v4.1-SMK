package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.ItemEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.example.data.model.ItemWithStock
import com.example.data.network.GoogleSheetsSyncService
import com.example.data.repository.InventoryRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.example.data.model.ReportStats
import com.example.data.model.ReportDetailItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import java.util.TimeZone

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val syncService = GoogleSheetsSyncService()
    private val repository = InventoryRepository(db.inventoryDao(), syncService)
    val settingsRepository = SettingsRepository(application)
    private val firebaseService = com.example.data.network.FirebaseService(db)

    // Users State
    val allUsers: StateFlow<List<UserEntity>> = db.inventoryDao().getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Student Permissions Control State
    private val _studentPermissions = MutableStateFlow(settingsRepository.getStudentPermissions())
    val studentPermissions: StateFlow<Map<String, Boolean>> = _studentPermissions.asStateFlow()

    private var permissionsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        firebaseService.startRealtimeSync()
        initStudentPermissionsListener()
        seedDefaultUsers()
    }

    private fun seedDefaultUsers() {
        viewModelScope.launch {
            try {
                val dao = db.inventoryDao()
                val count = dao.getAllUsers().first().size
                val lintangUserEntity = UserEntity(
                    username = "lintang",
                    password = "lintanglunaris",
                    role = "super_admin",
                    fullName = "Lintang Senja"
                )
                if (count == 0) {
                    dao.insertUser(UserEntity("admin", "admin123", "super_admin", "Super Admin"))
                    dao.insertUser(lintangUserEntity)
                    dao.insertUser(UserEntity("siswa", "siswa19", "siswa", "Siswa Lunaris"))
                } else {
                    val lintangUser = dao.getUserByUsername("lintang")
                    if (lintangUser == null || lintangUser.role != "super_admin" || lintangUser.fullName != "Lintang Senja" || lintangUser.password != "lintanglunaris") {
                        dao.insertUser(lintangUserEntity)
                    }
                }
                writeUserToFirestore("lintang", "super_admin", "Lintang Senja")
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error seeding default users", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseService.stopRealtimeSync()
        permissionsListener?.remove()
    }

    private fun initStudentPermissionsListener() {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            permissionsListener = firestore.collection("settings").document("student_permissions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("InventoryVM", "Error listening to student_permissions", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data
                        if (data != null) {
                            val current = _studentPermissions.value.toMutableMap()
                            data.forEach { (key, value) ->
                                if (value is Boolean) {
                                    current[key] = value
                                }
                            }
                            _studentPermissions.value = current
                            settingsRepository.saveStudentPermissions(current)
                        }
                    } else {
                        saveStudentPermissionsToFirestore(_studentPermissions.value)
                    }
                }
        } catch (e: Exception) {
            Log.e("InventoryVM", "Failed to init student permissions listener", e)
        }
    }

    private fun saveStudentPermissionsToFirestore(permissions: Map<String, Boolean>) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val data = hashMapOf<String, Any>()
            permissions.forEach { (k, v) -> data[k] = v }
            firestore.collection("settings").document("student_permissions")
                .set(data, com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            Log.e("InventoryVM", "Failed to save student permissions to Firestore", e)
        }
    }

    fun updateStudentPermission(key: String, isAllowed: Boolean) {
        val updated = _studentPermissions.value.toMutableMap()
        updated[key] = isAllowed
        _studentPermissions.value = updated
        settingsRepository.saveStudentPermissions(updated)
        saveStudentPermissionsToFirestore(updated)
    }

    fun updateStudentPermissionsBatch(updates: Map<String, Boolean>) {
        val updated = _studentPermissions.value.toMutableMap()
        updated.putAll(updates)
        _studentPermissions.value = updated
        settingsRepository.saveStudentPermissions(updated)
        saveStudentPermissionsToFirestore(updated)
    }

    fun resetStudentPermissionsToDefault() {
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
        _studentPermissions.value = defaults
        settingsRepository.saveStudentPermissions(defaults)
        saveStudentPermissionsToFirestore(defaults)
    }

    // Authentication States
    private val _isLoggedIn = MutableStateFlow(settingsRepository.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userRole = MutableStateFlow(settingsRepository.getUserRole())
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _loggedInUser = MutableStateFlow(settingsRepository.getLoggedInUser())
    val loggedInUser: StateFlow<String> = _loggedInUser.asStateFlow()

    fun login(usernameInput: String, passwordInput: String, onResult: (Boolean, String) -> Unit) {
        val username = usernameInput.trim()
        val password = passwordInput.trim()

        viewModelScope.launch {
            val userFromDb = db.inventoryDao().getUserByUsername(username)

            val expectedRole = when {
                userFromDb != null && userFromDb.password == password -> {
                    if (userFromDb.role == "super_admin" || userFromDb.role == "admin") "admin" else "siswa"
                }
                username == "admin" && password == "admin123" -> "admin"
                username == "lintang" && password == "lintanglunaris" -> "admin"
                username == "siswa" && password == "siswa19" -> "siswa"
                else -> {
                    onResult(false, "Username atau password salah!")
                    return@launch
                }
            }

            val userRoleActual = userFromDb?.role ?: (if (expectedRole == "admin") "admin" else "siswa")
            val email = "${username}@lunaris.com"

            val saveLocalStateAndSuccess = {
                settingsRepository.setLoggedIn(true)
                settingsRepository.setLoggedInUser(username)
                settingsRepository.setUserRole(expectedRole)

                _isLoggedIn.value = true
                _userRole.value = expectedRole
                _loggedInUser.value = username

                val displayName = when (userRoleActual) {
                    "super_admin" -> "Super Admin"
                    "admin" -> "Admin"
                    else -> "Siswa"
                }
                onResult(true, "Login berhasil sebagai $displayName!")
            }

            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            writeUserToFirestore(username, expectedRole)
                            saveLocalStateAndSuccess()
                        } else {
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { createDocTask ->
                                    if (createDocTask.isSuccessful) {
                                        writeUserToFirestore(username, expectedRole)
                                        saveLocalStateAndSuccess()
                                    } else {
                                        Log.e("InventoryVM", "Firebase login/create failed, falling back to local", createDocTask.exception)
                                        saveLocalStateAndSuccess()
                                    }
                                }
                        }
                    }
            } catch (e: Exception) {
                Log.e("InventoryVM", "Firebase SDK failure, falling back to local", e)
                saveLocalStateAndSuccess()
            }
        }
    }

    fun registerUser(
        usernameInput: String,
        passwordInput: String,
        role: String,
        fullNameInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val username = usernameInput.trim().lowercase()
        val password = passwordInput.trim()
        val fullName = fullNameInput.trim().ifEmpty { username }

        if (username.isBlank() || password.isBlank()) {
            onResult(false, "Username dan password tidak boleh kosong!")
            return
        }

        viewModelScope.launch {
            try {
                val existing = db.inventoryDao().getUserByUsername(username)
                if (existing != null) {
                    onResult(false, "Username '$username' sudah terdaftar!")
                    return@launch
                }

                val newUser = UserEntity(
                    username = username,
                    password = password,
                    role = role,
                    fullName = fullName,
                    createdAt = System.currentTimeMillis()
                )
                db.inventoryDao().insertUser(newUser)
                writeUserToFirestore(username, if (role == "super_admin" || role == "admin") "admin" else "siswa", fullName, password)
                onResult(true, "Pengguna '$username' berhasil ditambahkan!")
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error registering user", e)
                onResult(false, "Gagal mendaftarkan pengguna: ${e.message}")
            }
        }
    }

    fun deleteUser(usernameToDelete: String, onResult: (Boolean, String) -> Unit) {
        val activeUser = _loggedInUser.value
        if (usernameToDelete.equals(activeUser, ignoreCase = true)) {
            onResult(false, "Anda tidak dapat menghapus akun Anda yang sedang aktif!")
            return
        }
        if (usernameToDelete.equals("admin", ignoreCase = true) || usernameToDelete.equals("lintang", ignoreCase = true)) {
            onResult(false, "Akun default Super Admin Lintang Senja tidak dapat dihapus!")
            return
        }

        viewModelScope.launch {
            try {
                db.inventoryDao().deleteUserByUsername(usernameToDelete)
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(usernameToDelete).delete()
                } catch (ex: Exception) {
                    Log.e("InventoryVM", "Error deleting user from Firestore", ex)
                }
                onResult(true, "Pengguna '$usernameToDelete' berhasil dihapus!")
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error deleting user", e)
                onResult(false, "Gagal menghapus pengguna: ${e.message}")
            }
        }
    }

    private fun writeUserToFirestore(username: String, role: String, fullName: String = "", password: String = "") {
        try {
            val dbFirestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val resolvedFullName = when {
                fullName.isNotBlank() -> fullName
                username.equals("lintang", ignoreCase = true) -> "Lintang Senja"
                username.equals("admin", ignoreCase = true) -> "Super Admin"
                else -> ""
            }
            val userData = hashMapOf<String, Any>(
                "username" to username,
                "role" to role,
                "fullName" to resolvedFullName,
                "isSuperAdmin" to (role == "super_admin" || username.equals("lintang", ignoreCase = true)),
                "isProtected" to (username.equals("admin", ignoreCase = true) || username.equals("lintang", ignoreCase = true)),
                "isActive" to true,
                "lastLogin" to System.currentTimeMillis()
            )
            if (password.isNotBlank()) {
                userData["password"] = password
            }
            dbFirestore.collection("users").document(username).set(userData, com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            Log.e("InventoryVM", "Failed to write user to Firestore", e)
        }
    }

    fun resetStudentPassword(
        usernameToReset: String,
        newPasswordInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val username = usernameToReset.trim()
        val newPassword = newPasswordInput.trim()

        if (username.isBlank() || newPassword.isBlank()) {
            onResult(false, "Username dan kata sandi baru tidak boleh kosong!")
            return
        }

        if (newPassword.length < 4) {
            onResult(false, "Kata sandi baru minimal 4 karakter!")
            return
        }

        viewModelScope.launch {
            try {
                val user = db.inventoryDao().getUserByUsername(username)
                val targetUser = if (user != null) {
                    user.copy(password = newPassword)
                } else {
                    val role = if (username.equals("siswa", ignoreCase = true)) "siswa" else "admin"
                    val defaultName = when (username.lowercase()) {
                        "lintang" -> "Lintang Senja"
                        "admin" -> "Super Admin"
                        else -> "Siswa Lunaris"
                    }
                    UserEntity(
                        username = username,
                        password = newPassword,
                        role = role,
                        fullName = defaultName
                    )
                }

                db.inventoryDao().insertUser(targetUser)
                writeUserToFirestore(
                    username = username,
                    role = if (targetUser.role == "super_admin" || targetUser.role == "admin") "admin" else "siswa",
                    fullName = targetUser.fullName,
                    password = newPassword
                )

                onResult(true, "Kata sandi untuk pengguna '$username' berhasil diperbarui!")
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error resetting student password", e)
                onResult(false, "Gagal mereset kata sandi: ${e.message}")
            }
        }
    }

    fun updateUserProfileData(
        usernameInput: String,
        fullNameInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val username = usernameInput.trim()
        val fullName = fullNameInput.trim()

        if (username.isBlank()) {
            onResult(false, "Username tidak valid!")
            return
        }

        viewModelScope.launch {
            try {
                val existingUser = db.inventoryDao().getUserByUsername(username)
                if (existingUser != null) {
                    val updatedUser = existingUser.copy(fullName = fullName)
                    db.inventoryDao().insertUser(updatedUser)
                    writeUserToFirestore(username, if (existingUser.role == "super_admin" || existingUser.role == "admin") "admin" else "siswa", fullName)
                    onResult(true, "Data profil berhasil diperbarui!")
                } else {
                    val role = if (username.equals("siswa", ignoreCase = true)) "siswa" else "super_admin"
                    val defaultPass = when (username.lowercase()) {
                        "lintang" -> "lintanglunaris"
                        "admin" -> "admin123"
                        else -> "siswa19"
                    }
                    val newUser = UserEntity(
                        username = username,
                        password = defaultPass,
                        role = role,
                        fullName = fullName
                    )
                    db.inventoryDao().insertUser(newUser)
                    writeUserToFirestore(username, if (role == "super_admin" || role == "admin") "admin" else "siswa", fullName)
                    onResult(true, "Data profil berhasil disimpan!")
                }
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error updating profile data", e)
                onResult(false, "Gagal memperbarui profil: ${e.message}")
            }
        }
    }

    fun changeUserPassword(
        usernameInput: String,
        currentPassInput: String,
        newPassInput: String,
        confirmPassInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val username = usernameInput.trim()
        val currentPass = currentPassInput.trim()
        val newPass = newPassInput.trim()
        val confirmPass = confirmPassInput.trim()

        if (currentPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            onResult(false, "Semua bidang password wajib diisi!")
            return
        }

        if (newPass.length < 4) {
            onResult(false, "Password baru minimal 4 karakter!")
            return
        }

        if (newPass != confirmPass) {
            onResult(false, "Konfirmasi password baru tidak cocok!")
            return
        }

        viewModelScope.launch {
            try {
                var user = db.inventoryDao().getUserByUsername(username)
                if (user == null) {
                    val defaultValid = when (username.lowercase()) {
                        "lintang" -> currentPass == "lintanglunaris"
                        "admin" -> currentPass == "admin123"
                        "siswa" -> currentPass == "siswa19"
                        else -> false
                    }
                    if (!defaultValid) {
                        onResult(false, "Password saat ini tidak sesuai!")
                        return@launch
                    }
                    val role = if (username.equals("siswa", ignoreCase = true)) "siswa" else "super_admin"
                    val defaultName = when (username.lowercase()) {
                        "lintang" -> "Lintang Senja"
                        "admin" -> "Super Admin"
                        else -> "Siswa Lunaris"
                    }
                    user = UserEntity(
                        username = username,
                        password = currentPass,
                        role = role,
                        fullName = defaultName
                    )
                    db.inventoryDao().insertUser(user)
                }

                if (user.password != currentPass) {
                    onResult(false, "Password saat ini tidak sesuai!")
                    return@launch
                }

                val updatedUser = user.copy(password = newPass)
                db.inventoryDao().insertUser(updatedUser)
                writeUserToFirestore(username, if (user.role == "super_admin" || user.role == "admin") "admin" else "siswa", user.fullName)

                try {
                    val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    authUser?.updatePassword(newPass)
                } catch (e: Exception) {
                    Log.e("InventoryVM", "Firebase auth update password failed", e)
                }

                onResult(true, "Kata sandi berhasil diperbarui!")
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error changing user password", e)
                onResult(false, "Gagal mengubah kata sandi: ${e.message}")
            }
        }
    }

    fun logout() {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.e("InventoryVM", "Firebase signOut error", e)
        }
        settingsRepository.logout()
        _isLoggedIn.value = false
        _userRole.value = "siswa"
        _loggedInUser.value = ""
    }

    // Reactively flowing DB states
    val itemsWithStock: StateFlow<List<ItemWithStock>> = repository.itemsWithStock
        .combine(userRole) { list, role ->
            if (role == "siswa") {
                list.filter { it.isBorrowable }
            } else {
                list
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStok: StateFlow<Int> = itemsWithStock
        .map { list -> list.sumOf { it.stokTersedia } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allPemakaianBahan: StateFlow<List<com.example.data.entity.PemakaianBahanEntity>> = repository.allPemakaianBahan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBahanAfkir: StateFlow<List<com.example.data.entity.BahanAfkirEntity>> = repository.allBahanAfkir
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<LoanTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val harianChartData: StateFlow<List<Pair<String, Float>>> = allTransactions
        .map { transactions ->
            val counts = mutableMapOf<Int, Int>()
            val dayConstants = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
            dayConstants.forEach { counts[it] = 0 }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val now = Calendar.getInstance()
            val currentWeek = now.get(Calendar.WEEK_OF_YEAR)
            val currentYear = now.get(Calendar.YEAR)

            transactions.forEach { tx ->
                try {
                    val date = sdf.parse(tx.tanggal)
                    if (date != null) {
                        val cal = Calendar.getInstance()
                        cal.time = date
                        val txWeek = cal.get(Calendar.WEEK_OF_YEAR)
                        val txYear = cal.get(Calendar.YEAR)
                        if (txWeek == currentWeek && txYear == currentYear) {
                            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                            counts[dayOfWeek] = (counts[dayOfWeek] ?: 0) + 1
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            listOf(
                "Sen" to (counts[Calendar.MONDAY] ?: 0).toFloat(),
                "Sel" to (counts[Calendar.TUESDAY] ?: 0).toFloat(),
                "Rab" to (counts[Calendar.WEDNESDAY] ?: 0).toFloat(),
                "Kam" to (counts[Calendar.THURSDAY] ?: 0).toFloat(),
                "Jum" to (counts[Calendar.FRIDAY] ?: 0).toFloat(),
                "Sab" to (counts[Calendar.SATURDAY] ?: 0).toFloat(),
                "Min" to (counts[Calendar.SUNDAY] ?: 0).toFloat()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(
            "Sen" to 0f, "Sel" to 0f, "Rab" to 0f, "Kam" to 0f, "Jum" to 0f, "Sab" to 0f, "Min" to 0f
        ))

    val mingguanChartData: StateFlow<List<Pair<String, Float>>> = allTransactions
        .map { transactions ->
            val now = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val counts = IntArray(4)

            transactions.forEach { tx ->
                try {
                    val date = sdf.parse(tx.tanggal)
                    if (date != null) {
                        val diffMillis = now.timeInMillis - date.time
                        val diffWeeks = (diffMillis / (1000L * 60 * 60 * 24 * 7)).toInt()
                        if (diffWeeks in 0..3) {
                           counts[3 - diffWeeks] += 1
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            listOf(
                "M-4" to counts[0].toFloat(),
                "M-3" to counts[1].toFloat(),
                "M-2" to counts[2].toFloat(),
                "M-1" to counts[3].toFloat()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(
            "M-4" to 0f, "M-3" to 0f, "M-2" to 0f, "M-1" to 0f
        ))

    val bulananChartData: StateFlow<List<Pair<String, Float>>> = allTransactions
        .map { transactions ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            val counts = mutableMapOf<Int, Int>()
            val last6Months = mutableListOf<Int>()

            val cal = Calendar.getInstance()
            for (i in 5 downTo 0) {
                val tempCal = Calendar.getInstance()
                tempCal.add(Calendar.MONTH, -i)
                val m = tempCal.get(Calendar.MONTH)
                last6Months.add(m)
                counts[m] = 0
            }

            transactions.forEach { tx ->
                try {
                    val date = sdf.parse(tx.tanggal)
                    if (date != null) {
                        val txCal = Calendar.getInstance()
                        txCal.time = date
                        val txMonth = txCal.get(Calendar.MONTH)
                        val txYear = txCal.get(Calendar.YEAR)
                        val nowYear = cal.get(Calendar.YEAR)
                        if (counts.containsKey(txMonth) && (txYear == nowYear || txYear == nowYear - 1)) {
                            counts[txMonth] = (counts[txMonth] ?: 0) + 1
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            last6Months.map { m ->
                monthNames[m] to (counts[m] ?: 0).toFloat()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(
            "Jan" to 0f, "Feb" to 0f, "Mar" to 0f, "Apr" to 0f, "Mei" to 0f, "Jun" to 0f
        ))

    val activeTransactions: StateFlow<List<LoanTransactionEntity>> = repository.activeTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDamagedItems: StateFlow<List<com.example.data.entity.DamagedItemEntity>> = repository.allDamagedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<com.example.data.entity.CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUnits: StateFlow<List<com.example.data.entity.UnitEntity>> = repository.allUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Support for direct reactive Bahan operations requested by the user
    private val _allBahan = MutableStateFlow<List<ItemWithStock>>(emptyList())
    val allBahan: StateFlow<List<ItemWithStock>> = _allBahan.asStateFlow()

    fun getAllBahan() {
        viewModelScope.launch {
            try {
                val items = db.inventoryDao().getItemsWithStock().first()
                val bahanOnly = items.filter { it.kategori.equals("Logistik", ignoreCase = true) }
                _allBahan.value = bahanOnly
                Log.d("InventoryVM", "getAllBahan fetched ${bahanOnly.size} bahan items successfully.")
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error in getAllBahan", e)
            }
        }
    }

    // Default date filter range (1st of current month to last day of current month) in Asia/Jakarta TimeZone
    private val _startDateText = MutableStateFlow(
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale("id", "ID")).let { cal ->
            cal.set(Calendar.DAY_OF_MONTH, 1)
            SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID")).format(cal.time)
        }
    )
    val startDateText = _startDateText.asStateFlow()

    private val _endDateText = MutableStateFlow(
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale("id", "ID")).let { cal ->
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID")).format(cal.time)
        }
    )
    val endDateText = _endDateText.asStateFlow()

    fun updateDateFilter(start: String, end: String) {
        _startDateText.value = start
        _endDateText.value = end
    }

    fun fetchReportStats(startDate: String, endDate: String): Flow<ReportStats> {
        return combine(
            repository.itemsWithStock,
            repository.allTransactions,
            repository.allDamagedItems,
            repository.allBahanAfkir
        ) { items, transactions, damaged, afkir ->
            val allLoanItems = repository.getAllLoanItems()
            val loanItemsByTx = allLoanItems.groupBy { it.idTransaksi }

            val tersedia = items.sumOf { it.stokTersedia }

            var dipinjam = 0
            val filteredTx = transactions.filter { 
                it.tanggal >= startDate && 
                it.tanggal <= endDate && 
                it.status == "Dipinjam" 
            }
            for (tx in filteredTx) {
                val txItems = loanItemsByTx[tx.idTransaksi] ?: emptyList()
                dipinjam += txItems.sumOf { it.jumlah }
            }

            val filteredDamaged = damaged.filter {
                it.tanggalKerusakan >= startDate &&
                it.tanggalKerusakan <= endDate &&
                (it.status.equals("Servis Luar/Pemeliharaan", ignoreCase = true) || 
                 it.status.equals("Pemeliharaan", ignoreCase = true))
            }
            val rusak = filteredDamaged.sumOf { it.jumlah }

            val filteredAfkir = afkir.filter {
                it.tanggalAfkir >= startDate &&
                it.tanggalAfkir <= endDate
            }
            val afkirCount = filteredAfkir.sumOf { it.jumlahAfkir }

            val total = tersedia + dipinjam + rusak

            ReportStats(
                total = total,
                tersedia = tersedia,
                dipinjam = dipinjam,
                rusak = rusak,
                afkir = afkirCount
            )
        }
    }

    fun fetchReportDetailItems(status: String, startDate: String, endDate: String): Flow<List<ReportDetailItem>> {
        return combine(
            repository.itemsWithStock,
            repository.allTransactions,
            repository.allDamagedItems,
            repository.allBahanAfkir
        ) { items, transactions, damaged, afkir ->
            val allLoanItems = repository.getAllLoanItems()
            val loanItemsByTx = allLoanItems.groupBy { it.idTransaksi }

            when (status) {
                "Tersedia" -> {
                    items.filter { it.stokTersedia > 0 }.map { item ->
                        ReportDetailItem(
                            id = item.idBarang,
                            name = item.namaBarang,
                            categoryOrRoom = item.ruang.ifBlank { "Lainnya" },
                            quantity = item.stokTersedia,
                            dateOrStatus = item.kondisi.ifBlank { "Baik" },
                            extra = item.satuan
                        )
                    }
                }
                "Dipinjam" -> {
                    val list = mutableListOf<ReportDetailItem>()
                    val filteredTx = transactions.filter {
                        it.tanggal >= startDate &&
                        it.tanggal <= endDate &&
                        it.status == "Dipinjam"
                    }
                    for (tx in filteredTx) {
                        val txItems = loanItemsByTx[tx.idTransaksi] ?: emptyList()
                        for (item in txItems) {
                            list.add(
                                ReportDetailItem(
                                    id = tx.idTransaksi,
                                    name = item.namaBarang,
                                    categoryOrRoom = tx.namaPeminjam + " (${tx.kelas})",
                                    quantity = item.jumlah,
                                    dateOrStatus = tx.tanggal,
                                    extra = "Dipinjam"
                                )
                            )
                        }
                    }
                    list.sortedByDescending { it.dateOrStatus }
                }
                "Perbaikan" -> {
                    val filteredDamaged = damaged.filter {
                        it.tanggalKerusakan >= startDate &&
                        it.tanggalKerusakan <= endDate &&
                        (it.status.equals("Servis Luar/Pemeliharaan", ignoreCase = true) ||
                         it.status.equals("Pemeliharaan", ignoreCase = true))
                    }
                    filteredDamaged.map { d ->
                        ReportDetailItem(
                            id = d.idBarang,
                            name = d.namaBarang,
                            categoryOrRoom = d.keteranganKerusakan.ifBlank { "Pemeliharaan rutin" },
                            quantity = d.jumlah,
                            dateOrStatus = d.tanggalKerusakan,
                            extra = d.status
                        )
                    }.sortedByDescending { it.dateOrStatus }
                }
                "Afkir" -> {
                    val filteredAfkir = afkir.filter {
                        it.tanggalAfkir >= startDate &&
                        it.tanggalAfkir <= endDate
                    }
                    filteredAfkir.map { a ->
                        ReportDetailItem(
                            id = a.idBarang,
                            name = a.namaBarang,
                            categoryOrRoom = a.alasan.ifBlank { "Afkir Permanen" },
                            quantity = a.jumlahAfkir,
                            dateOrStatus = a.tanggalAfkir,
                            extra = a.satuan
                        )
                    }.sortedByDescending { it.dateOrStatus }
                }
                "Stok Aman" -> {
                    items.filter { it.stokTersedia > 2 }.map { item ->
                        ReportDetailItem(
                            id = item.idBarang,
                            name = item.namaBarang,
                            categoryOrRoom = item.ruang.ifBlank { "Lainnya" },
                            quantity = item.stokTersedia,
                            dateOrStatus = "Aman",
                            extra = item.satuan.ifBlank { "Pcs" }
                        )
                    }
                }
                "Perlu Pengadaan" -> {
                    items.filter { it.stokTersedia == 0 }.map { item ->
                        ReportDetailItem(
                            id = item.idBarang,
                            name = item.namaBarang,
                            categoryOrRoom = item.ruang.ifBlank { "Lainnya" },
                            quantity = item.stokTersedia,
                            dateOrStatus = "Habis",
                            extra = item.satuan.ifBlank { "Pcs" }
                        )
                    }
                }
                "Stok Kritis" -> {
                    items.filter { it.stokTersedia in 1..2 }.map { item ->
                        ReportDetailItem(
                            id = item.idBarang,
                            name = item.namaBarang,
                            categoryOrRoom = item.ruang.ifBlank { "Lainnya" },
                            quantity = item.stokTersedia,
                            dateOrStatus = "Kritis",
                            extra = item.satuan.ifBlank { "Pcs" }
                        )
                    }
                }
                else -> emptyList()
            }
        }
    }

    // Explicitly expose maintenance items from damaged_items table, matching Status and synchronized with Date Filters
    val maintenanceItems: StateFlow<List<com.example.data.entity.DamagedItemEntity>> = allDamagedItems
        .map { list ->
            val result = list.filter { 
                it.status.equals("Servis Luar/Pemeliharaan", ignoreCase = true) || 
                it.status.equals("Pemeliharaan", ignoreCase = true)
            }
            Log.d("InventoryVM", "Loaded ${result.size} maintenance items from damaged_items table")
            result
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings States
    private val _sheetsUrl = MutableStateFlow(settingsRepository.getSheetsUrl())
    val sheetsUrl = _sheetsUrl.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow(settingsRepository.isAutoSyncEnabled())
    val autoSyncEnabled = _autoSyncEnabled.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(settingsRepository.getLastSyncTime())
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val _defaultOfficer = MutableStateFlow(settingsRepository.getDefaultOfficer())
    val defaultOfficer = _defaultOfficer.asStateFlow()

    private val _officerNip = MutableStateFlow(settingsRepository.getOfficerNip())
    val officerNip = _officerNip.asStateFlow()

    private val _appTheme = MutableStateFlow(settingsRepository.getAppTheme())
    val appTheme = _appTheme.asStateFlow()

    private val _recentMenus = MutableStateFlow(settingsRepository.getRecentMenus())
    val recentMenus = _recentMenus.asStateFlow()

    // Drawer state synchronization
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen = _isDrawerOpen.asStateFlow()

    fun setDrawerOpen(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    fun logMenuVisit(menuName: String) {
        val currentList = _recentMenus.value.toMutableList()
        currentList.remove(menuName)
        currentList.add(0, menuName)
        val truncated = currentList.distinct().take(3)
        settingsRepository.saveRecentMenus(truncated)
        _recentMenus.value = truncated
    }

    private val _instansiName = MutableStateFlow(settingsRepository.getInstansiName())
    val instansiName = _instansiName.asStateFlow()

    private val _instansiLogoPath = MutableStateFlow(settingsRepository.getInstansiLogoPath())
    val instansiLogoPath = _instansiLogoPath.asStateFlow()

    private val _userProfilePhoto = MutableStateFlow(settingsRepository.getUserProfilePhoto())
    val userProfilePhoto: StateFlow<String> = _userProfilePhoto.asStateFlow()

    private val _merekAlat = MutableStateFlow(settingsRepository.getMerekAlat())
    val merekAlat = _merekAlat.asStateFlow()

    private val _merekBahan = MutableStateFlow(settingsRepository.getMerekBahan())
    val merekBahan = _merekBahan.asStateFlow()

    private val _ruang = MutableStateFlow(settingsRepository.getRuang())
    val ruang = _ruang.asStateFlow()

    private val _sumberDana = MutableStateFlow(settingsRepository.getSumberDana())
    val sumberDana = _sumberDana.asStateFlow()

    private val _kondisi = MutableStateFlow(settingsRepository.getKondisi())
    val kondisi = _kondisi.asStateFlow()

    private val _guruMapel = MutableStateFlow(settingsRepository.getGuruMapel())
    val guruMapel = _guruMapel.asStateFlow()

    private val _staf = MutableStateFlow(settingsRepository.getStaf())
    val staf = _staf.asStateFlow()

    fun updateMerekAlat(list: List<String>) {
        val sorted = list.sorted()
        settingsRepository.saveMerekAlat(sorted)
        _merekAlat.value = sorted
    }

    fun updateMerekBahan(list: List<String>) {
        val sorted = list.sorted()
        settingsRepository.saveMerekBahan(sorted)
        _merekBahan.value = sorted
    }

    fun updateRuang(list: List<String>) {
        val sorted = list.sorted()
        settingsRepository.saveRuang(sorted)
        _ruang.value = sorted
    }

    fun updateSumberDana(list: List<String>) {
        val sorted = list.sorted()
        settingsRepository.saveSumberDana(sorted)
        _sumberDana.value = sorted
    }

    fun updateKondisi(list: List<String>) {
        val sorted = list.sorted()
        settingsRepository.saveKondisi(sorted)
        _kondisi.value = sorted
    }

    fun updateGuruMapel(list: List<String>) {
        val sorted = list.sorted()
        settingsRepository.saveGuruMapel(sorted)
        _guruMapel.value = sorted
    }

    fun updateStaf(list: List<String>) {
        val sorted = list.sorted()
        settingsRepository.saveStaf(sorted)
        _staf.value = sorted
    }

    // Sync status states
    private val _syncProgress = MutableStateFlow("")
    val syncProgress = _syncProgress.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError = _syncError.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    init {
        // Pre-populate profile database table from settings if empty, and collect reactively
        viewModelScope.launch {
            try {
                val dao = db.inventoryDao()
                val existing = dao.getProfile()
                if (existing == null) {
                    val defaultName = settingsRepository.getDefaultOfficer().ifBlank { "Administrator" }
                    val defaultNip = settingsRepository.getOfficerNip().ifBlank { "-" }
                    val defaultInstansi = settingsRepository.getInstansiName().ifBlank { "Gudang Utama Lunaris" }
                    val defaultLogo = settingsRepository.getInstansiLogoPath()

                    val initialProfile = com.example.data.entity.ProfileEntity(
                        id = 1,
                        namaPetugas = defaultName,
                        nip = defaultNip,
                        namaInstansi = defaultInstansi,
                        fotoUri = defaultLogo
                    )
                    dao.insertProfile(initialProfile)
                }
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error pre-populating profile", e)
            }
        }

        viewModelScope.launch {
            try {
                db.inventoryDao().getProfileFlow().collect { profileEntity ->
                    profileEntity?.let {
                        _defaultOfficer.value = it.namaPetugas
                        _officerNip.value = it.nip
                        _instansiName.value = it.namaInstansi
                        if (it.fotoUri.isNotBlank()) {
                            _userProfilePhoto.value = it.fotoUri
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error collecting profile flow", e)
            }
        }

        viewModelScope.launch {
            itemsWithStock.collect { list ->
                _allBahan.value = list.filter { it.kategori.equals("Logistik", ignoreCase = true) }
            }
        }
    }

    fun updateSheetsUrl(url: String) {
        settingsRepository.setSheetsUrl(url)
        _sheetsUrl.value = url
    }

    fun updateAutoSyncEnabled(enabled: Boolean) {
        settingsRepository.setAutoSyncEnabled(enabled)
        _autoSyncEnabled.value = enabled
    }

    fun updateLastSyncTime(time: String) {
        settingsRepository.setLastSyncTime(time)
        _lastSyncTime.value = time
    }

    fun updateDefaultOfficer(officer: String) {
        settingsRepository.setDefaultOfficer(officer)
        _defaultOfficer.value = officer
        viewModelScope.launch {
            try {
                val dao = db.inventoryDao()
                val existing = dao.getProfile() ?: com.example.data.entity.ProfileEntity(namaPetugas = "", nip = "", namaInstansi = "", fotoUri = "")
                dao.insertProfile(existing.copy(namaPetugas = officer))
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error updating default officer", e)
            }
        }
    }

    fun updateOfficerNip(nip: String) {
        settingsRepository.setOfficerNip(nip)
        _officerNip.value = nip
        viewModelScope.launch {
            try {
                val dao = db.inventoryDao()
                val existing = dao.getProfile() ?: com.example.data.entity.ProfileEntity(namaPetugas = "", nip = "", namaInstansi = "", fotoUri = "")
                dao.insertProfile(existing.copy(nip = nip))
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error updating officer NIP", e)
            }
        }
    }

    fun updateAppTheme(theme: String) {
        settingsRepository.setAppTheme(theme)
        _appTheme.value = theme
    }

    fun updateInstansiName(name: String) {
        settingsRepository.setInstansiName(name)
        _instansiName.value = name
        viewModelScope.launch {
            try {
                val dao = db.inventoryDao()
                val existing = dao.getProfile() ?: com.example.data.entity.ProfileEntity(namaPetugas = "", nip = "", namaInstansi = "", fotoUri = "")
                dao.insertProfile(existing.copy(namaInstansi = name))
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error updating instansi name", e)
            }
        }
    }

    fun updateUserProfilePhoto(path: String) {
        settingsRepository.setUserProfilePhoto(path)
        _userProfilePhoto.value = path
        viewModelScope.launch {
            try {
                val dao = db.inventoryDao()
                val existing = dao.getProfile() ?: com.example.data.entity.ProfileEntity(namaPetugas = "", nip = "", namaInstansi = "", fotoUri = "")
                dao.insertProfile(existing.copy(fotoUri = path))
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error updating user profile photo", e)
            }
        }
    }

    fun updateInstansiLogoPath(path: String) {
        settingsRepository.setInstansiLogoPath(path)
        _instansiLogoPath.value = path
        viewModelScope.launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val docRef = firestore.collection("pengaturan_global").document("profil_admin")
                docRef.set(mapOf("logo_url" to path), com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error syncing instansi logo path to firestore", e)
            }
        }
    }

    fun insertBahan(
        name: String,
        stokAwal: Int,
        kategori: String = "Logistik",
        satuan: String = "",
        merekAlat: String = "",
        ruang: String = "",
        sumberDana: String? = null,
        kondisi: String = "",
        keterangan: String = "",
        isBorrowable: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    onError("Nama barang tidak boleh kosong!")
                    return@launch
                }
                if (stokAwal < 1) {
                    onError("Stok awal minimal harus diisi angka 1!")
                    return@launch
                }

                val currentItems = itemsWithStock.value
                val existing = currentItems.find { it.namaBarang.equals(name.trim(), ignoreCase = true) }
                if (existing != null) {
                    val newStokAwal = existing.stokAwal + stokAwal
                    val updatedItemEntity = ItemEntity(
                        idBarang = existing.idBarang,
                        namaBarang = existing.namaBarang,
                        stokAwal = newStokAwal,
                        kategori = if (kategori.isNotBlank()) kategori.trim() else existing.kategori,
                        satuan = if (satuan.isNotBlank()) satuan.trim() else existing.satuan,
                        stokRusak = existing.stokRusak,
                        merekAlat = if (merekAlat.isNotBlank()) merekAlat.trim() else existing.merekAlat,
                        ruang = if (ruang.isNotBlank()) ruang.trim() else existing.ruang,
                        sumberDana = sumberDana ?: existing.sumberDana,
                        kondisi = if (kondisi.isNotBlank()) kondisi.trim() else existing.kondisi,
                        keterangan = if (keterangan.isNotBlank()) keterangan.trim() else existing.keterangan,
                        type = "BAHAN",
                        isBorrowable = isBorrowable
                    )
                    repository.updateItem(updatedItemEntity)
                    firebaseService.saveItemToFirestore(updatedItemEntity)

                    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                    val currentDate = sdfDate.format(Date())
                    val currentTime = sdfTime.format(Date())
                    val auditTx = com.example.data.entity.LoanTransactionEntity(
                        idTransaksi = "TX-INP-${System.currentTimeMillis()}",
                        tanggal = currentDate,
                        waktu = currentTime,
                        namaPeminjam = "Penambahan Stok: ${name.trim()}",
                        kelas = "Sistem / Aset",
                        kondisi = kondisi.trim().ifEmpty { "Baik" },
                        namaPetugas = defaultOfficer.value.ifBlank { "Administrator" },
                        status = "Aset Baru",
                        tanggalKembali = currentDate,
                        waktuKembali = currentTime,
                        kondisiKembali = kondisi.trim().ifEmpty { "Baik" },
                        petugasKembali = defaultOfficer.value.ifBlank { "Administrator" },
                        keteranganKerusakan = "Stok bahan bertambah $stokAwal $satuan. Total stok awal sekarang: $newStokAwal."
                    )
                    db.inventoryDao().insertTransaction(auditTx)
                    getAllBahan()
                    onSuccess()
                    return@launch
                }

                var maxIdNum = 0
                currentItems.forEach { item ->
                    if (item.idBarang.startsWith("BRG-")) {
                        val numPart = item.idBarang.substringAfter("BRG-").toIntOrNull()
                        if (numPart != null && numPart > maxIdNum) {
                            maxIdNum = numPart
                        }
                    }
                }
                val nextNum = maxIdNum + 1
                val idBarang = "BRG-${String.format(Locale.US, "%03d", nextNum)}"

                val itemEntity = ItemEntity(
                    idBarang = idBarang,
                    namaBarang = name.trim(),
                    stokAwal = stokAwal,
                    kategori = kategori.trim(),
                    satuan = satuan.trim(),
                    merekAlat = merekAlat.trim(),
                    ruang = ruang.trim(),
                    sumberDana = sumberDana,
                    kondisi = kondisi.trim(),
                    keterangan = keterangan.trim(),
                    type = "BAHAN",
                    isBorrowable = isBorrowable
                )

                repository.insertItem(
                    id = idBarang,
                    name = name.trim(),
                    stokAwal = stokAwal,
                    kategori = kategori.trim(),
                    satuan = satuan.trim(),
                    merekAlat = merekAlat.trim(),
                    ruang = ruang.trim(),
                    sumberDana = sumberDana,
                    kondisi = kondisi.trim(),
                    keterangan = keterangan.trim(),
                    type = "BAHAN",
                    isBorrowable = isBorrowable
                )

                firebaseService.saveItemToFirestore(itemEntity)

                // Log to loan_transactions for Stock Management
                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                val currentDate = sdfDate.format(Date())
                val currentTime = sdfTime.format(Date())
                val auditTx = com.example.data.entity.LoanTransactionEntity(
                    idTransaksi = "TX-INP-${System.currentTimeMillis()}",
                    tanggal = currentDate,
                    waktu = currentTime,
                    namaPeminjam = "Input Baru: ${name.trim()}",
                    kelas = "Sistem / Aset",
                    kondisi = kondisi.trim().ifEmpty { "Baik" },
                    namaPetugas = defaultOfficer.value.ifBlank { "Administrator" },
                    status = "Aset Baru",
                    tanggalKembali = currentDate,
                    waktuKembali = currentTime,
                    kondisiKembali = kondisi.trim().ifEmpty { "Baik" },
                    petugasKembali = defaultOfficer.value.ifBlank { "Administrator" },
                    keteranganKerusakan = "Barang baru didaftarkan ke sistem dengan stok awal $stokAwal $satuan di ruang $ruang."
                )
                db.inventoryDao().insertTransaction(auditTx)

                // Sync with sheets in background if configured and auto-sync is on
                val webAppUrl = sheetsUrl.value
                if (webAppUrl.isNotEmpty() && autoSyncEnabled.value) {
                    launch {
                        try {
                            val list = db.inventoryDao().getAllItems().first()
                            val success = syncService.pushItems(webAppUrl, list)
                            if (success) {
                                val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
                                settingsRepository.setLastSyncTime(sdf.format(Date()))
                                _lastSyncTime.value = settingsRepository.getLastSyncTime()
                            }
                        } catch (e: Exception) {
                            Log.e("InventoryVM", "Auto sync item registration failed", e)
                        }
                    }
                }

                // Force UI Update / Reactive Refresh: pemanggilan ulang fungsi pengambil data (getAllBahan)
                getAllBahan()

                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal mendaftarkan barang")
            }
        }
    }

    fun registerNewItem(
        name: String,
        stokAwal: Int,
        kategori: String = "",
        satuan: String = "",
        merekAlat: String = "",
        ruang: String = "",
        sumberDana: String? = null,
        kondisi: String = "",
        keterangan: String = "",
        isBorrowable: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    onError("Nama barang tidak boleh kosong!")
                    return@launch
                }
                if (stokAwal < 1) {
                    onError("Stok awal minimal harus diisi angka 1!")
                    return@launch
                }

                val currentItems = itemsWithStock.value
                val existing = currentItems.find { it.namaBarang.equals(name.trim(), ignoreCase = true) }
                if (existing != null) {
                    val newStokAwal = existing.stokAwal + stokAwal
                    val updatedItemEntity = ItemEntity(
                        idBarang = existing.idBarang,
                        namaBarang = existing.namaBarang,
                        stokAwal = newStokAwal,
                        kategori = if (kategori.isNotBlank()) kategori.trim() else existing.kategori,
                        satuan = if (satuan.isNotBlank()) satuan.trim() else existing.satuan,
                        stokRusak = existing.stokRusak,
                        merekAlat = if (merekAlat.isNotBlank()) merekAlat.trim() else existing.merekAlat,
                        ruang = if (ruang.isNotBlank()) ruang.trim() else existing.ruang,
                        sumberDana = sumberDana ?: existing.sumberDana,
                        kondisi = if (kondisi.isNotBlank()) kondisi.trim() else existing.kondisi,
                        keterangan = if (keterangan.isNotBlank()) keterangan.trim() else existing.keterangan,
                        type = "ALAT",
                        isBorrowable = isBorrowable
                    )
                    repository.updateItem(updatedItemEntity)
                    firebaseService.saveItemToFirestore(updatedItemEntity)

                    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                    val currentDate = sdfDate.format(Date())
                    val currentTime = sdfTime.format(Date())
                    val auditTx = com.example.data.entity.LoanTransactionEntity(
                        idTransaksi = "TX-INP-${System.currentTimeMillis()}",
                        tanggal = currentDate,
                        waktu = currentTime,
                        namaPeminjam = "Penambahan Stok: ${name.trim()}",
                        kelas = "Sistem / Aset",
                        kondisi = kondisi.trim().ifEmpty { "Baik" },
                        namaPetugas = defaultOfficer.value.ifBlank { "Administrator" },
                        status = "Aset Baru",
                        tanggalKembali = currentDate,
                        waktuKembali = currentTime,
                        kondisiKembali = kondisi.trim().ifEmpty { "Baik" },
                        petugasKembali = defaultOfficer.value.ifBlank { "Administrator" },
                        keteranganKerusakan = "Stok alat bertambah $stokAwal $satuan. Total stok awal sekarang: $newStokAwal."
                    )
                    db.inventoryDao().insertTransaction(auditTx)
                    onSuccess()
                    return@launch
                }

                var maxIdNum = 0
                currentItems.forEach { item ->
                    if (item.idBarang.startsWith("BRG-")) {
                        val numPart = item.idBarang.substringAfter("BRG-").toIntOrNull()
                        if (numPart != null && numPart > maxIdNum) {
                            maxIdNum = numPart
                        }
                    }
                }
                val nextNum = maxIdNum + 1
                val idBarang = "BRG-${String.format(Locale.US, "%03d", nextNum)}"

                val itemEntity = ItemEntity(
                    idBarang = idBarang,
                    namaBarang = name.trim(),
                    stokAwal = stokAwal,
                    kategori = kategori.trim(),
                    satuan = satuan.trim(),
                    merekAlat = merekAlat.trim(),
                    ruang = ruang.trim(),
                    sumberDana = sumberDana,
                    kondisi = kondisi.trim(),
                    keterangan = keterangan.trim(),
                    type = "ALAT",
                    isBorrowable = isBorrowable
                )

                repository.insertItem(
                    id = idBarang,
                    name = name.trim(),
                    stokAwal = stokAwal,
                    kategori = kategori.trim(),
                    satuan = satuan.trim(),
                    merekAlat = merekAlat.trim(),
                    ruang = ruang.trim(),
                    sumberDana = sumberDana,
                    kondisi = kondisi.trim(),
                    keterangan = keterangan.trim(),
                    type = "ALAT",
                    isBorrowable = isBorrowable
                )

                firebaseService.saveItemToFirestore(itemEntity)

                // Log to loan_transactions for Stock Management
                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                val currentDate = sdfDate.format(Date())
                val currentTime = sdfTime.format(Date())
                val auditTx = com.example.data.entity.LoanTransactionEntity(
                    idTransaksi = "TX-INP-${System.currentTimeMillis()}",
                    tanggal = currentDate,
                    waktu = currentTime,
                    namaPeminjam = "Input Baru: ${name.trim()}",
                    kelas = "Sistem / Aset",
                    kondisi = kondisi.trim().ifEmpty { "Baik" },
                    namaPetugas = defaultOfficer.value.ifBlank { "Administrator" },
                    status = "Aset Baru",
                    tanggalKembali = currentDate,
                    waktuKembali = currentTime,
                    kondisiKembali = kondisi.trim().ifEmpty { "Baik" },
                    petugasKembali = defaultOfficer.value.ifBlank { "Administrator" },
                    keteranganKerusakan = "Barang baru didaftarkan ke sistem dengan stok awal $stokAwal $satuan di ruang $ruang."
                )
                db.inventoryDao().insertTransaction(auditTx)

                // Sync with sheets in background if configured and auto-sync is on
                val webAppUrl = sheetsUrl.value
                if (webAppUrl.isNotEmpty() && autoSyncEnabled.value) {
                    launch {
                        try {
                            val list = db.inventoryDao().getAllItems().first()
                            val success = syncService.pushItems(webAppUrl, list)
                            if (success) {
                                val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
                                settingsRepository.setLastSyncTime(sdf.format(Date()))
                                _lastSyncTime.value = settingsRepository.getLastSyncTime()
                            }
                        } catch (e: Exception) {
                            Log.e("InventoryVM", "Auto sync item registration failed", e)
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal mendaftarkan barang")
            }
        }
    }

    fun recordPemakaian(
        idBarang: String,
        namaBarang: String,
        jumlahDiambil: Int,
        satuan: String,
        namaPeminta: String,
        jabatan: String,
        kelas: String?,
        namaPetugas: String,
        tanggalPemakaian: String,
        keterangan: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (idBarang.isBlank()) {
                    onError("Barang harus dipilih!")
                    return@launch
                }
                if (jumlahDiambil <= 0) {
                    onError("Jumlah diambil harus lebih besar dari 0!")
                    return@launch
                }
                if (namaPeminta.isBlank()) {
                    onError("Nama peminta tidak boleh kosong!")
                    return@launch
                }
                if (namaPetugas.isBlank()) {
                    onError("Nama petugas tidak boleh kosong!")
                    return@launch
                }

                // Stock Validation for Consumable (BAHAN)
                val existingItem = itemsWithStock.value.find { it.idBarang == idBarang }
                if (existingItem == null) {
                    onError("Bahan '$namaBarang' tidak ditemukan di database master!")
                    return@launch
                }
                if (existingItem.stokTersedia < jumlahDiambil) {
                    onError("Stok bahan '${existingItem.namaBarang}' tidak mencukupi! Stok tersedia: ${existingItem.stokTersedia} ${existingItem.satuan}, diminta: $jumlahDiambil")
                    return@launch
                }

                val currentPemakaian = allPemakaianBahan.value
                var maxIdNum = 0
                currentPemakaian.forEach { pmk ->
                    if (pmk.idPemakaian.startsWith("PMK-")) {
                        val numPart = pmk.idPemakaian.substringAfter("PMK-").toIntOrNull()
                        if (numPart != null && numPart > maxIdNum) {
                            maxIdNum = numPart
                        }
                    }
                }
                val nextNum = maxIdNum + 1
                val idPemakaian = "PMK-${String.format(Locale.US, "%03d", nextNum)}"

                val newPemakaian = com.example.data.entity.PemakaianBahanEntity(
                    idPemakaian = idPemakaian,
                    idBarang = idBarang,
                    namaBarang = namaBarang,
                    jumlahDiambil = jumlahDiambil,
                    satuan = satuan,
                    namaPeminta = namaPeminta.trim(),
                    jabatan = jabatan.trim(),
                    kelas = if (kelas.isNullOrBlank()) null else kelas.trim(),
                    namaPetugas = namaPetugas.trim(),
                    tanggalPemakaian = tanggalPemakaian,
                    keterangan = keterangan.trim()
                )

                repository.recordPemakaian(newPemakaian)

                // Sync updated stock to Firestore
                val updatedStokAwal = (existingItem.stokAwal - jumlahDiambil).coerceAtLeast(0)
                val updatedItemEntity = ItemEntity(
                    idBarang = existingItem.idBarang,
                    namaBarang = existingItem.namaBarang,
                    stokAwal = updatedStokAwal,
                    kategori = existingItem.kategori,
                    satuan = existingItem.satuan,
                    stokRusak = existingItem.stokRusak,
                    merekAlat = existingItem.merekAlat,
                    ruang = existingItem.ruang,
                    sumberDana = existingItem.sumberDana,
                    kondisi = existingItem.kondisi,
                    keterangan = existingItem.keterangan,
                    isDemo = existingItem.isDemo,
                    type = existingItem.type,
                    isBorrowable = existingItem.isBorrowable
                )
                firebaseService.saveItemToFirestore(updatedItemEntity)

                // Log to loan_transactions for Unified Transaction Audit & Reports
                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                val currentDate = sdfDate.format(Date())
                val currentTime = sdfTime.format(Date())
                val auditTx = com.example.data.entity.LoanTransactionEntity(
                    idTransaksi = "TX-PMK-${System.currentTimeMillis()}",
                    tanggal = if (tanggalPemakaian.isNotBlank()) tanggalPemakaian else currentDate,
                    waktu = currentTime,
                    namaPeminjam = "Pemakaian: ${namaPeminta.trim()}" + if (!kelas.isNullOrBlank()) " ($kelas)" else "",
                    kelas = if (!kelas.isNullOrBlank()) kelas.trim() else "Pemakaian Bahan",
                    kondisi = "Habis Pakai",
                    namaPetugas = namaPetugas.trim(),
                    status = "Pemakaian Bahan",
                    tanggalKembali = if (tanggalPemakaian.isNotBlank()) tanggalPemakaian else currentDate,
                    waktuKembali = currentTime,
                    kondisiKembali = "Habis Pakai",
                    petugasKembali = namaPetugas.trim(),
                    keteranganKerusakan = "Pemakaian bahan $namaBarang sebanyak $jumlahDiambil $satuan oleh ${namaPeminta.trim()} ($jabatan)."
                )
                db.inventoryDao().insertTransaction(auditTx)
                firebaseService.saveTransactionToFirestore(auditTx)

                onSuccess()
            } catch (e: Exception) {
                onError("Gagal merekam pemakaian: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun recordBahanAfkir(
        idBarang: String,
        namaBarang: String,
        jumlahAfkir: Int,
        satuan: String,
        alasan: String,
        tanggalAfkir: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (idBarang.isBlank()) {
                    onError("Bahan harus dipilih!")
                    return@launch
                }
                if (jumlahAfkir <= 0) {
                    onError("Jumlah afkir harus lebih besar dari 0!")
                    return@launch
                }
                if (alasan.isBlank()) {
                    onError("Alasan tidak boleh kosong!")
                    return@launch
                }

                // Stock Validation
                val existingItem = itemsWithStock.value.find { it.idBarang == idBarang }
                if (existingItem != null && existingItem.stokTersedia < jumlahAfkir) {
                    onError("Stok bahan tidak mencukupi! Tersedia: ${existingItem.stokTersedia} ${existingItem.satuan}")
                    return@launch
                }

                val currentAfkir = allBahanAfkir.value
                var maxIdNum = 0
                currentAfkir.forEach { afk ->
                    if (afk.idAfkir.startsWith("AFK-")) {
                        val numPart = afk.idAfkir.substringAfter("AFK-").toIntOrNull()
                        if (numPart != null && numPart > maxIdNum) {
                            maxIdNum = numPart
                        }
                    }
                }
                val nextNum = maxIdNum + 1
                val idAfkir = "AFK-${String.format(Locale.US, "%03d", nextNum)}"

                val newAfkir = com.example.data.entity.BahanAfkirEntity(
                    idAfkir = idAfkir,
                    idBarang = idBarang,
                    namaBarang = namaBarang,
                    jumlahAfkir = jumlahAfkir,
                    satuan = satuan,
                    alasan = alasan.trim(),
                    tanggalAfkir = tanggalAfkir
                )

                repository.recordBahanAfkir(newAfkir)

                if (existingItem != null) {
                    val updatedStokAwal = (existingItem.stokAwal - jumlahAfkir).coerceAtLeast(0)
                    val updatedItemEntity = ItemEntity(
                        idBarang = existingItem.idBarang,
                        namaBarang = existingItem.namaBarang,
                        stokAwal = updatedStokAwal,
                        kategori = existingItem.kategori,
                        satuan = existingItem.satuan,
                        stokRusak = existingItem.stokRusak,
                        merekAlat = existingItem.merekAlat,
                        ruang = existingItem.ruang,
                        sumberDana = existingItem.sumberDana,
                        kondisi = existingItem.kondisi,
                        keterangan = existingItem.keterangan,
                        isDemo = existingItem.isDemo,
                        type = existingItem.type,
                        isBorrowable = existingItem.isBorrowable
                    )
                    firebaseService.saveItemToFirestore(updatedItemEntity)
                }

                // Log to loan_transactions for Condition & Maintenance
                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                val currentDate = sdfDate.format(Date())
                val currentTime = sdfTime.format(Date())
                val auditTx = com.example.data.entity.LoanTransactionEntity(
                    idTransaksi = "TX-AFK-${System.currentTimeMillis()}",
                    tanggal = currentDate,
                    waktu = currentTime,
                    namaPeminjam = "Bahan Afkir: $namaBarang",
                    kelas = "Kondisi & Pemeliharaan",
                    kondisi = "Afkir",
                    namaPetugas = defaultOfficer.value.ifBlank { "Administrator" },
                    status = "Afkir",
                    tanggalKembali = currentDate,
                    waktuKembali = currentTime,
                    kondisiKembali = "Afkir",
                    petugasKembali = defaultOfficer.value.ifBlank { "Administrator" },
                    keteranganKerusakan = "Pencatatan bahan afkir sebanyak $jumlahAfkir $satuan. Alasan: $alasan"
                )
                db.inventoryDao().insertTransaction(auditTx)

                onSuccess()
            } catch (e: Exception) {
                onError("Gagal mencatat bahan afkir: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun undoBahanAfkir(
        idAfkir: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.undoBahanAfkir(idAfkir)
                onSuccess()
            } catch (e: Exception) {
                onError("Gagal membatalkan afkir: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun deleteBahanAfkirPermanently(
        idAfkir: String,
        namaPetugas: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("id", "ID"))
                val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID"))
                val currentDate = sdfDate.format(java.util.Date())
                val currentTime = sdfTime.format(java.util.Date())

                repository.deleteBahanAfkirPermanently(
                    idAfkir = idAfkir,
                    currentDate = currentDate,
                    currentTime = currentTime,
                    namaPetugas = if (namaPetugas.isBlank()) "Administrator" else namaPetugas.trim()
                )
                onSuccess()
            } catch (e: Exception) {
                onError("Gagal menghapus secara permanen: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun recordDamagedReport(
        idBarang: String,
        namaBarang: String,
        jumlah: Int,
        tanggalKerusakan: String,
        waktuKerusakan: String,
        keteranganKerusakan: String,
        namaPetugas: String,
        kondisiBaru: String,
        status: String = "Rusak (Perlu Tindakan)",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (idBarang.isBlank()) {
                    onError("Alat harus dipilih!")
                    return@launch
                }
                if (jumlah <= 0) {
                    onError("Jumlah rusak harus lebih besar dari 0!")
                    return@launch
                }
                if (namaPetugas.isBlank()) {
                    onError("Nama petugas tidak boleh kosong!")
                    return@launch
                }
                if (keteranganKerusakan.isBlank()) {
                    onError("Keterangan/Kronologi tidak boleh kosong!")
                    return@launch
                }

                // Stock Validation
                val existingItem = itemsWithStock.value.find { it.idBarang == idBarang }
                if (existingItem != null && existingItem.stokTersedia < jumlah) {
                    onError("Stok tidak mencukupi! Tersedia: ${existingItem.stokTersedia} ${existingItem.satuan}")
                    return@launch
                }

                val newReport = com.example.data.entity.DamagedItemEntity(
                    idBarang = idBarang,
                    namaBarang = namaBarang,
                    jumlah = jumlah,
                    tanggalKerusakan = tanggalKerusakan,
                    waktuKerusakan = waktuKerusakan,
                    keteranganKerusakan = keteranganKerusakan.trim(),
                    namaPetugas = namaPetugas.trim(),
                    kondisiBaru = kondisiBaru,
                    status = status
                )

                repository.recordDamagedReport(newReport)

                // Log to loan_transactions for Condition & Maintenance
                val isMaint = status.contains("Servis") || status.contains("Pemeliharaan") || kondisiBaru.contains("Perbaikan")
                val auditTx = com.example.data.entity.LoanTransactionEntity(
                    idTransaksi = "TX-DMG-${System.currentTimeMillis()}",
                    tanggal = tanggalKerusakan,
                    waktu = waktuKerusakan,
                    namaPeminjam = if (isMaint) "Sedang Servis: $namaBarang" else "Barang Rusak: $namaBarang",
                    kelas = "Kondisi & Pemeliharaan",
                    kondisi = kondisiBaru,
                    namaPetugas = namaPetugas,
                    status = if (isMaint) "Servis" else "Rusak",
                    tanggalKembali = tanggalKerusakan,
                    waktuKembali = waktuKerusakan,
                    kondisiKembali = kondisiBaru,
                    petugasKembali = namaPetugas,
                    keteranganKerusakan = "Pencatatan $status sebanyak $jumlah unit. Keterangan: $keteranganKerusakan"
                )
                db.inventoryDao().insertTransaction(auditTx)

                onSuccess()
            } catch (e: Exception) {
                onError("Gagal mencatat laporan kerusakan: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun cancelDamagedReport(
        id: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.cancelDamagedReport(id)
                onSuccess()
            } catch (e: Exception) {
                onError("Gagal menghapus laporan: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun updateDamagedStatus(
        damagedId: Int,
        newStatus: String,
        alasan: String,
        namaPetugas: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (alasan.isBlank()) {
                    onError("Alasan perubahan status tidak boleh kosong!")
                    return@launch
                }

                val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("id", "ID"))
                val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID"))
                val currentDate = sdfDate.format(java.util.Date())
                val currentTime = sdfTime.format(java.util.Date())

                repository.updateDamagedStatus(
                    damagedId = damagedId,
                    newStatus = newStatus,
                    alasan = alasan.trim(),
                    namaPetugas = if (namaPetugas.isBlank()) "Administrator" else namaPetugas.trim(),
                    currentDate = currentDate,
                    currentTime = currentTime
                )
                onSuccess()
            } catch (e: Exception) {
                onError("Gagal mengubah status: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun deleteDamagedItemPermanently(
        id: Int,
        namaPetugas: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale("id", "ID"))
                val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID"))
                val currentDate = sdfDate.format(java.util.Date())
                val currentTime = sdfTime.format(java.util.Date())

                repository.deleteDamagedItemPermanently(
                    id = id,
                    currentDate = currentDate,
                    currentTime = currentTime,
                    namaPetugas = if (namaPetugas.isBlank()) "Administrator" else namaPetugas.trim()
                )
                onSuccess()
            } catch (e: Exception) {
                onError("Gagal menghapus secara permanen: ${e.localizedMessage ?: "Terjadi kesalahan"}")
            }
        }
    }

    fun updateStock(idBarang: String, namaBarang: String, stokAwal: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (stokAwal < 0) {
                    onError("Stok awal tidak boleh kurang dari 0!")
                    return@launch
                }
                val existing = itemsWithStock.value.find { it.idBarang == idBarang }
                val kat = existing?.kategori ?: ""
                val sat = existing?.satuan ?: ""
                val item = ItemEntity(idBarang, namaBarang, stokAwal, kat, sat)
                repository.updateItem(item)

                // Log to loan_transactions for Stock Management
                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                val currentDate = sdfDate.format(Date())
                val currentTime = sdfTime.format(Date())
                val auditTx = com.example.data.entity.LoanTransactionEntity(
                    idTransaksi = "TX-OPN-${System.currentTimeMillis()}",
                    tanggal = currentDate,
                    waktu = currentTime,
                    namaPeminjam = "Stock Opname: $namaBarang",
                    kelas = "Sistem / Aset",
                    kondisi = "Normal",
                    namaPetugas = defaultOfficer.value.ifBlank { "Administrator" },
                    status = "Stock Opname",
                    tanggalKembali = currentDate,
                    waktuKembali = currentTime,
                    kondisiKembali = "Normal",
                    petugasKembali = defaultOfficer.value.ifBlank { "Administrator" },
                    keteranganKerusakan = "Penyesuaian stok awal melalui Stock Opname dari ${existing?.stokAwal ?: 0} menjadi $stokAwal."
                )
                db.inventoryDao().insertTransaction(auditTx)

                // Sync in background
                val webAppUrl = sheetsUrl.value
                if (webAppUrl.isNotEmpty() && autoSyncEnabled.value) {
                    launch {
                        try {
                            val list = db.inventoryDao().getAllItems().first()
                            val success = syncService.pushItems(webAppUrl, list)
                            if (success) {
                                val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
                                settingsRepository.setLastSyncTime(sdf.format(Date()))
                                _lastSyncTime.value = settingsRepository.getLastSyncTime()
                            }
                        } catch (e: Exception) {
                            Log.e("InventoryVM", "Auto sync stock update failed", e)
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal memperbarui stok")
            }
        }
    }

    fun updateItemDetails(
        idBarang: String,
        namaBarang: String,
        kategori: String,
        satuan: String,
        stokAwal: Int,
        merekAlat: String = "",
        ruang: String = "",
        sumberDana: String? = null,
        kondisi: String = "",
        keterangan: String = "",
        isBorrowable: Boolean? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (stokAwal < 1) {
                    onError("Stok awal minimal harus diisi angka 1!")
                    return@launch
                }
                val existing = itemsWithStock.value.find { it.idBarang == idBarang }
                val finalIsBorrowable = isBorrowable ?: existing?.isBorrowable ?: false
                val item = ItemEntity(
                    idBarang = idBarang,
                    namaBarang = namaBarang,
                    stokAwal = stokAwal,
                    kategori = kategori,
                    satuan = satuan,
                    stokRusak = existing?.stokRusak ?: 0,
                    merekAlat = merekAlat,
                    ruang = ruang,
                    sumberDana = sumberDana,
                    kondisi = kondisi,
                    keterangan = keterangan,
                    type = existing?.type ?: "ALAT",
                    isBorrowable = finalIsBorrowable
                )
                repository.updateItem(item)
                firebaseService.saveItemToFirestore(item)

                // Log to loan_transactions for Stock Management (Room Transfer)
                if (existing != null && existing.ruang != ruang) {
                    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                    val currentDate = sdfDate.format(Date())
                    val currentTime = sdfTime.format(Date())
                    val auditTx = com.example.data.entity.LoanTransactionEntity(
                        idTransaksi = "TX-RUM-${System.currentTimeMillis()}",
                        tanggal = currentDate,
                        waktu = currentTime,
                        namaPeminjam = "Pindah Ruangan: $namaBarang",
                        kelas = "Sistem / Aset",
                        kondisi = kondisi,
                        namaPetugas = defaultOfficer.value.ifBlank { "Administrator" },
                        status = "Pindah Ruangan",
                        tanggalKembali = currentDate,
                        waktuKembali = currentTime,
                        kondisiKembali = kondisi,
                        petugasKembali = defaultOfficer.value.ifBlank { "Administrator" },
                        keteranganKerusakan = "Aset dipindahkan dari ruang '${existing.ruang}' ke '$ruang'."
                    )
                    db.inventoryDao().insertTransaction(auditTx)
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal memperbarui data barang")
            }
        }
    }

    fun deleteItem(idBarang: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val activeCount = repository.getActiveLoanCountForItem(idBarang)
                if (activeCount > 0) {
                    onError("Barang tidak bisa dihapus karena masih aktif dipinjam!")
                    return@launch
                }
                repository.deleteItemById(idBarang)
                firebaseService.deleteItemFromFirestore(idBarang)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal menghapus barang")
            }
        }
    }

    // Categories CRUD
    fun addCategory(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    onError("Nama kategori tidak boleh kosong!")
                    return@launch
                }
                repository.insertCategory(name.trim())
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal menambah kategori")
            }
        }
    }

    fun updateCategory(id: Int, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    onError("Nama kategori tidak boleh kosong!")
                    return@launch
                }
                repository.updateCategory(com.example.data.entity.CategoryEntity(id = id, name = name.trim()))
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal memperbarui kategori")
            }
        }
    }

    fun deleteCategory(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteCategoryById(id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal menghapus kategori")
            }
        }
    }

    // Units CRUD
    fun addUnit(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    onError("Nama satuan tidak boleh kosong!")
                    return@launch
                }
                repository.insertUnit(name.trim())
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal menambah satuan")
            }
        }
    }

    fun updateUnit(id: Int, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    onError("Nama satuan tidak boleh kosong!")
                    return@launch
                }
                repository.updateUnit(com.example.data.entity.UnitEntity(id = id, name = name.trim()))
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal memperbarui satuan")
            }
        }
    }

    fun deleteUnit(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteUnitById(id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal menghapus satuan")
            }
        }
    }

    fun submitLoan(
        namaPeminjam: String,
        kelas: String,
        kondisi: String,
        namaPetugas: String,
        tanggal: String,
        waktu: String,
        whatsappNumber: String? = null,
        itemsToBorrow: List<Pair<String, Int>>,
        durasiHari: Int = 1,
        tujuanPeminjaman: String? = null,
        detailTujuan: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (namaPeminjam.isBlank()) {
                    onError("Nama peminjam harus diisi!")
                    return@launch
                }
                if (kelas.isBlank()) {
                    onError("Kelas harus diisi!")
                    return@launch
                }
                if (namaPetugas.isBlank()) {
                    onError("Nama petugas harus diisi!")
                    return@launch
                }
                if (itemsToBorrow.isEmpty()) {
                    onError("Harus ada minimal 1 barang yang dipinjam!")
                    return@launch
                }

                val idTransaksi = "TX-${System.currentTimeMillis()}-${(100..999).random()}"
                val loanItems = mutableListOf<LoanItemEntity>()
                val itemsToRegister = mutableListOf<ItemEntity>()

                // 1. Normalize and combine duplicate item requests into a single line entry per item ID/Name
                val mergedItemMap = LinkedHashMap<String, Int>() // Key: canonical identifier, Value: combined total qty
                val displayNamesMap = HashMap<String, String>()

                for (line in itemsToBorrow) {
                    val inputKey = line.first.trim()
                    val qty = line.second
                    if (inputKey.isBlank() || qty <= 0) continue

                    val matched = itemsWithStock.value.find {
                        it.idBarang.equals(inputKey, ignoreCase = true) || it.namaBarang.equals(inputKey, ignoreCase = true)
                    }

                    val canonicalId = matched?.idBarang ?: inputKey
                    val displayName = matched?.namaBarang ?: inputKey
                    displayNamesMap[canonicalId] = displayName
                    mergedItemMap[canonicalId] = (mergedItemMap[canonicalId] ?: 0) + qty
                }

                if (mergedItemMap.isEmpty()) {
                    onError("Harus ada minimal 1 barang valid yang dipinjam!")
                    return@launch
                }

                // 2. Validate combined quantity against available stock (stokTersedia) for ALAT
                for ((canonicalId, totalQty) in mergedItemMap) {
                    val displayName = displayNamesMap[canonicalId] ?: canonicalId
                    val matchedItem = itemsWithStock.value.find {
                        it.idBarang.equals(canonicalId, ignoreCase = true) || it.namaBarang.equals(displayName, ignoreCase = true)
                    }

                    if (matchedItem != null) {
                        val availableStock = matchedItem.stokTersedia
                        if (totalQty > availableStock) {
                            onError("Stok '${matchedItem.namaBarang}' tidak mencukupi! Stok tersedia: $availableStock ${matchedItem.satuan}, total diminta: $totalQty.")
                            return@launch
                        }

                        loanItems.add(
                            LoanItemEntity(
                                idTransaksi = idTransaksi,
                                idBarang = matchedItem.idBarang,
                                namaBarang = matchedItem.namaBarang,
                                jumlah = totalQty
                            )
                        )
                    } else {
                        // Register item on-the-fly if missing from database
                        val currentItems = itemsWithStock.value
                        var maxIdNum = 0
                        currentItems.forEach { item ->
                            if (item.idBarang.startsWith("BRG-")) {
                                val numPart = item.idBarang.substringAfter("BRG-").toIntOrNull()
                                if (numPart != null && numPart > maxIdNum) {
                                    maxIdNum = numPart
                                }
                            }
                        }
                        val nextNum = maxIdNum + 1
                        val newId = "BRG-${String.format(Locale.US, "%03d", nextNum)}"

                        val newItem = ItemEntity(
                            idBarang = newId,
                            namaBarang = displayName,
                            stokAwal = totalQty // Physical stock created equal to borrowed amount
                        )
                        itemsToRegister.add(newItem)

                        loanItems.add(
                            LoanItemEntity(
                                idTransaksi = idTransaksi,
                                idBarang = newId,
                                namaBarang = displayName,
                                jumlah = totalQty
                            )
                        )
                    }
                }

                // Register missing items in Room
                itemsToRegister.forEach { item ->
                    db.inventoryDao().insertItem(item)
                    firebaseService.saveItemToFirestore(item)
                }

                val transaction = LoanTransactionEntity(
                    idTransaksi = idTransaksi,
                    tanggal = tanggal,
                    namaPeminjam = namaPeminjam.trim(),
                    kelas = kelas.trim(),
                    waktu = waktu,
                    kondisi = kondisi,
                    namaPetugas = namaPetugas.trim(),
                    status = "Dipinjam",
                    whatsappNumber = whatsappNumber?.trim(),
                    durasiHari = durasiHari,
                    tujuanPeminjaman = tujuanPeminjaman?.trim(),
                    detailTujuan = detailTujuan?.trim()
                )

                val success = repository.createLoan(transaction, loanItems, settingsRepository)
                if (success) {
                    _lastSyncTime.value = settingsRepository.getLastSyncTime()
                    
                    // Write atomically to Firestore (Transaction & Loan Items WITHOUT modifying master physical stock)
                    val dbFirestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    dbFirestore.runTransaction { transactionAccessor ->
                        // 1. Write transaction to 'transactions' collection
                        val txRef = dbFirestore.collection("transactions").document(idTransaksi)
                        val txData = hashMapOf<String, Any>(
                            "idTransaksi" to transaction.idTransaksi,
                            "tanggal" to transaction.tanggal,
                            "namaPeminjam" to transaction.namaPeminjam,
                            "kelas" to transaction.kelas,
                            "waktu" to transaction.waktu,
                            "kondisi" to transaction.kondisi,
                            "namaPetugas" to transaction.namaPetugas,
                            "status" to transaction.status,
                            "durasiHari" to transaction.durasiHari,
                            "isDemo" to (if (transaction.isDemo) 1 else 0)
                        )
                        transaction.whatsappNumber?.let { txData["whatsappNumber"] = it }
                        transaction.tujuanPeminjaman?.let { txData["tujuanPeminjaman"] = it }
                        transaction.detailTujuan?.let { txData["detailTujuan"] = it }
                        transactionAccessor.set(txRef, txData)

                        // 2. Write loan items to 'loan_items' collection
                        loanItems.forEach { loanItem ->
                            val docId = "${loanItem.idTransaksi}_${loanItem.idBarang}"
                            val itemData = hashMapOf<String, Any>(
                                "id" to loanItem.id,
                                "idTransaksi" to loanItem.idTransaksi,
                                "idBarang" to loanItem.idBarang,
                                "namaBarang" to loanItem.namaBarang,
                                "jumlah" to loanItem.jumlah,
                                "isDemo" to loanItem.isDemo
                            )
                            val loanItemRef = dbFirestore.collection("loan_items").document(docId)
                            transactionAccessor.set(loanItemRef, itemData)
                        }
                    }.addOnSuccessListener {
                        android.util.Log.d("InventoryViewModel", "Firestore atomic submitLoan success.")
                    }.addOnFailureListener { e ->
                        android.util.Log.e("InventoryViewModel", "Firestore atomic submitLoan failed", e)
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Terjadi kesalahan saat menyimpan peminjaman")
            }
        }
    }

    fun submitReturn(
        idTransaksi: String,
        kondisiKembali: String,
        namaPetugas: String,
        tanggalKembali: String,
        waktuKembali: String,
        keteranganKerusakan: String? = null,
        itemConditions: Map<String, String> = emptyMap(),
        itemDamagedCounts: Map<String, Int> = emptyMap(),
        itemNotes: Map<String, String> = emptyMap(),
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (idTransaksi.isBlank()) {
                    onError("Transaksi belum dipilih!")
                    return@launch
                }
                if (namaPetugas.isBlank()) {
                    onError("Nama petugas penerima harus diisi!")
                    return@launch
                }

                val success = repository.returnLoan(
                    idTransaksi = idTransaksi,
                    tanggalKembali = tanggalKembali,
                    waktuKembali = waktuKembali,
                    kondisiKembali = kondisiKembali,
                    petugasKembali = namaPetugas.trim(),
                    keteranganKerusakan = keteranganKerusakan,
                    itemConditions = itemConditions,
                    itemDamagedCounts = itemDamagedCounts,
                    itemNotes = itemNotes,
                    settingsRepo = settingsRepository
                )

                if (success) {
                    _lastSyncTime.value = settingsRepository.getLastSyncTime()
                    val updatedTx = db.inventoryDao().getTransactionById(idTransaksi)
                    
                    if (updatedTx != null) {
                        val dbFirestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        dbFirestore.runTransaction { transactionAccessor ->
                            // Update transaction status to 'Kembali' in Firestore
                            val txRef = dbFirestore.collection("transactions").document(idTransaksi)
                            val txData = hashMapOf<String, Any>(
                                "status" to "Kembali",
                                "tanggalKembali" to (updatedTx.tanggalKembali ?: ""),
                                "waktuKembali" to (updatedTx.waktuKembali ?: ""),
                                "kondisiKembali" to (updatedTx.kondisiKembali ?: ""),
                                "petugasKembali" to (updatedTx.petugasKembali ?: "")
                            )
                            updatedTx.keteranganKerusakan?.let { txData["keteranganKerusakan"] = it }
                            transactionAccessor.update(txRef, txData)
                        }.addOnSuccessListener {
                            android.util.Log.d("InventoryViewModel", "Firestore atomic returnLoan success.")
                        }.addOnFailureListener { e ->
                            android.util.Log.e("InventoryViewModel", "Firestore atomic returnLoan failed", e)
                        }
                    }
                    onSuccess()
                } else {
                    onError("Gagal memproses pengembalian. Transaksi tidak valid!")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Terjadi kesalahan saat menyimpan pengembalian")
            }
        }
    }

    fun forceSyncWithSheets() {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            _syncError.value = null
            _syncProgress.value = "Memulai sinkronisasi..."

            try {
                val result = repository.syncWithSheets(settingsRepository) { progress ->
                    _syncProgress.value = progress
                }
                if (result.isSuccess) {
                    _lastSyncTime.value = settingsRepository.getLastSyncTime()
                    
                    // Log to loan_transactions for System Activity
                    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                    val currentDate = sdfDate.format(Date())
                    val currentTime = sdfTime.format(Date())
                    val auditTx = com.example.data.entity.LoanTransactionEntity(
                        idTransaksi = "TX-SYN-${System.currentTimeMillis()}",
                        tanggal = currentDate,
                        waktu = currentTime,
                        namaPeminjam = "Sinkronisasi Google Sheets",
                        kelas = "Aktivitas Sistem",
                        kondisi = "Normal",
                        namaPetugas = defaultOfficer.value.ifBlank { "Administrator" },
                        status = "Sukses",
                        tanggalKembali = currentDate,
                        waktuKembali = currentTime,
                        kondisiKembali = "Normal",
                        petugasKembali = defaultOfficer.value.ifBlank { "Administrator" },
                        keteranganKerusakan = "Sinkronisasi database lokal dengan Google Sheets berhasil dilakukan secara real-time."
                    )
                    db.inventoryDao().insertTransaction(auditTx)
                } else {
                    _syncError.value = result.exceptionOrNull()?.message ?: "Gagal menyinkronkan data."
                }
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Terjadi kesalahan."
            } finally {
                _isSyncing.value = false
            }
        }
    }

    suspend fun getItemsForTransaction(idTransaksi: String): List<LoanItemEntity> {
        return repository.getItemsForTransaction(idTransaksi)
    }

    suspend fun getTransactionDetail(idTransaksi: String): com.example.data.repository.TransactionDetailResult? {
        return repository.getTransactionDetail(idTransaksi)
    }

    fun clearAllTransactionsData(onCompleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllTransactions()
                firebaseService.clearAllTransactionsFromFirestore()
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error clearing all transactions", e)
            } finally {
                onCompleted()
            }
        }
    }

    fun clearAllLocalData(onCompleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                firebaseService.clearAllTransactionsFromFirestore()
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error clearing all local data", e)
            } finally {
                onCompleted()
            }
        }
    }

    fun repairStokRusak(idBarang: String, amount: Int, onCompleted: () -> Unit) {
        viewModelScope.launch {
            repository.repairStokRusak(idBarang, amount)
            onCompleted()
        }
    }

    fun importCsvData(
        csvLines: List<List<String>>, 
        defaultType: String? = null,
        onSuccess: (addedCount: Int, updatedCount: Int) -> Unit, 
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (csvLines.isEmpty()) {
                    onError("File CSV kosong!")
                    return@launch
                }

                // Validation of target menu (Alat vs Bahan) using the header row
                val headerRow = csvLines.firstOrNull()
                val firstHeaderCell = headerRow?.firstOrNull()?.trim()?.lowercase() ?: ""
                
                if (defaultType == "ALAT" && (firstHeaderCell == "nama_bahan" || firstHeaderCell.contains("bahan"))) {
                    onError("Format file tidak sesuai dengan menu tujuan (File CSV Bahan diunggah ke menu Alat).")
                    return@launch
                }
                if (defaultType == "BAHAN" && (firstHeaderCell == "nama_alat" || firstHeaderCell.contains("alat"))) {
                    onError("Format file tidak sesuai dengan menu tujuan (File CSV Alat diunggah ke menu Bahan).")
                    return@launch
                }

                var added = 0
                var updated = 0
                
                val existingItems = itemsWithStock.value.toMutableList()
                
                var maxIdNum = 0
                existingItems.forEach { item ->
                    if (item.idBarang.startsWith("BRG-")) {
                        val numPart = item.idBarang.substringAfter("BRG-").toIntOrNull()
                        if (numPart != null && numPart > maxIdNum) {
                            maxIdNum = numPart
                        }
                    }
                }
                
                csvLines.forEach { row ->
                    if (row.isEmpty()) return@forEach
                    val namaBarang = row.getOrNull(0)?.trim() ?: ""
                    if (namaBarang.isBlank()) return@forEach
                    
                    if (namaBarang.equals("nama_alat", ignoreCase = true) || 
                        namaBarang.equals("nama_bahan", ignoreCase = true) || 
                        namaBarang.equals("Nama Barang", ignoreCase = true) || 
                        namaBarang.equals("Nama_Barang", ignoreCase = true) ||
                        namaBarang.equals("Nama", ignoreCase = true) ||
                        namaBarang.equals("kategori", ignoreCase = true)) {
                        return@forEach
                    }
                    
                    val kategori = row.getOrNull(1)?.trim() ?: ""
                    val merekAlat = row.getOrNull(2)?.trim() ?: ""
                    val ruang = row.getOrNull(3)?.trim() ?: ""
                    val satuan = row.getOrNull(4)?.trim() ?: ""
                    val stokAwalStr = row.getOrNull(5)?.trim() ?: ""
                    var stokAwal = stokAwalStr.toIntOrNull() ?: 1
                    if (stokAwal <= 0) {
                        stokAwal = 1
                    }
                    val sumberDanaRaw = row.getOrNull(6)?.trim()
                    val sumberDana = if (sumberDanaRaw.isNullOrBlank() || sumberDanaRaw.equals("Belum Diketahui / Kosongkan", ignoreCase = true) || sumberDanaRaw.equals("null", ignoreCase = true)) null else sumberDanaRaw
                    val kondisi = row.getOrNull(7)?.trim() ?: "Normal"
                    val keterangan = row.getOrNull(8)?.trim() ?: ""
                    
                    val existingItem = existingItems.find { it.namaBarang.equals(namaBarang, ignoreCase = true) }
                    if (existingItem != null) {
                        val updatedItem = ItemEntity(
                            idBarang = existingItem.idBarang,
                            namaBarang = existingItem.namaBarang,
                            stokAwal = stokAwal,
                            kategori = if (kategori.isNotEmpty()) kategori else existingItem.kategori,
                            satuan = if (satuan.isNotEmpty()) satuan else existingItem.satuan,
                            stokRusak = existingItem.stokRusak,
                            merekAlat = if (merekAlat.isNotEmpty()) merekAlat else existingItem.merekAlat,
                            ruang = if (ruang.isNotEmpty()) ruang else existingItem.ruang,
                            sumberDana = if (sumberDana != null) sumberDana else existingItem.sumberDana,
                            kondisi = if (kondisi.isNotEmpty()) kondisi else existingItem.kondisi,
                            keterangan = if (keterangan.isNotEmpty()) keterangan else existingItem.keterangan,
                            type = existingItem.type
                        )
                        repository.updateItem(updatedItem)
                        updated++
                    } else {
                        maxIdNum++
                        val newId = "BRG-${String.format(Locale.US, "%03d", maxIdNum)}"
                        val resolvedType = defaultType ?: if (kategori.equals("Logistik", ignoreCase = true)) "BAHAN" else "ALAT"
                        repository.insertItem(
                            id = newId,
                            name = namaBarang,
                            stokAwal = stokAwal,
                            kategori = kategori,
                            satuan = satuan,
                            merekAlat = merekAlat,
                            ruang = ruang,
                            sumberDana = sumberDana,
                            kondisi = kondisi,
                            keterangan = keterangan,
                            type = resolvedType
                        )
                        
                        existingItems.add(
                            ItemWithStock(
                                idBarang = newId,
                                namaBarang = namaBarang,
                                stokAwal = stokAwal,
                                stokTersedia = stokAwal,
                                kategori = kategori,
                                satuan = satuan,
                                stokRusak = 0,
                                merekAlat = merekAlat,
                                ruang = ruang,
                                sumberDana = sumberDana,
                                kondisi = kondisi,
                                keterangan = keterangan,
                                type = resolvedType
                            )
                        )
                        added++
                    }
                }
                
                val webAppUrl = sheetsUrl.value
                if (webAppUrl.isNotEmpty() && autoSyncEnabled.value) {
                    launch {
                        try {
                            repository.syncWithSheets(settingsRepository) { _ -> }
                        } catch (e: Exception) {
                            Log.e("InventoryVM", "Error syncing after CSV import", e)
                        }
                    }
                }
                
                onSuccess(added, updated)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal memproses data CSV")
            }
        }
    }

    // --- BACKUP & RESTORE REMOVED ---

    fun hardLogout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Close database
                db.close()
                
                // 2. Delete database files
                val context = getApplication<Application>()
                val dbFile = context.getDatabasePath("gudang_sman_database")
                val dbWal = java.io.File(dbFile.path + "-wal")
                val dbShm = java.io.File(dbFile.path + "-shm")

                if (dbFile.exists()) dbFile.delete()
                if (dbWal.exists()) dbWal.delete()
                if (dbShm.exists()) dbShm.delete()
                
                // Reset Room DB instance
                AppDatabase.resetDatabaseInstance()
                
                // 3. Clear settings in SettingsRepository
                settingsRepository.clearAllSettings()
                
                // 4. Trigger UI callback
                onComplete()
            } catch (e: Exception) {
                Log.e("Auth", "Hard logout failed", e)
                onComplete()
            }
        }
    }

    fun generateDemoData(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val dao = db.inventoryDao()
                
                // 1. Categories
                val categoriesList = listOf(
                    com.example.data.entity.CategoryEntity(name = "Elektronik", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Alat Tulis Kantor", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Peralatan Olahraga", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Peralatan Lab Fisika", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Peralatan Lab Kimia", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Peralatan Lab Biologi", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Kesenian & Musik", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Peralatan Bengkel", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Media Pembelajaran", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Kebersihan", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Furniture Kelas", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Perlengkapan UKS", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Dokumentasi", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Perlengkapan Pramuka", isDemo = true),
                    com.example.data.entity.CategoryEntity(name = "Keamanan", isDemo = true)
                )
                dao.insertCategories(categoriesList)

                // 2. Units
                val unitsList = listOf(
                    com.example.data.entity.UnitEntity(name = "Unit", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Pcs", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Box", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Lusin", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Set", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Roll", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Pack", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Botol", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Lembar", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Meter", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Liter", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Kilogram", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Batang", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Rim", isDemo = true),
                    com.example.data.entity.UnitEntity(name = "Pasang", isDemo = true)
                )
                dao.insertUnits(unitsList)

                // 3. SharedPreferences: Merek Alat, Merek Bahan, Ruang, Sumber Dana, Kondisi
                val demoMerekAlat = listOf("ASUS", "Epson", "Canon", "Hikvision", "Polytron", "Logitech", "Makita", "Bosch", "Dekko", "Philips", "Lenovo", "HP", "Samsung", "Panasonic", "Sony")
                val demoMerekBahan = listOf("PaperOne", "Joyko", "Kenko", "Sinar Dunia", "Faber-Castell", "Aica Aibon", "3M", "Alteco", "Kenmaster", "Kangaroo", "Standard", "Pilot", "Snowman", "Chappy", "Greebel")
                val demoRuang = listOf("Lab Komputer 1", "Lab Komputer 2", "Lab IPA Fisika", "Lab IPA Biologi", "Lab IPA Kimia", "Perpustakaan", "Ruang Guru", "Ruang Kepala Sekolah", "Ruang TU", "Aula Utama", "Gudang Sarpras", "Bengkel Elektronik", "Ruang Kelas X-A", "Ruang Kelas XI-B", "Ruang Kelas XII-C")
                val demoSumberDana = listOf("BOS Reguler", "BOS Kinerja", "BOP Provinsi", "Bantuan Komite Sekolah", "Bantuan Pemda", "Bantuan Kementerian", "Hibah Alumni 2020", "Sponsorship Industri", "Dana Kas Sekolah", "Bantuan CSR Bank BJB", "Anggaran Yayasan", "Hibah Kedutaan Jepang", "Donasi Orang Tua", "Subsidi Pemerintah Pusat", "Dana Swadaya")
                val demoKondisi = listOf("Sangat Baik", "Baik (Siap Pakai)", "Cukup Baik", "Normal (Terawat)", "Perlu Kalibrasi", "Rusak Ringan", "Rusak Sedang", "Rusak Berat", "Sedang Diperbaiki", "Butuh Suku Cadang", "Selesai Pemeliharaan", "Dalam Pemantauan", "Kadaluwarsa Ringan", "Siap Afkir", "Tidak Layak Guna")

                val currentMerekAlat = (settingsRepository.getMerekAlat() + demoMerekAlat).distinct().sorted()
                settingsRepository.saveMerekAlat(currentMerekAlat)
                _merekAlat.value = currentMerekAlat

                val currentMerekBahan = (settingsRepository.getMerekBahan() + demoMerekBahan).distinct().sorted()
                settingsRepository.saveMerekBahan(currentMerekBahan)
                _merekBahan.value = currentMerekBahan

                val currentRuang = (settingsRepository.getRuang() + demoRuang).distinct().sorted()
                settingsRepository.saveRuang(currentRuang)
                _ruang.value = currentRuang

                val currentSumberDana = (settingsRepository.getSumberDana() + demoSumberDana).distinct().sorted()
                settingsRepository.saveSumberDana(currentSumberDana)
                _sumberDana.value = currentSumberDana

                val currentKondisi = (settingsRepository.getKondisi() + demoKondisi).distinct().sorted()
                settingsRepository.saveKondisi(currentKondisi)
                _kondisi.value = currentKondisi

                // 4. Items (Daftar Alat)
                val alatItems = listOf(
                    ItemEntity("ALT-DEMO-01", "Laptop ASUS Core i5", 15, "Elektronik", "Unit", 0, "ASUS", "Lab Komputer 1", "BOS Reguler", "Sangat Baik", "Laptop untuk ujian", isDemo = true, type = "ALAT"),
                    ItemEntity("ALT-DEMO-02", "Proyektor Epson EB-X400", 15, "Elektronik", "Unit", 0, "Epson", "Aula Utama", "BOS Kinerja", "Baik (Siap Pakai)", "Proyektor presentasi", isDemo = true, type = "ALAT"),
                    ItemEntity("ALT-DEMO-03", "Kamera Canon EOS 1500D", 15, "Dokumentasi", "Unit", 0, "Canon", "Ruang TU", "BOP Provinsi", "Cukup Baik", "Kamera dokumentasi", isDemo = true, type = "ALAT"),
                    ItemEntity("ALT-DEMO-04", "Printer HP Laserjet Pro", 15, "Elektronik", "Unit", 0, "HP", "Ruang Guru", "Dana Swadaya", "Normal (Terawat)", "Printer administrasi", isDemo = true, type = "ALAT"),
                    ItemEntity("ALT-DEMO-05", "Mikroskop Binokuler", 15, "Peralatan Lab Biologi", "Unit", 0, "Philips", "Lab IPA Biologi", "BOS Reguler", "Sangat Baik", "Mikroskop siswa", isDemo = true, type = "ALAT"),
                    ItemEntity("ALT-DEMO-06", "Solder Listrik 40W Dekko", 15, "Peralatan Bengkel", "Unit", 0, "Dekko", "Bengkel Elektronik", "Dana Kas Sekolah", "Normal (Terawat)", "Solder praktik", isDemo = true, type = "ALAT"),
                    ItemEntity("ALT-DEMO-07", "Speaker Portable Polytron", 15, "Elektronik", "Unit", 0, "Polytron", "Aula Utama", "Bantuan Komite Sekolah", "Cukup Baik", "Speaker upacara", isDemo = true, type = "ALAT"),
                    ItemEntity("ALT-DEMO-08", "CCTV Dome Hikvision 2MP", 15, "Keamanan", "Unit", 0, "Hikvision", "Gudang Sarpras", "BOS Kinerja", "Sangat Baik", "Kamera pemantau", isDemo = true, type = "ALAT"),
                    ItemEntity("ALT-DEMO-09", "Keyboard Logitech K120", 15, "Elektronik", "Unit", 0, "Logitech", "Lab Komputer 2", "BOS Reguler", "Normal (Terawat)", "Keyboard Lab", isDemo = true, type = "ALAT"),
                    ItemEntity("ALT-DEMO-10", "Gitar Akustik Yamaha", 15, "Kesenian & Musik", "Unit", 0, "Sony", "Aula Utama", "Bantuan Pemda", "Cukup Baik", "Alat musik seni", isDemo = true, type = "ALAT")
                )
                dao.insertItems(alatItems)

                // 5. Items (Daftar Bahan, i.e., category = "Logistik")
                val bahanItems = listOf(
                    ItemEntity("BHN-DEMO-01", "Kertas HVS A4 80g PaperOne", 100, "Logistik", "Rim", 0, "PaperOne", "Ruang TU", "BOS Reguler", "Normal (Terawat)", "Kertas print laporan", isDemo = true, type = "BAHAN"),
                    ItemEntity("BHN-DEMO-02", "Buku Tulis Sidu 38 Lembar", 100, "Logistik", "Pack", 0, "Sinar Dunia", "Gudang Sarpras", "Bantuan Komite Sekolah", "Normal (Terawat)", "Buku bantuan siswa", isDemo = true, type = "BAHAN"),
                    ItemEntity("BHN-DEMO-03", "Spidol Snowman Boardmarker Hitam", 100, "Logistik", "Lusin", 0, "Snowman", "Gudang Sarpras", "BOS Reguler", "Normal (Terawat)", "Spidol kelas", isDemo = true, type = "BAHAN"),
                    ItemEntity("BHN-DEMO-04", "Lem Fox Kaleng 150g", 100, "Logistik", "Botol", 0, "Aica Aibon", "Bengkel Elektronik", "Dana Kas Sekolah", "Normal (Terawat)", "Lem perekat prakarya", isDemo = true, type = "BAHAN"),
                    ItemEntity("BHN-DEMO-05", "Double Tape 3M 1 Inch", 100, "Logistik", "Roll", 0, "3M", "Ruang TU", "BOS Reguler", "Normal (Terawat)", "Perekat dinding", isDemo = true, type = "BAHAN"),
                    ItemEntity("BHN-DEMO-06", "Pulpen Standard AE7 Hitam", 100, "Logistik", "Box", 0, "Standard", "Ruang Guru", "BOS Reguler", "Normal (Terawat)", "Pulpen guru", isDemo = true, type = "BAHAN"),
                    ItemEntity("BHN-DEMO-07", "Pensil Greebel 2B", 100, "Logistik", "Box", 0, "Greebel", "Gudang Sarpras", "Bantuan Pemda", "Normal (Terawat)", "Pensil ujian", isDemo = true, type = "BAHAN"),
                    ItemEntity("BHN-DEMO-08", "Penghapus Kenko Kecil", 100, "Logistik", "Box", 0, "Kenko", "Gudang Sarpras", "Dana Kas Sekolah", "Normal (Terawat)", "Penghapus karet", isDemo = true, type = "BAHAN"),
                    ItemEntity("BHN-DEMO-09", "Staples Kangaroo No.10", 100, "Logistik", "Box", 0, "Kangaroo", "Ruang TU", "BOS Reguler", "Normal (Terawat)", "Isi staples", isDemo = true, type = "BAHAN"),
                    ItemEntity("BHN-DEMO-10", "Isolasi Bening Joyko 2 Inch", 100, "Logistik", "Roll", 0, "Joyko", "Gudang Sarpras", "Dana Swadaya", "Normal (Terawat)", "Isolasi packing", isDemo = true, type = "BAHAN")
                )
                dao.insertItems(bahanItems)

                // 6. Active Loans (Peminjaman, status == "Dipinjam")
                val activeLoans = mutableListOf<LoanTransactionEntity>()
                val activeLoanItems = mutableListOf<LoanItemEntity>()
                val names = listOf("Rian", "Siti", "Hendra", "Dewi", "Agus", "Yanti", "Fajar", "Lina", "Taufik", "Novi", "Andi", "Rina", "Budi", "Santi", "Eko")
                val classesList = listOf("X-MIPA-1", "X-MIPA-2", "XI-IPS-1", "XI-IPS-2", "XII-MIPA-3", "XII-IPS-1", "X-IPS-2", "XI-MIPA-4", "XII-MIPA-1", "X-MIPA-3", "XI-IPS-3", "XII-IPS-2", "XI-MIPA-2", "XII-MIPA-2", "X-IPS-1")
                
                for (i in 1..10) {
                    val txId = "TX-LND-DEMO-${String.format(Locale.US, "%02d", i)}"
                    val name = names[(i - 1) % names.size] + " Demo"
                    val className = classesList[(i - 1) % classesList.size]
                    val itemId = String.format(Locale.US, "ALT-DEMO-%02d", ((i - 1) % 10) + 1)
                    val realItemName = alatItems[(i - 1) % alatItems.size].namaBarang
                    
                    activeLoans.add(
                        LoanTransactionEntity(
                            idTransaksi = txId,
                            tanggal = "2026-07-16",
                            namaPeminjam = name,
                            kelas = className,
                            waktu = "08:${String.format(Locale.US, "%02d", i + 10)}",
                            kondisi = "Baik",
                            namaPetugas = "Administrator",
                            status = "Dipinjam",
                            isDemo = true
                        )
                    )
                    
                    activeLoanItems.add(
                        LoanItemEntity(
                            idTransaksi = txId,
                            idBarang = itemId,
                            namaBarang = realItemName,
                            jumlah = 1,
                            isDemo = true
                        )
                    )
                }
                dao.insertTransactions(activeLoans)
                dao.insertLoanItems(activeLoanItems)

                // 7. Returned Loans (Pengembalian, status == "Kembali")
                val returnedLoans = mutableListOf<LoanTransactionEntity>()
                val returnedLoanItems = mutableListOf<LoanItemEntity>()
                
                for (i in 1..10) {
                    val txId = "TX-RTN-DEMO-${String.format(Locale.US, "%02d", i)}"
                    val name = names[(i + 2) % names.size] + " Demo"
                    val className = classesList[(i + 2) % classesList.size]
                    val itemId = String.format(Locale.US, "ALT-DEMO-%02d", ((i + 3) % 10) + 1)
                    val realItemName = alatItems[(i + 3) % alatItems.size].namaBarang
                    
                    returnedLoans.add(
                        LoanTransactionEntity(
                            idTransaksi = txId,
                            tanggal = "2026-07-11",
                            namaPeminjam = name,
                            kelas = className,
                            waktu = "09:${String.format(Locale.US, "%02d", i + 10)}",
                            kondisi = "Baik",
                            namaPetugas = "Administrator",
                            status = "Kembali",
                            tanggalKembali = "2026-07-13",
                            waktuKembali = "14:00",
                            kondisiKembali = "Baik",
                            petugasKembali = "Administrator",
                            isDemo = true
                        )
                    )
                    
                    returnedLoanItems.add(
                        LoanItemEntity(
                            idTransaksi = txId,
                            idBarang = itemId,
                            namaBarang = realItemName,
                            jumlah = 1,
                            isDemo = true
                        )
                    )
                }
                dao.insertTransactions(returnedLoans)
                dao.insertLoanItems(returnedLoanItems)

                // 8. Pemakaian Bahan (PemakaianBahanEntity)
                val pemakaianList = mutableListOf<com.example.data.entity.PemakaianBahanEntity>()
                for (i in 1..10) {
                    val useId = "TX-USE-DEMO-${String.format(Locale.US, "%02d", i)}"
                    val realItem = bahanItems[(i - 1) % bahanItems.size]
                    pemakaianList.add(
                        com.example.data.entity.PemakaianBahanEntity(
                            idPemakaian = useId,
                            idBarang = realItem.idBarang,
                            namaBarang = realItem.namaBarang,
                            jumlahDiambil = 2,
                            satuan = realItem.satuan,
                            namaPeminta = names[(i + 4) % names.size] + " Demo",
                            jabatan = "Guru",
                            kelas = "X-B",
                            namaPetugas = "Administrator",
                            tanggalPemakaian = "2026-07-15",
                            keterangan = "Pemakaian praktikum kelas X-B",
                            isDemo = true
                        )
                    )
                }
                dao.insertPemakaianBahanList(pemakaianList)

                // 9. Alat Rusak (DamagedItemEntity status != Servis/Pemeliharaan && status != Normal)
                val damagedList = mutableListOf<com.example.data.entity.DamagedItemEntity>()
                for (i in 1..10) {
                    val realItem = alatItems[(i) % alatItems.size]
                    damagedList.add(
                        com.example.data.entity.DamagedItemEntity(
                            idBarang = realItem.idBarang,
                            namaBarang = realItem.namaBarang,
                            jumlah = 1,
                            tanggalKerusakan = "2026-07-14",
                            waktuKerusakan = "10:30",
                            keteranganKerusakan = "Kerusakan fisik sedang",
                            namaPetugas = "Administrator",
                            kondisiBaru = "Rusak",
                            status = "Rusak (Perlu Tindakan)",
                            statusKeterangan = "Butuh perbaikan mendesak",
                            isDemo = true
                        )
                    )
                }
                dao.insertDamagedItems(damagedList)

                // 10. Pemeliharaan (DamagedItemEntity status == "Servis Luar/Pemeliharaan" or "Pemeliharaan")
                val maintenanceList = mutableListOf<com.example.data.entity.DamagedItemEntity>()
                for (i in 1..10) {
                    val realItem = alatItems[(i + 2) % alatItems.size]
                    maintenanceList.add(
                        com.example.data.entity.DamagedItemEntity(
                            idBarang = realItem.idBarang,
                            namaBarang = realItem.namaBarang,
                            jumlah = 1,
                            tanggalKerusakan = "2026-07-15",
                            waktuKerusakan = "11:15",
                            keteranganKerusakan = "Pemeliharaan rutin berkala",
                            namaPetugas = "Administrator",
                            kondisiBaru = "Pemeliharaan",
                            status = "Servis Luar/Pemeliharaan",
                            statusKeterangan = "Servis luar oleh teknisi",
                            isDemo = true
                        )
                    )
                }
                dao.insertDamagedItems(maintenanceList)

                // 11. Bahan Afkir (BahanAfkirEntity)
                val afkirList = mutableListOf<com.example.data.entity.BahanAfkirEntity>()
                for (i in 1..10) {
                    val realItem = bahanItems[(i + 1) % bahanItems.size]
                    afkirList.add(
                        com.example.data.entity.BahanAfkirEntity(
                            idAfkir = "TX-AFK-DEMO-${String.format(Locale.US, "%02d", i)}",
                            idBarang = realItem.idBarang,
                            namaBarang = realItem.namaBarang,
                            jumlahAfkir = 1,
                            satuan = realItem.satuan,
                            alasan = if (i % 2 == 0) "Kedaluwarsa" else "Rusak Fisik",
                            tanggalAfkir = "2026-07-13",
                            status = "Aktif",
                            isDemo = true
                        )
                    )
                }
                dao.insertBahanAfkirList(afkirList)

                settingsRepository.setDemoFinished(false) // Reset demo finished state
                onResult(true, "Data contoh berhasil ditambahkan! Selamat mengeksplorasi.")
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error generating demo data", e)
                onResult(false, "Gagal membuat data demo: ${e.localizedMessage}")
            }
        }
    }

    fun hapusDataDemo(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val dao = db.inventoryDao()
                
                // 1. Delete Room Tables demo data
                dao.deleteDemoCategories()
                dao.deleteDemoUnits()
                dao.deleteDemoItems()
                dao.deleteDemoTransactions()
                dao.deleteDemoLoanItems()
                dao.deleteDemoDamagedItems()
                dao.deleteDemoPemakaianBahan()
                dao.deleteDemoBahanAfkir()

                // Clear Firestore transaction collections as well
                firebaseService.clearAllTransactionsFromFirestore()

                // 2. Clear SharedPreferences demo entries
                val demoMerekAlat = listOf("ASUS", "Epson", "Canon", "Hikvision", "Polytron", "Logitech", "Makita", "Bosch", "Dekko", "Philips", "Lenovo", "HP", "Samsung", "Panasonic", "Sony")
                val demoMerekBahan = listOf("PaperOne", "Joyko", "Kenko", "Sinar Dunia", "Faber-Castell", "Aica Aibon", "3M", "Alteco", "Kenmaster", "Kangaroo", "Standard", "Pilot", "Snowman", "Chappy", "Greebel")
                val demoRuang = listOf("Lab Komputer 1", "Lab Komputer 2", "Lab IPA Fisika", "Lab IPA Biologi", "Lab IPA Kimia", "Perpustakaan", "Ruang Guru", "Ruang Kepala Sekolah", "Ruang TU", "Aula Utama", "Gudang Sarpras", "Bengkel Elektronik", "Ruang Kelas X-A", "Ruang Kelas XI-B", "Ruang Kelas XII-C")
                val demoSumberDana = listOf("BOS Reguler", "BOS Kinerja", "BOP Provinsi", "Bantuan Komite Sekolah", "Bantuan Pemda", "Bantuan Kementerian", "Hibah Alumni 2020", "Sponsorship Industri", "Dana Kas Sekolah", "Bantuan CSR Bank BJB", "Anggaran Yayasan", "Hibah Kedutaan Jepang", "Donasi Orang Tua", "Subsidi Pemerintah Pusat", "Dana Swadaya")
                val demoKondisi = listOf("Sangat Baik", "Baik (Siap Pakai)", "Cukup Baik", "Normal (Terawat)", "Perlu Kalibrasi", "Rusak Ringan", "Rusak Sedang", "Rusak Berat", "Sedang Diperbaiki", "Butuh Suku Cadang", "Selesai Pemeliharaan", "Dalam Pemantauan", "Kadaluwarsa Ringan", "Siap Afkir", "Tidak Layak Guna")

                val cleanMerekAlat = settingsRepository.getMerekAlat().filterNot { demoMerekAlat.contains(it) }.sorted()
                settingsRepository.saveMerekAlat(cleanMerekAlat)
                _merekAlat.value = cleanMerekAlat

                val cleanMerekBahan = settingsRepository.getMerekBahan().filterNot { demoMerekBahan.contains(it) }.sorted()
                settingsRepository.saveMerekBahan(cleanMerekBahan)
                _merekBahan.value = cleanMerekBahan

                val cleanRuang = settingsRepository.getRuang().filterNot { demoRuang.contains(it) }.sorted()
                settingsRepository.saveRuang(cleanRuang)
                _ruang.value = cleanRuang

                val cleanSumberDana = settingsRepository.getSumberDana().filterNot { demoSumberDana.contains(it) }.sorted()
                settingsRepository.saveSumberDana(cleanSumberDana)
                _sumberDana.value = cleanSumberDana

                val cleanKondisi = settingsRepository.getKondisi().filterNot { demoKondisi.contains(it) }.sorted()
                settingsRepository.saveKondisi(cleanKondisi)
                _kondisi.value = cleanKondisi

                settingsRepository.setDemoFinished(true) // Mark as finished permanently
                onResult(true, "Data contoh berhasil dibersihkan! Anda bisa mulai mencatat data asli.")
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error clearing demo data", e)
                onResult(false, "Gagal menghapus data demo: ${e.localizedMessage}")
            }
        }
    }

    // Backup & Restore Database Operations
    fun performBackup(outputStream: java.io.OutputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val backupManager = com.example.data.database.BackupManager(db)
                val result = backupManager.exportDatabase(outputStream)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengekspor data")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal mengekspor data")
            }
        }
    }

    fun performRestore(inputStream: java.io.InputStream, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val backupManager = com.example.data.database.BackupManager(db)
                val result = backupManager.importDatabase(inputStream)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.localizedMessage ?: "Gagal mengimpor data")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Gagal mengimpor data")
            }
        }
    }
}
