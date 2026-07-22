package com.example.ui.screens
import com.example.ui.components.LunarisCard
import coil.compose.AsyncImage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.components.DynamicLogo
import com.example.ui.theme.PastelSoftBlue
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.SoftCream
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.SoftGoldText
import com.example.ui.theme.CarbonBlackText
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GlassWhiteMore
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassLavender
import com.example.ui.theme.BrightGold
import com.example.ui.theme.pastelGradientBackground
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import android.widget.Toast
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

data class GlobalActivity(
    val id: String,
    val title: String,
    val subtitle: String,
    val date: String,
    val time: String,
    val type: String, // "Sirkulasi", "Bahan", "Input Baru", "Manajemen"
    val statusText: String,
    val statusColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InventoryViewModel,
    onNavigateToMenu: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = false
    val items by viewModel.itemsWithStock.collectAsState()
    val activeTransactions by viewModel.activeTransactions.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()

    val instansiName by viewModel.instansiName.collectAsState()
    val instansiLogoPath by viewModel.instansiLogoPath.collectAsState()
    val namaPetugasState by viewModel.defaultOfficer.collectAsState()
    val namaPetugas = namaPetugasState.ifBlank { "Administrator" }
    val officerNipState by viewModel.officerNip.collectAsState()
    val officerNipText = if (officerNipState.isNotBlank()) {
        if (officerNipState.lowercase().startsWith("nip")) officerNipState else "NIP: $officerNipState"
    } else {
        "NIP: -"
    }

    val allTransactions by viewModel.allTransactions.collectAsState()
    val allPemakaianBahan by viewModel.allPemakaianBahan.collectAsState()

    val harianChartData by viewModel.harianChartData.collectAsState()
    val mingguanChartData by viewModel.mingguanChartData.collectAsState()
    val bulananChartData by viewModel.bulananChartData.collectAsState()

    var recentFirestoreActivities by remember { mutableStateOf<List<GlobalActivity>>(emptyList()) }

    DisposableEffect(Unit) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val listener = firestore.collection("transactions")
            .orderBy("tanggal", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .orderBy("waktu", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("DashboardScreen", "Error listening to transactions", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<GlobalActivity>()
                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val namaPeminjam = doc.getString("namaPeminjam") ?: ""
                        val status = doc.getString("status") ?: "Dipinjam"
                        val tanggal = doc.getString("tanggal") ?: ""
                        val waktu = doc.getString("waktu") ?: ""
                        
                        val isReturned = status == "Kembali"
                        list.add(
                            GlobalActivity(
                                id = id,
                                title = namaPeminjam,
                                subtitle = "Sirkulasi Alat ($id)",
                                date = tanggal,
                                time = waktu,
                                type = "Sirkulasi",
                                statusText = if (isReturned) "Kembali" else "Dipinjam",
                                statusColor = if (isReturned) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        )
                    }
                    recentFirestoreActivities = list
                }
            }
        onDispose {
            listener.remove()
        }
    }

    val globalActivities = remember(recentFirestoreActivities, allTransactions, allPemakaianBahan) {
        val list = mutableListOf<GlobalActivity>()
        
        // 1. Add real-time loan transactions from Firestore
        list.addAll(recentFirestoreActivities)
        
        // 2. Add other local transaction activities (not sirkulasi)
        allTransactions.forEach { tx ->
            val isSirkulasi = !tx.idTransaksi.startsWith("TX-INP") && 
                              !tx.idTransaksi.startsWith("TX-AFK") && 
                              !tx.idTransaksi.startsWith("TX-DMG") && 
                              !tx.idTransaksi.startsWith("TX-OPN") && 
                              !tx.idTransaksi.startsWith("TX-RUM") && 
                              !tx.idTransaksi.startsWith("TX-SYN")
            if (!isSirkulasi) {
                if (tx.idTransaksi.startsWith("TX-INP")) {
                    list.add(
                        GlobalActivity(
                            id = tx.idTransaksi,
                            title = tx.namaPeminjam,
                            subtitle = "Aset Baru Terdaftar",
                            date = tx.tanggal,
                            time = tx.waktu,
                            type = "Input Baru",
                            statusText = "Aset Baru",
                            statusColor = Color(0xFF3B82F6)
                        )
                    )
                } else if (tx.idTransaksi.startsWith("TX-OPN")) {
                    list.add(
                        GlobalActivity(
                            id = tx.idTransaksi,
                            title = tx.namaPeminjam,
                            subtitle = "Stock Opname Penyesuaian",
                            date = tx.tanggal,
                            time = tx.waktu,
                            type = "Manajemen",
                            statusText = "Opname",
                            statusColor = Color(0xFF8B5CF6)
                        )
                    )
                }
            }
        }
        
        allPemakaianBahan.forEach { pmk ->
            list.add(
                GlobalActivity(
                    id = pmk.idPemakaian,
                    title = pmk.namaPeminta,
                    subtitle = "Pemakaian ${pmk.namaBarang} (${pmk.jumlahDiambil} ${pmk.satuan})",
                    date = pmk.tanggalPemakaian,
                    time = "12:00",
                    type = "Bahan",
                    statusText = "Pemakaian",
                    statusColor = Color(0xFFEC4899)
                )
            )
        }
        
        list.sortedWith(compareByDescending<GlobalActivity> { it.date }.thenByDescending { it.time })
    }

    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isOperasionalExpanded by rememberSaveable { mutableStateOf(false) }
    var isSirkulasiExpanded by rememberSaveable { mutableStateOf(false) }
    var isAnalisisExpanded by rememberSaveable { mutableStateOf(false) }
    var isAktivitasExpanded by rememberSaveable { mutableStateOf(false) }
    var isPengembalianExpanded by rememberSaveable { mutableStateOf(false) }

    // Compute metrics
    val totalStokAwal = remember(items) { items.sumOf { it.stokAwal } }
    val totalStokTersedia = remember(items) { items.sumOf { it.stokTersedia } }
    val activeLoanCount = remember(activeTransactions) { activeTransactions.size }

    val context = LocalContext.current
    val itemsCache = remember { mutableStateMapOf<String, List<com.example.data.entity.LoanItemEntity>>() }
    var showAlertDialog by remember { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<AlertItem?>(null) }
    var showProfileBottomSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        showExitDialog = true
    }

    LaunchedEffect(activeTransactions, globalActivities) {
        activeTransactions.forEach { tx ->
            if (!itemsCache.containsKey(tx.idTransaksi)) {
                val list = viewModel.getItemsForTransaction(tx.idTransaksi)
                itemsCache[tx.idTransaksi] = list
            }
        }
        globalActivities.forEach { act ->
            if (act.type == "Sirkulasi" && !itemsCache.containsKey(act.id)) {
                val list = viewModel.getItemsForTransaction(act.id)
                itemsCache[act.id] = list
            }
        }
    }

    val alertItems = remember(activeTransactions, itemsCache) {
        fun getOverdueStatus(tanggalTransaksi: String, durasiHari: Int): Pair<String, Int> {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val transDate = sdf.parse(tanggalTransaksi) ?: return Pair("Hari Ini", 0)
                
                val calDeadline = Calendar.getInstance().apply {
                    time = transDate
                    add(Calendar.DAY_OF_YEAR, durasiHari)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val calToday = Calendar.getInstance().apply {
                    set(Calendar.YEAR, 2026)
                    set(Calendar.MONTH, Calendar.JULY)
                    set(Calendar.DAY_OF_MONTH, 16)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                val diffMs = calToday.timeInMillis - calDeadline.timeInMillis
                val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                
                return if (diffDays <= 0) {
                    Pair("Hari Ini", 0)
                } else {
                    Pair("Terlambat $diffDays Hari", diffDays)
                }
            } catch (e: Exception) {
                return Pair("Hari Ini", 0)
            }
        }

        val list = mutableListOf<AlertItem>()
        
        // Add real active transactions
        activeTransactions.forEach { tx ->
            val txItems = itemsCache[tx.idTransaksi] ?: emptyList()
            val itemsStr = if (txItems.isEmpty()) {
                "Barang Inventaris"
            } else {
                txItems.joinToString(", ") { "${it.namaBarang} (${it.jumlah}x)" }
            }
            
            val (label, days) = getOverdueStatus(tx.tanggal, tx.durasiHari)
            list.add(
                AlertItem(
                    idTransaksi = tx.idTransaksi,
                    namaPeminjam = tx.namaPeminjam,
                    barangText = itemsStr,
                    tanggal = tx.tanggal,
                    deadlineLabel = label,
                    daysOverdue = days,
                    whatsappNumber = tx.whatsappNumber
                )
            )
        }
        
        list
    }

    val sirkulasiBadgeCount = remember(alertItems) {
        alertItems.count { it.daysOverdue > 0 }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    LaunchedEffect(drawerState.isOpen) {
        viewModel.setDrawerOpen(drawerState.isOpen)
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.setDrawerOpen(false)
        }
    }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var selectedFilter by remember { mutableStateOf("Harian") }

    val userProfilePhoto by viewModel.userProfilePhoto.collectAsState()
    val userProfileBitmap = remember(userProfilePhoto) {
        if (userProfilePhoto.isNotEmpty()) {
            try {
                val uri = android.net.Uri.parse(userProfilePhoto)
                val inputStream = context.contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            } catch (e: Exception) {
                try {
                    val file = java.io.File(userProfilePhoto)
                    if (file.exists()) {
                        android.graphics.BitmapFactory.decodeFile(userProfilePhoto)?.asImageBitmap()
                    } else null
                } catch (ex: Exception) {
                    null
                }
            }
        } else null
    }

    val logoFile = remember(instansiLogoPath) {
        if (instansiLogoPath.isNotEmpty() && !instansiLogoPath.startsWith("content://") && !instansiLogoPath.startsWith("file://")) {
            try {
                java.io.File(instansiLogoPath)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    val lastModified = logoFile?.lastModified() ?: 0L
    val logoBitmap = remember(instansiLogoPath, lastModified) {
        if (instansiLogoPath.isNotEmpty()) {
            try {
                val uri = android.net.Uri.parse(instansiLogoPath)
                val inputStream = context.contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            } catch (e: Exception) {
                try {
                    val file = java.io.File(instansiLogoPath)
                    if (file.exists()) {
                        android.graphics.BitmapFactory.decodeFile(instansiLogoPath)?.asImageBitmap()
                    } else {
                        null
                    }
                } catch (ex: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFFAFAFC),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE9D5FF),
                        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                ) {
                    // Profile/Header inside drawer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(54.dp)
                        ) {
                            // Avatar circle using user profile photo
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center)
                                    .shadow(elevation = 2.dp, shape = CircleShape)
                                    .background(Color.White, CircleShape)
                                    .border(1.dp, Color(0xFFE9D5FF), CircleShape)
                                    .clip(CircleShape)
                            ) {
                                if (userProfileBitmap != null) {
                                    Image(
                                        bitmap = userProfileBitmap,
                                        contentDescription = "Foto Profil",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (userProfilePhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = userProfilePhoto,
                                        contentDescription = "Foto Profil",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Foto Profil",
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            // Active status dot
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)) // Green status indicator
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = namaPetugas,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF3B0764)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = officerNipText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = instansiName.ifBlank { "Gudang Utama Lunaris" },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE9D5FF))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Drawer Menu Items
                    if (userRole == "admin") {
                        DrawerMenuItem(
                            label = "Master Data",
                            icon = Icons.Default.Storage,
                            iconColor = Color(0xFF10B981),
                            bgColor = Color(0xFFECFDF5),
                            borderColor = Color(0xFFD1FAE5),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToMenu("Master Data")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DrawerMenuItem(
                            label = "Stok Opname",
                            icon = Icons.Default.Inventory,
                            iconColor = Color(0xFF3B82F6),
                            bgColor = Color(0xFFEFF6FF),
                            borderColor = Color(0xFFDBEAFE),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToMenu("Stok Opname")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DrawerMenuItem(
                            label = "Pengaturan Akses",
                            icon = Icons.Default.Security,
                            iconColor = Color(0xFFEA580C),
                            bgColor = Color(0xFFFFEDD5),
                            borderColor = Color(0xFFFED7AA),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToMenu("Pengaturan Akses")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DrawerMenuItem(
                            label = "Manajemen Pengguna",
                            icon = Icons.Default.AccountCircle,
                            iconColor = Color(0xFF7C3AED),
                            bgColor = Color(0xFFF3E8FF),
                            borderColor = Color(0xFFE9D5FF),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToMenu("Manajemen Pengguna")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (userRole.contains("admin", ignoreCase = true) || (studentPermissions["scan_qr"] == true) || (studentPermissions["generate_qr"] == true) || (studentPermissions["qr_group"] == true)) {
                        DrawerMenuItem(
                            label = "Scan QR Code",
                            icon = Icons.Default.QrCode,
                            iconColor = Color(0xFFDB2777),
                            bgColor = Color(0xFFFDF2F8),
                            borderColor = Color(0xFFFCE7F3),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToMenu("Scan QR")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (userRole == "admin") {
                        DrawerMenuItem(
                            label = "Backup & Restore",
                            icon = Icons.Default.CloudSync,
                            iconColor = Color(0xFF8B5CF6),
                            bgColor = Color(0xFFF5F3FF),
                            borderColor = Color(0xFFEDE9FE),
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToMenu("Backup & Restore")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    DrawerMenuItem(
                        label = "Pengaturan",
                        icon = Icons.Default.Settings,
                        iconColor = Color(0xFF6B7280),
                        bgColor = Color(0xFFF3F4F6),
                        borderColor = Color(0xFFE5E7EB),
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                onNavigateToMenu("Pengaturan")
                            }
                        }
                    )

                    // Spacer to push the exit/logout button and divider to the bottom
                    Spacer(modifier = Modifier.weight(1f))

                    // Elegant divider right above Logout/Keluar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x267C3AED)) // Very subtle violet-purple color
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    DrawerMenuItem(
                        label = "Keluar",
                        icon = Icons.Default.ExitToApp,
                        iconColor = Color(0xFFEF4444),
                        textColor = Color(0xFFEF4444),
                        bgColor = Color(0xFFFEF2F2),
                        borderColor = Color(0xFFFCA5A5),
                        onClick = {
                            scope.launch { drawerState.close() }
                            showExitDialog = true
                        }
                    )
                }
            }
        }
    ) {
        val isDark = false
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.9f),
                                    Color(0xFF2DD4BF).copy(alpha = 0.9f)
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Hamburger Menu Button (tiga garis)
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("btn_hamburger_drawer")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu Samping",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Lunaris • ${instansiName.ifBlank { "Gudang Utama" }}",
                                        fontSize = 18.sp,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "$namaPetugas • $officerNipText",
                                        fontSize = 14.sp,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Avatar / Profile Photo Container
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(elevation = 6.dp, shape = CircleShape)
                                    .background(Color.White, CircleShape)
                                    .border(1.5.dp, Color(0xFFE9D5FF), CircleShape)
                                    .clip(CircleShape)
                                    .clickable { showProfileBottomSheet = true }
                            ) {
                                if (userProfileBitmap != null) {
                                    Image(
                                        bitmap = userProfileBitmap,
                                        contentDescription = "Foto Profil",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (userProfilePhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = userProfilePhoto,
                                        contentDescription = "Foto Profil",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Foto Profil",
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                    androidx.compose.material3.HorizontalDivider(
                        thickness = 1.2.dp,
                        color = Color.Transparent
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pastelGradientBackground(isDark = isDark)
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
                ) {





        // Floating Search Results inside Dashboard if search query is active
        if (searchQuery.isNotEmpty()) {
            val filteredItems = items.filter { 
                it.namaBarang.contains(searchQuery, ignoreCase = true) || 
                it.idBarang.contains(searchQuery, ignoreCase = true) 
            }
            LunarisCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hasil Pencarian Gudang (${filteredItems.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (filteredItems.isEmpty()) {
                        Text(
                            text = "Barang tidak ditemukan",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    } else {
                        filteredItems.take(5).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.namaBarang, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CarbonBlackText)
                                    Text(item.idBarang, fontSize = 10.sp, color = Color.Gray)
                                }
                                Text(
                                    text = "Stok: ${item.stokTersedia}/${item.stokAwal}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.stokTersedia <= 2) Color(0xFFEF4444) else Color(0xFF22C55E)
                                )
                            }
                        }
                    }
                }
            }
        }



        // Metrics Section Card with pure white clean layout, soft shadow, and thin lavender border
        LunarisCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_banner_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stock Metric Column (with soft lavender background)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF5F3FF), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "TOTAL STOK BARANG",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$totalStokTersedia / $totalStokAwal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CarbonBlackText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Stok Fisik Tersedia",
                        fontSize = 10.sp,
                        color = CarbonBlackText.copy(alpha = 0.7f)
                    )
                }

                // Loan Metric Column (with soft blue background)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "PEMINJAMAN ALAT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$activeLoanCount Transaksi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D4ED8) // Elegant deep blue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sedang Keluar",
                        fontSize = 10.sp,
                        color = CarbonBlackText.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title List Header
        Text(
            text = "Menu Layanan Gudang",
            style = MaterialTheme.typography.titleMedium,
            color = DeepPurpleText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = "Silakan pilih modul administrasi gudang di bawah ini:",
            style = MaterialTheme.typography.bodySmall,
            color = CarbonBlackText.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        fun isMenuAllowedForSiswa(route: String): Boolean {
            if (userRole.contains("admin", ignoreCase = true)) return true
            return when (route) {
                "Scan QR" -> (studentPermissions["scan_qr"] == true) || (studentPermissions["generate_qr"] == true) || (studentPermissions["qr_group"] == true)
                "Alat" -> (studentPermissions["alat"] == true) || (studentPermissions["alat_view"] == true) || (studentPermissions["alat_detail"] == true) || (studentPermissions["alat_import"] == true) || (studentPermissions["alat_export"] == true)
                "Bahan" -> (studentPermissions["bahan"] == true) || (studentPermissions["bahan_view"] == true) || (studentPermissions["bahan_detail"] == true) || (studentPermissions["bahan_import"] == true) || (studentPermissions["bahan_export"] == true)
                "Pemakaian Bahan" -> (studentPermissions["pemakaian_bahan"] == true) || (studentPermissions["pemakaian_bahan_form"] == true) || (studentPermissions["pemakaian_bahan_log"] == true)
                "Bahan Afkir" -> (studentPermissions["bahan_afkir"] == true) || (studentPermissions["bahan_afkir_submit"] == true) || (studentPermissions["bahan_afkir_view"] == true)
                "Alat Rusak" -> (studentPermissions["alat_rusak"] == true) || (studentPermissions["alat_rusak_submit"] == true) || (studentPermissions["alat_rusak_view"] == true)
                "Pemeliharaan" -> (studentPermissions["pemeliharaan"] == true) || (studentPermissions["pemeliharaan_tambah"] == true) || (studentPermissions["pemeliharaan_view"] == true)
                "Peminjaman" -> (studentPermissions["peminjaman"] == true) || (studentPermissions["peminjaman_form"] == true) || (studentPermissions["peminjaman_riwayat"] == true)
                "Pengembalian" -> (studentPermissions["pengembalian"] == true) || (studentPermissions["pengembalian_normal"] == true) || (studentPermissions["pengembalian_parsial"] == true)
                "Kondisi Alat" -> (studentPermissions["kondisi_alat"] == true) || (studentPermissions["kondisi_alat_catat"] == true) || (studentPermissions["kondisi_alat_view"] == true)
                "Log Transaksi" -> (studentPermissions["log_transaksi"] == true) || (studentPermissions["log_sirkulasi"] == true) || (studentPermissions["log_bahan_habis"] == true) || (studentPermissions["log_stok"] == true) || (studentPermissions["log_pemeliharaan"] == true) || (studentPermissions["log_aktivitas"] == true)
                "Master Data" -> (studentPermissions["master_data"] == true) || (studentPermissions["master_data_view"] == true) || (studentPermissions["master_data_manage"] == true)
                "Stok Opname" -> (studentPermissions["stok_opname"] == true) || (studentPermissions["stok_opname_audit"] == true) || (studentPermissions["stok_opname_reconcile"] == true)
                "Laporan" -> (studentPermissions["laporan"] == true) || (studentPermissions["laporan_ringkasan"] == true) || (studentPermissions["laporan_alat"] == true) || (studentPermissions["laporan_bahan"] == true) || (studentPermissions["laporan_afkir"] == true) || (studentPermissions["laporan_peminjaman"] == true) || (studentPermissions["laporan_pengembalian"] == true) || (studentPermissions["laporan_alat_rusak"] == true) || (studentPermissions["laporan_pemeliharaan"] == true) || (studentPermissions["laporan_export_excel"] == true) || (studentPermissions["laporan_print_pdf"] == true)
                else -> true
            }
        }

        val operasionalMenus = remember(userRole, studentPermissions) {
            listOf(
                DashboardMenuData("ALAT", "Kelola & cek stok barang", Icons.Default.Build, Color(0xFF7C3AED), Color(0xFFF3E8FF), "menu_alat", "Alat"),
                DashboardMenuData("BAHAN", "Stok barang habis pakai", Icons.Default.Science, Color(0xFF0284C7), Color(0xFFE0F2FE), "menu_bahan", "Bahan"),
                DashboardMenuData("Pemakaian Bahan", "Pakai bahan habis pakai", Icons.Default.ShoppingCart, Color(0xFFDB2777), Color(0xFFFCE7F3), "menu_pemakaian_bahan", "Pemakaian Bahan"),
                DashboardMenuData("Bahan Afkir", "Bahan rusak / kedaluwarsa", Icons.Default.DeleteSweep, Color(0xFFEA580C), Color(0xFFFFEDD5), "menu_bahan_afkir", "Bahan Afkir"),
                DashboardMenuData("Alat Rusak", "Kelola & lapor alat rusak", Icons.Default.Warning, Color(0xFFEF4444), Color(0xFFFFECEF), "menu_alat_rusak", "Alat Rusak"),
                DashboardMenuData("Pemeliharaan", "Servis & pemeliharaan alat", Icons.Default.Build, Color(0xFF2563EB), Color(0xFFEFF6FF), "menu_pemeliharaan", "Pemeliharaan")
            ).filter { isMenuAllowedForSiswa(it.route) }
        }

        val sirkulasiMenus = remember(userRole, studentPermissions) {
            listOf(
                DashboardMenuData("Peminjaman Alat", "Input barang keluar", Icons.Default.Assignment, Color(0xFF059669), Color(0xFFD1FAE5), "menu_peminjaman", "Peminjaman"),
                DashboardMenuData("Pengembalian Alat", "Input barang kembali", Icons.Default.AssignmentReturn, Color(0xFF4F46E5), Color(0xFFE0E7FF), "menu_pengembalian", "Pengembalian"),
                DashboardMenuData("Kondisi Alat", "Cek kelayakan alat", Icons.Default.Info, Color(0xFFE11D48), Color(0xFFFFE4E6), "menu_kondisi_alat", "Kondisi Alat"),
                DashboardMenuData("Log Transaksi", "Riwayat aktivitas", Icons.Default.CloudSync, Color(0xFF0D9488), Color(0xFFCCFBF1), "menu_log_transaksi", "Log Transaksi")
            ).filter { isMenuAllowedForSiswa(it.route) }
        }

        val analisisMenus = remember(userRole, studentPermissions) {
            listOf(
                DashboardMenuData("Laporan", "Unduh laporan & rekapan", Icons.Default.Assessment, Color(0xFF06B6D4), Color(0xFFECFEFF), "menu_laporan", "Laporan")
            ).filter { isMenuAllowedForSiswa(it.route) }
        }

        // 1. KELOMPOK OPERASIONAL (EXPANDABLE)
        if (operasionalMenus.isNotEmpty()) {
            ExpandableCategoryCard(
                title = "Operasional",
                description = "Kelola ketersediaan barang operasional",
                icon = Icons.Default.Build,
                iconBgColor = Color(0xFFF3E8FF),
                iconTint = Color(0xFF7C3AED),
                isExpanded = isOperasionalExpanded,
                onToggle = { isOperasionalExpanded = !isOperasionalExpanded }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    operasionalMenus.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { item ->
                                GlassMenuCard(
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    icon = item.icon,
                                    iconColor = item.iconColor,
                                    boxBgColor = item.boxBgColor,
                                    testTag = item.testTag,
                                    onClick = { onNavigateToMenu(item.route) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. KELOMPOK SIRKULASI (EXPANDABLE)
        if (sirkulasiMenus.isNotEmpty()) {
            ExpandableCategoryCard(
                title = "Sirkulasi",
                description = "Alur keluar masuk dan kondisi inventaris",
                icon = Icons.Default.CloudSync,
                iconBgColor = Color(0xFFD1FAE5),
                iconTint = Color(0xFF059669),
                isExpanded = isSirkulasiExpanded,
                onToggle = { isSirkulasiExpanded = !isSirkulasiExpanded },
                badgeCount = sirkulasiBadgeCount
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sirkulasiMenus.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { item ->
                                GlassMenuCard(
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    icon = item.icon,
                                    iconColor = item.iconColor,
                                    boxBgColor = item.boxBgColor,
                                    testTag = item.testTag,
                                    onClick = { onNavigateToMenu(item.route) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. KELOMPOK ANALISIS (EXPANDABLE)
        if (analisisMenus.isNotEmpty()) {
            ExpandableCategoryCard(
                title = "Analisis",
                description = "Laporan administrasi dan data rekapan",
                icon = Icons.Default.Assessment,
                iconBgColor = Color(0xFFECFEFF),
                iconTint = Color(0xFF06B6D4),
                isExpanded = isAnalisisExpanded,
                onToggle = { isAnalisisExpanded = !isAnalisisExpanded }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    analisisMenus.forEach { item ->
                        GlassMenuCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            icon = item.icon,
                            iconColor = item.iconColor,
                            boxBgColor = item.boxBgColor,
                            testTag = item.testTag,
                            onClick = { onNavigateToMenu(item.route) },
                            modifier = Modifier.fillMaxWidth(),
                            isFullWidth = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 4. AKTIVITAS TERKINI (5 LOG TRANSAKSI TERAKHIR)
        Text(
            text = "Aktivitas Terkini",
            style = MaterialTheme.typography.titleMedium,
            color = DeepPurpleText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = "5 Transaksi log inventaris terakhir untuk akuntabilitas real-time:",
            style = MaterialTheme.typography.bodySmall,
            color = CarbonBlackText.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LunarisCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recent_activities_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (globalActivities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada aktivitas terekam",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    val displayActivities = if (isAktivitasExpanded) globalActivities.take(5) else globalActivities.take(3)
                    displayActivities.forEachIndexed { index, act ->
                        val isSirkulasi = act.type == "Sirkulasi"
                        val isReturned = act.statusText == "Kembali"
                        
                        val actTitle = if (isSirkulasi) {
                            val itemsList = itemsCache[act.id]
                            val itemsDesc = if (!itemsList.isNullOrEmpty()) {
                                itemsList.joinToString { "${it.namaBarang} (${it.jumlah} Pcs)" }
                            } else {
                                "Aset"
                            }
                            if (isReturned) {
                                "${act.title} mengembalikan $itemsDesc"
                            } else {
                                "${act.title} meminjam $itemsDesc"
                            }
                        } else if (act.type == "Bahan") {
                            "${act.title} mengambil ${act.subtitle}"
                        } else if (act.type == "Input Baru") {
                            "${act.title} (Aset Baru Terdaftar)"
                        } else {
                            "${act.title} (${act.subtitle})"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Circular icon indicator
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(act.statusColor.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = when(act.type) {
                                        "Sirkulasi" -> if (isReturned) Icons.Default.AssignmentReturn else Icons.Default.Assignment
                                        "Bahan" -> Icons.Default.ShoppingCart
                                        "Input Baru" -> Icons.Default.Inventory
                                        else -> Icons.Default.Storage
                                    },
                                    contentDescription = null,
                                    tint = act.statusColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = actTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarbonBlackText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when(act.type) {
                                            "Sirkulasi" -> "Sirkulasi Alat"
                                            "Bahan" -> "Bahan Habis"
                                            "Input Baru" -> "Aset Baru"
                                            "Manajemen" -> "Stok Opname"
                                            else -> act.type
                                        },
                                        fontSize = 10.sp,
                                        color = act.statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(Color.Gray)
                                    )
                                    Text(
                                        text = "${act.date} ${act.time}",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (index < displayActivities.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFFF3F4F6)
                            )
                        }
                    }

                    if (globalActivities.size > 3) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFFF3F4F6)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAktivitasExpanded = !isAktivitasExpanded }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isAktivitasExpanded) "Sembunyikan" else "Lihat Semua",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7C3AED)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. DAILY CHART WITH FILTERS (GRAFIK PEMINJAMAN HARIAN DENGAN FILTER)
        Text(
            text = "Analisis & Statistik",
            style = MaterialTheme.typography.titleMedium,
            color = DeepPurpleText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = "Statistik peminjaman barang inventaris terupdate secara real-time:",
            style = MaterialTheme.typography.bodySmall,
            color = CarbonBlackText.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LunarisCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("analytics_chart_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header of chart card: Title + Selector tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Analisis Aktivitas Peminjaman",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText
                    )
                    
                    // Filter Tab Selector
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("Harian", "Mingguan", "Bulanan").forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF6366F1) else Color.Transparent)
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chart visualization
                val currentData = when (selectedFilter) {
                    "Harian" -> harianChartData
                    "Mingguan" -> mingguanChartData
                    else -> bulananChartData
                }
                val maxVal = currentData.maxOf { it.second }.coerceAtLeast(1f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("canvas_bar_chart")
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val itemCount = currentData.size

                            // 1. Draw premium subtle horizontal gridlines
                            val gridLinesCount = 4
                            for (i in 0 until gridLinesCount) {
                                val y = (canvasHeight / (gridLinesCount - 1)) * i
                                drawLine(
                                    color = Color(0xFFE5E7EB), // Very thin, premium subtle gray line
                                    start = Offset(0f, y),
                                    end = Offset(canvasWidth, y),
                                    strokeWidth = 1f
                                )
                            }

                            // 2. Draw rounded gradient bars
                            val spacing = 28f // comfortable spacing between bars
                            val totalSpacing = spacing * (itemCount - 1)
                            val barWidth = (canvasWidth - totalSpacing) / itemCount

                            currentData.forEachIndexed { index, pair ->
                                val heightFraction = pair.second / maxVal
                                val barHeight = (canvasHeight * heightFraction).coerceAtLeast(8f)
                                val x = index * (barWidth + spacing)
                                val y = canvasHeight - barHeight

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFC7D2FE), // Pastel Lavender (Top)
                                            Color(0xFF93C5FD)  // Pastel Soft Blue (Bottom)
                                        )
                                    ),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(12f, 12f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Labels and values perfectly aligned underneath the canvas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        currentData.forEach { pair ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = pair.second.toInt().toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1) // Indigo accent for value
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = pair.first,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B5563), // Slate-600 label
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. FITUR PERINGATAN BATAS PENGEMBALIAN (DUE SOON & OVERDUE ALERT)
        Text(
            text = "Peringatan Pengembalian",
            style = MaterialTheme.typography.titleMedium,
            color = DeepPurpleText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = "Daftar peminjaman barang yang mendekati tenggat atau terlambat kembali:",
            style = MaterialTheme.typography.bodySmall,
            color = CarbonBlackText.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val displayAlertItems = if (isPengembalianExpanded) alertItems else alertItems.take(5)

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            if (displayAlertItems.isEmpty()) {
                LunarisCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tidak ada peringatan",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak ada peringatan pengembalian",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepPurpleText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Semua sirkulasi peminjaman berjalan tepat waktu.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                displayAlertItems.forEach { item ->
                    // Calculate colors dynamically based on urgency
                    val (iconBgColor, iconColor, borderStrokeColor) = when {
                        item.daysOverdue <= 0 -> {
                            // "Hari Ini" -> Kuning Pastel
                            Triple(
                                Color(0xFFFEF08A), // Light Pastel Yellow (Amber-100)
                                Color(0xFF854D0E), // Amber-800
                                Color(0xFFFDE047)  // Border Amber-300
                            )
                        }
                        item.daysOverdue == 1 -> {
                            // "Terlambat 1 Hari" -> Orange Pastel
                            Triple(
                                Color(0xFFFFEDD5), // Light Pastel Orange (Orange-100)
                                Color(0xFFC2410C), // Orange-700
                                Color(0xFFFDBA74)  // Border Orange-300
                            )
                        }
                        else -> {
                            // "Terlambat >= 2 Hari" -> Red/Pink/Deep Red
                            Triple(
                                Color(0xFFFEE2E2), // Light Red Pastel (Red-100)
                                Color(0xFF991B1B), // Red-800 / Deep Red
                                Color(0xFFFCA5A5)  // Border Red-300
                            )
                        }
                    }

                    val badgeBgColor = when {
                        item.daysOverdue <= 0 -> Color(0xFFFEF9C3) // Soft light yellow tag
                        item.daysOverdue == 1 -> Color(0xFFFFE4E6) // Soft orange/rose tag
                        else -> Color(0xFF991B1B) // Merah Solid/Pekat (Deep Red) for extreme urgency
                    }

                    val badgeTextColor = when {
                        item.daysOverdue <= 0 -> Color(0xFF854D0E)
                        item.daysOverdue == 1 -> Color(0xFF991B1B)
                        else -> Color.White // High contrast for Deep Red
                    }

                    val alertIcon = if (item.daysOverdue <= 0) Icons.Default.AccessTime else Icons.Default.Warning

                    LunarisCard(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAlert = item
                                showAlertDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(iconBgColor)
                            ) {
                                Icon(
                                    imageVector = alertIcon,
                                    contentDescription = item.deadlineLabel,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.namaPeminjam,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFE9D5FF) else DeepPurpleText
                                )
                                Text(
                                    text = item.barangText,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBgColor)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item.deadlineLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor
                                )
                            }
                        }
                    }
                }
            }

            if (alertItems.size > 5) {
                LunarisCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPengembalianExpanded = !isPengembalianExpanded }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPengembalianExpanded) "Sembunyikan" else "Lihat Semua",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Beautiful AlertDialog implementation
        if (showAlertDialog && selectedAlert != null) {
            val item = selectedAlert!!
            AlertDialog(
                onDismissRequest = { showAlertDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFDFBFF),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Detail Alert",
                            tint = Color(0xFF6366F1)
                        )
                        Text(
                            text = "Konfirmasi Pengembalian",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Peminjam: ${item.namaPeminjam}",
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else CarbonBlackText
                        )
                        Text(
                            text = "Barang: ${item.barangText}",
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else CarbonBlackText.copy(alpha = 0.8f)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Status:",
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else CarbonBlackText.copy(alpha = 0.6f)
                            )
                            
                            val overdueColor = if (item.daysOverdue <= 0) Color(0xFF854D0E) else if (item.daysOverdue == 1) Color(0xFFC2410C) else Color.White
                            val overdueBg = if (item.daysOverdue <= 0) Color(0xFFFEF9C3) else if (item.daysOverdue == 1) Color(0xFFFFE4E6) else Color(0xFF991B1B)
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(overdueBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.deadlineLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = overdueColor
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "Tata tertib: Peminjaman barang inventaris harus dikembalikan dalam keadaan baik sesuai waktu kesepakatan. Hubungi peminjam di bawah ini untuk konfirmasi status pengembalian barang.",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showAlertDialog = false
                                onNavigateToMenu("Log Transaksi")
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF6366F1)
                            ),
                            modifier = Modifier.testTag("dialog_detail_button")
                        ) {
                            Text("Lihat Detail")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val isWaAvailable = !item.whatsappNumber.isNullOrBlank()
                        Button(
                            onClick = {
                                if (isWaAvailable) {
                                    showAlertDialog = false
                                    try {
                                        val message = "Halo ${item.namaPeminjam}, kami dari sarpras ingin mengingatkan bahwa ${item.barangText} yang Anda pinjam telah melewati batas pengembalian (${item.deadlineLabel}). Harap segera mengembalikannya ke gudang. Terima kasih!"
                                        val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=${item.whatsappNumber}&text=$encodedMsg")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal membuka WhatsApp", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = isWaAvailable,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE5E7EB),
                                disabledContentColor = Color(0xFF9CA3AF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dialog_whatsapp_button")
                        ) {
                            Text(
                                text = if (isWaAvailable) "Hubungi via WhatsApp" else "WhatsApp Tidak Tersedia",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Text(
                    text = "🥺",
                    fontSize = 36.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            },
            title = {
                Text(
                    text = "Mau pergi ya? 🥺",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Kami akan merindukanmu di Lunaris...",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Keluar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // DYNAMIC FLOATING ACTION BUTTON (FAB)
    val fabText = when {
        userRole == "siswa" -> "Scan QR"
        isSirkulasiExpanded -> "Tambah Peminjaman"
        isOperasionalExpanded -> "Tambah Alat"
        else -> "Scan QR"
    }
    val fabIcon = when {
        userRole == "siswa" -> Icons.Default.QrCodeScanner
        isSirkulasiExpanded -> Icons.Default.Assignment
        isOperasionalExpanded -> Icons.Default.Build
        else -> Icons.Default.QrCodeScanner
    }
    val fabRoute = when {
        userRole == "siswa" -> "Scan QR"
        isSirkulasiExpanded -> "Peminjaman"
        isOperasionalExpanded -> "Alat"
        else -> "Scan QR"
    }

    ExtendedFloatingActionButton(
        onClick = { onNavigateToMenu(fabRoute) },
        icon = { Icon(imageVector = fabIcon, contentDescription = fabText, tint = Color.White) },
        text = { Text(text = fabText, fontWeight = FontWeight.Bold, color = Color.White) },
        containerColor = Color(0xFF7C3AED), // Premium purple
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(24.dp)
            .testTag("dynamic_fab")
    )

    if (showProfileBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showProfileBottomSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            tonalElevation = 8.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFE5E7EB))
                )
            },
            modifier = Modifier.testTag("profile_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profil Petugas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF3B0764),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Large Profile Avatar with Status indicator
                Box(
                    modifier = Modifier.size(90.dp)
                ) {
                    if (userProfileBitmap != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, if (isDark) Color(0x66A78BFA) else Color(0xFFE9D5FF), CircleShape)
                        ) {
                            Image(
                                bitmap = userProfileBitmap,
                                contentDescription = "Foto Profil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else if (userProfilePhoto.isNotBlank()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, if (isDark) Color(0x66A78BFA) else Color(0xFFE9D5FF), CircleShape)
                        ) {
                            AsyncImage(
                                model = userProfilePhoto,
                                contentDescription = "Foto Profil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x33A78BFA) else Color(0xFFF3E8FF))
                                .border(2.dp, if (isDark) Color(0x66A78BFA) else Color(0xFFE9D5FF), CircleShape)
                        ) {
                            Text(
                                text = if (namaPetugas.isNotEmpty()) namaPetugas.take(1).uppercase() else "A",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED),
                                fontSize = 32.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = namaPetugas,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF3B0764)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = officerNipText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = instansiName.ifBlank { "Gudang Utama Lunaris" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8B5CF6),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            showProfileBottomSheet = false
                            scope.launch {
                                drawerState.close()
                                onNavigateToMenu("Profil")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF5F3FF),
                            contentColor = Color(0xFF7C3AED)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp)
                            .testTag("btn_edit_profil"),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Edit Profil",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit Profil",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            showProfileBottomSheet = false
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF3F4F6),
                            contentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF4B5563)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp)
                            .testTag("btn_kembali_profil"),
                        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else Color(0xFFD1D5DB))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kembali",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}
}
}

@Composable
fun DrawerMenuItem(
    label: String,
    icon: ImageVector,
    iconColor: Color = Color(0xFF7C3AED),
    textColor: Color = Color(0xFF3B0764),
    bgColor: Color = Color(0xFFF5F3FF),
    borderColor: Color = Color(0xFFE9D5FF),
    onClick: () -> Unit
) {
    val isDark = false
    val finalBgColor = if (isDark) {
        if (label == "Keluar") Color(0x33EF4444) else MaterialTheme.colorScheme.surfaceVariant
    } else {
        bgColor
    }
    val finalBorderColor = if (isDark) {
        if (label == "Keluar") Color(0x66EF4444) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    } else {
        borderColor
    }
    val finalIconColor = if (isDark) {
        if (label == "Keluar") Color(0xFFFCA5A5) else MaterialTheme.colorScheme.primary
    } else {
        iconColor
    }
    val finalTextColor = if (isDark) {
        if (label == "Keluar") Color(0xFFFCA5A5) else MaterialTheme.colorScheme.onSurface
    } else {
        textColor
    }

    LunarisCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = finalBgColor),
        border = BorderStroke(1.dp, finalBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = finalIconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = finalTextColor
            )
        }
    }
}

data class MenuGridItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val testTag: String,
    val subtitle: String
)

data class AlertItem(
    val idTransaksi: String?,
    val namaPeminjam: String,
    val barangText: String,
    val tanggal: String,
    val deadlineLabel: String,
    val daysOverdue: Int,
    val whatsappNumber: String?
)

@Composable
fun GradientMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    iconColor: Color,
    boxBgColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = false
    LunarisCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .aspectRatio(1.35f)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = true)
            .background(Brush.linearGradient(gradientColors), RoundedCornerShape(24.dp))
            .border(1.dp, if (isDark) Color(0x33A78BFA) else Color(0xFFE9D5FF), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(boxBgColor)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (isDark) Color(0xFFF3E8FF) else Color(0xFF3B0764)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ExpandableCategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int? = null,
    content: @Composable () -> Unit
) {
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrow_rotation")
    val isDark = false
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
    val borderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText
    val descColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else CarbonBlackText.copy(alpha = 0.65f)
    val iconTintResolved = if (isDark) MaterialTheme.colorScheme.primary else iconTint
    val iconBgResolved = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else iconBgColor

    LunarisCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // FLAT (no shadows)
        border = BorderStroke(1.dp, borderColor), // Symmetrical thin clean border
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBgResolved)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTintResolved,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Title & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = titleColor
                        )
                        if (badgeCount != null && badgeCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = descColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Expanding indicator
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = titleColor.copy(alpha = 0.7f),
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                // Nested content (sub-menus)
                content()
            }
        }
    }
}

@Composable
fun GlassMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    boxBgColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = false
) {
    val isDark = false
    val resolvedContainerBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    } else {
        boxBgColor.copy(alpha = 0.85f)
    }
    val resolvedBorderColor = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    } else {
        iconColor.copy(alpha = 0.15f)
    }
    val resolvedTitleColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        iconColor
    }
    val resolvedSubtitleColor = if (isDark) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFF334155)
    }
    val resolvedIconBg = if (isDark) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.White
    }
    val resolvedIconColor = if (isDark) {
        MaterialTheme.colorScheme.primary
    } else {
        iconColor
    }

    LunarisCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .then(if (isFullWidth) Modifier.height(88.dp) else Modifier.aspectRatio(1.35f))
            .background(resolvedContainerBg, RoundedCornerShape(24.dp))
            .border(1.dp, resolvedBorderColor, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        if (isFullWidth) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(resolvedIconBg)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = resolvedIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = resolvedTitleColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = resolvedSubtitleColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(resolvedIconBg)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = resolvedIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = resolvedTitleColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = resolvedSubtitleColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun GlassSmallCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    boxBgColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = false
    val resolvedContainerBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }
    val resolvedBorderColor = if (isDark) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    } else {
        Color(0xFFE9D5FF)
    }
    val resolvedTitleColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFF3B0764)
    }
    val resolvedIconBg = if (isDark) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        boxBgColor
    }
    val resolvedIconColor = if (isDark) {
        MaterialTheme.colorScheme.primary
    } else {
        iconColor
    }

    LunarisCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = true)
            .background(resolvedContainerBg, RoundedCornerShape(24.dp))
            .border(1.dp, resolvedBorderColor, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(resolvedIconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = resolvedIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = resolvedTitleColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class SmallMenuData(
    val title: String,
    val icon: ImageVector,
    val testTag: String,
    val iconColor: Color,
    val boxBgColor: Color,
    val route: String
)

data class DashboardMenuData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val boxBgColor: Color,
    val testTag: String,
    val route: String
)
