package com.example.ui.screens

import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField
import com.example.ui.components.FilterGroup
import com.example.ui.components.LunarisFilterDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.DeepPurpleText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogTransaksiScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = false
    val topBarGradient = if (isDark) {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surface
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF3B82F6).copy(alpha = 0.9f),
                Color(0xFF2DD4BF).copy(alpha = 0.9f)
            )
        )
    }
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
    val appBarContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    // Data flow
    val localTransactions by viewModel.allTransactions.collectAsState()
    val pemakaianBahan by viewModel.allPemakaianBahan.collectAsState()
    val bahanAfkir by viewModel.allBahanAfkir.collectAsState()

    var recentFirestoreTransactions by remember { mutableStateOf<List<LoanTransactionEntity>>(emptyList()) }

    DisposableEffect(Unit) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val listener = firestore.collection("transactions")
            .orderBy("tanggal", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .orderBy("waktu", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("LogTransaksiScreen", "Error listening to transactions", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<LoanTransactionEntity>()
                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val namaPeminjam = doc.getString("namaPeminjam") ?: ""
                        val status = doc.getString("status") ?: "Dipinjam"
                        val tanggal = doc.getString("tanggal") ?: ""
                        val waktu = doc.getString("waktu") ?: ""
                        val kelasVal = doc.getString("kelas") ?: ""
                        val kondisiVal = doc.getString("kondisi") ?: ""
                        val namaPetugasVal = doc.getString("namaPetugas") ?: ""
                        val durasiVal = doc.getLong("durasiHari")?.toInt() ?: 1
                        val isDemoVal = doc.getLong("isDemo")?.toInt() == 1
                        val whatsapp = doc.getString("whatsappNumber")
                        val tujuan = doc.getString("tujuanPeminjaman")
                        val detail = doc.getString("detailTujuan")
                        val tanggalKembaliVal = doc.getString("tanggalKembali")
                        val waktuKembaliVal = doc.getString("waktuKembali")
                        val kondisiKembaliVal = doc.getString("kondisiKembali")
                        val petugasKembaliVal = doc.getString("petugasKembali")
                        val keteranganKerusakanVal = doc.getString("keteranganKerusakan")
                        
                        list.add(
                            LoanTransactionEntity(
                                idTransaksi = id,
                                tanggal = tanggal,
                                namaPeminjam = namaPeminjam,
                                kelas = kelasVal,
                                waktu = waktu,
                                kondisi = kondisiVal,
                                namaPetugas = namaPetugasVal,
                                status = status,
                                durasiHari = durasiVal,
                                isDemo = isDemoVal,
                                whatsappNumber = whatsapp,
                                tujuanPeminjaman = tujuan,
                                detailTujuan = detail,
                                tanggalKembali = tanggalKembaliVal,
                                waktuKembali = waktuKembaliVal,
                                kondisiKembali = kondisiKembaliVal,
                                petugasKembali = petugasKembaliVal,
                                keteranganKerusakan = keteranganKerusakanVal
                            )
                        )
                    }
                    recentFirestoreTransactions = list
                }
            }
        onDispose {
            listener.remove()
        }
    }

    val transactions = remember(recentFirestoreTransactions, localTransactions) {
        val list = mutableListOf<LoanTransactionEntity>()
        val localMap = localTransactions.associateBy { it.idTransaksi }
        
        // 1. Process real-time loan transactions from Firestore merged with local DB
        recentFirestoreTransactions.forEach { firestoreTx ->
            val localTx = localMap[firestoreTx.idTransaksi]
            if (localTx != null) {
                val mergedStatus = if (localTx.status == "Kembali" || firestoreTx.status == "Kembali") "Kembali" else firestoreTx.status
                val mergedTglKmb = firestoreTx.tanggalKembali.takeIf { !it.isNullOrBlank() } ?: localTx.tanggalKembali
                val mergedWktKmb = firestoreTx.waktuKembali.takeIf { !it.isNullOrBlank() } ?: localTx.waktuKembali
                val mergedKndKmb = firestoreTx.kondisiKembali.takeIf { !it.isNullOrBlank() } ?: localTx.kondisiKembali
                val mergedPtgKmb = firestoreTx.petugasKembali.takeIf { !it.isNullOrBlank() } ?: localTx.petugasKembali
                val mergedKet = firestoreTx.keteranganKerusakan.takeIf { !it.isNullOrBlank() } ?: localTx.keteranganKerusakan

                list.add(
                    firestoreTx.copy(
                        status = mergedStatus,
                        tanggalKembali = mergedTglKmb,
                        waktuKembali = mergedWktKmb,
                        kondisiKembali = mergedKndKmb,
                        petugasKembali = mergedPtgKmb,
                        keteranganKerusakan = mergedKet
                    )
                )
            } else {
                list.add(firestoreTx)
            }
        }
        
        // 2. Add local transactions that are not in Firestore list
        val firestoreIds = recentFirestoreTransactions.map { it.idTransaksi }.toSet()
        localTransactions.forEach { localTx ->
            if (!firestoreIds.contains(localTx.idTransaksi)) {
                list.add(localTx)
            }
        }
        
        list.sortedWith(compareByDescending<LoanTransactionEntity> { it.tanggal }.thenByDescending { it.waktu })
    }

    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun isTabAllowed(index: Int): Boolean {
        if (!userRole.contains("siswa", ignoreCase = true)) return true
        val key = when(index) {
            0 -> "log_sirkulasi"
            1 -> "log_bahan_habis"
            2 -> "log_stok"
            3 -> "log_pemeliharaan"
            4 -> "log_aktivitas"
            else -> "log_sirkulasi"
        }
        return studentPermissions[key] == true
    }

    // 5 Categories Tier 1 Tab State
    val mainCategories = listOf(
        "Sirkulasi Alat",
        "Bahan Habis",
        "Manajemen Stok",
        "Pemeliharaan",
        "Aktivitas Sistem"
    )
    var selectedMainCategoryIndex by remember { mutableStateOf(0) }

    LaunchedEffect(userRole, studentPermissions) {
        if (userRole.contains("siswa", ignoreCase = true) && !isTabAllowed(selectedMainCategoryIndex)) {
            val nextAllowed = (0..4).firstOrNull { isTabAllowed(it) }
            if (nextAllowed != null) {
                selectedMainCategoryIndex = nextAllowed
            }
        }
    }

    // Tier 2 Sub-Tabs States
    var subTabSirkulasi by remember { mutableStateOf(0) } // 0=Semua, 1=Dipinjam, 2=Kembali, 3=Terlambat
    var subTabBahan by remember { mutableStateOf(0) } // 0=Pemakaian, 1=Restock Baru, 2=Kedaluwarsa

    // Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var tempSelectedClass by remember { mutableStateOf("Semua Kelas") }
    var tempSelectedCondition by remember { mutableStateOf("Semua Kondisi") }
    var tempSelectedOfficer by remember { mutableStateOf("Semua Petugas") }
    
    var appliedClass by remember { mutableStateOf("Semua Kelas") }
    var appliedCondition by remember { mutableStateOf("Semua Kondisi") }
    var appliedOfficer by remember { mutableStateOf("Semua Petugas") }

    // Keep a local cache of items per transaction since database queries are asynchronous
    val itemsCache = remember { mutableStateMapOf<String, List<LoanItemEntity>>() }

    // Query item details on display
    LaunchedEffect(transactions) {
        transactions.forEach { tx ->
            if (!itemsCache.containsKey(tx.idTransaksi)) {
                val list = viewModel.getItemsForTransaction(tx.idTransaksi)
                itemsCache[tx.idTransaksi] = list
            }
        }
    }

    // Filter Options for global dialog
    val classOptions = remember(transactions) {
        val uniqueClasses = transactions.map { it.kelas }.filter { it.isNotEmpty() && !it.contains("Sistem") }.distinct().sorted()
        listOf("Semua Kelas") + uniqueClasses
    }
    val conditionTxOptions = listOf("Semua Kondisi", "Baik", "Rusak")
    val officerOptions = remember(transactions) {
        val uniqueOfficers = transactions.map { it.namaPetugas }.filter { it.isNotEmpty() }.distinct().sorted()
        listOf("Semua Petugas") + uniqueOfficers
    }

    // 1. FILTERING Sirkulasi Peminjaman (Rumpun A)
    val lendingTransactions = remember(transactions) {
        transactions.filter { tx ->
            !tx.idTransaksi.startsWith("TX-SYN") &&
            !tx.idTransaksi.startsWith("TX-INP") &&
            !tx.idTransaksi.startsWith("TX-OPN") &&
            !tx.idTransaksi.startsWith("TX-RUM") &&
            !tx.idTransaksi.startsWith("TX-DMG") &&
            !tx.idTransaksi.startsWith("TX-AFK")
        }
    }
    val sirkulasiFiltered = remember(lendingTransactions, subTabSirkulasi, searchQuery, appliedClass, appliedCondition, appliedOfficer) {
        lendingTransactions.filter { tx ->
            val matchesSearch = tx.namaPeminjam.contains(searchQuery, ignoreCase = true) ||
                    tx.kelas.contains(searchQuery, ignoreCase = true) ||
                    tx.idTransaksi.contains(searchQuery, ignoreCase = true)
            
            val matchesSub = when (subTabSirkulasi) {
                1 -> tx.status == "Dipinjam" && !isOverdue(tx.tanggal)
                2 -> tx.status == "Kembali"
                3 -> tx.status == "Dipinjam" && isOverdue(tx.tanggal)
                else -> true
            }
            
            val matchesClass = appliedClass == "Semua Kelas" || tx.kelas == appliedClass
            val matchesCondition = appliedCondition == "Semua Kondisi" || tx.kondisi == appliedCondition
            val matchesOfficer = appliedOfficer == "Semua Petugas" || tx.namaPetugas == appliedOfficer
            
            matchesSearch && matchesSub && matchesClass && matchesCondition && matchesOfficer
        }
    }

    // 2. FILTERING Bahan Habis Pakai (Rumpun B)
    // Sub-tab 0: Riwayat Pemakaian
    val filteredPemakaian = remember(pemakaianBahan, searchQuery) {
        pemakaianBahan.filter { pmk ->
            pmk.namaPeminta.contains(searchQuery, ignoreCase = true) ||
            pmk.namaBarang.contains(searchQuery, ignoreCase = true) ||
            pmk.idPemakaian.contains(searchQuery, ignoreCase = true) ||
            (pmk.kelas?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }
    // Sub-tab 1: Restock/Pembelian Baru (System Input & Opname of Logistik items)
    val restockTransactions = remember(transactions) {
        transactions.filter { tx ->
            (tx.idTransaksi.startsWith("TX-INP") || tx.idTransaksi.startsWith("TX-OPN")) &&
            (tx.namaPeminjam.contains("Bahan", ignoreCase = true) || 
             tx.namaPeminjam.contains("Tinta", ignoreCase = true) || 
             tx.namaPeminjam.contains("Kertas", ignoreCase = true) || 
             tx.namaPeminjam.contains("Printer", ignoreCase = true) ||
             tx.keteranganKerusakan?.contains("Bahan", ignoreCase = true) == true ||
             tx.keteranganKerusakan?.contains("Logistik", ignoreCase = true) == true)
        }
    }
    val filteredRestock = remember(restockTransactions, searchQuery) {
        restockTransactions.filter { tx ->
            tx.namaPeminjam.contains(searchQuery, ignoreCase = true) ||
            tx.idTransaksi.contains(searchQuery, ignoreCase = true)
        }
    }
    // Sub-tab 2: Kedaluwarsa/Expired (Bahan Afkir / Expired reasons)
    val expiredMaterials = remember(bahanAfkir) {
        bahanAfkir.filter { afk ->
            afk.alasan.contains("Kedaluwarsa", ignoreCase = true) ||
            afk.alasan.contains("Expired", ignoreCase = true) ||
            afk.alasan.contains("Afkir", ignoreCase = true)
        }
    }
    val filteredExpired = remember(expiredMaterials, searchQuery) {
        expiredMaterials.filter { afk ->
            afk.namaBarang.contains(searchQuery, ignoreCase = true) ||
            afk.idAfkir.contains(searchQuery, ignoreCase = true)
        }
    }

    // 3. FILTERING Log Manajemen Stok Aset
    val stokTransactions = remember(transactions) {
        transactions.filter { tx ->
            tx.idTransaksi.startsWith("TX-INP") ||
            tx.idTransaksi.startsWith("TX-OPN") ||
            tx.idTransaksi.startsWith("TX-RUM")
        }
    }
    val filteredStokLog = remember(stokTransactions, searchQuery) {
        stokTransactions.filter { tx ->
            tx.namaPeminjam.contains(searchQuery, ignoreCase = true) ||
            tx.idTransaksi.contains(searchQuery, ignoreCase = true) ||
            (tx.keteranganKerusakan?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    // 4. FILTERING Log Kondisi & Pemeliharaan
    val pemeliharaanTransactions = remember(transactions) {
        transactions.filter { tx ->
            tx.idTransaksi.startsWith("TX-DMG") ||
            tx.idTransaksi.startsWith("TX-AFK")
        }
    }
    val filteredPemeliharaanLog = remember(pemeliharaanTransactions, searchQuery) {
        pemeliharaanTransactions.filter { tx ->
            tx.namaPeminjam.contains(searchQuery, ignoreCase = true) ||
            tx.idTransaksi.contains(searchQuery, ignoreCase = true) ||
            (tx.keteranganKerusakan?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    // 5. FILTERING Log Aktivitas Sistem
    val sistemTransactions = remember(transactions) {
        transactions.filter { tx ->
            tx.idTransaksi.startsWith("TX-SYN") ||
            tx.idTransaksi.startsWith("TX-AFK-DEL") ||
            tx.idTransaksi.startsWith("TX-DMG-DEL")
        }
    }
    val filteredSistemLog = remember(sistemTransactions, searchQuery) {
        sistemTransactions.filter { tx ->
            tx.namaPeminjam.contains(searchQuery, ignoreCase = true) ||
            tx.idTransaksi.contains(searchQuery, ignoreCase = true) ||
            (tx.keteranganKerusakan?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Main Header
                Box(
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
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("log_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Kembali",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Log Terpadu & Audit",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Tier 1: Scrollable Main Category Tab Row (5 Categories)
                ScrollableTabRow(
                    selectedTabIndex = selectedMainCategoryIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Color(0xFF7C3AED),
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedMainCategoryIndex]),
                            height = 3.dp,
                            color = Color(0xFF7C3AED)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    mainCategories.forEachIndexed { index, title ->
                        val allowed = isTabAllowed(index)
                        Tab(
                            selected = selectedMainCategoryIndex == index,
                            onClick = {
                                if (allowed) {
                                    selectedMainCategoryIndex = index
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Sub-menu '$title' terkunci untuk akun Siswa",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            selectedContentColor = Color(0xFF7C3AED),
                            unselectedContentColor = if (allowed) Color.Gray else Color.LightGray,
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!allowed) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Terkunci",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedMainCategoryIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (!allowed) Color.Gray.copy(alpha = 0.6f) else Color.Unspecified,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            },
                            modifier = Modifier.testTag("main_tab_$index")
                        )
                    }
                }

                // Tier 2: Sub-tabs (Only if Sirkulasi or Bahan Habis is selected)
                if (selectedMainCategoryIndex == 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Sub-tabs for Sirkulasi Alat
                    TabRow(
                        selectedTabIndex = subTabSirkulasi,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[subTabSirkulasi]),
                                height = 2.dp,
                                color = Color(0xFF7C3AED)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val subTabs = listOf("Semua", "Dipinjam", "Kembali", "Terlambat")
                        subTabs.forEachIndexed { sIdx, sTitle ->
                            Tab(
                                selected = subTabSirkulasi == sIdx,
                                onClick = { subTabSirkulasi = sIdx },
                                selectedContentColor = Color(0xFF7C3AED),
                                unselectedContentColor = Color.Gray,
                                text = {
                                    Text(
                                        text = sTitle,
                                        fontSize = 12.sp,
                                        fontWeight = if (subTabSirkulasi == sIdx) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                modifier = Modifier.testTag("sub_tab_sirkulasi_$sIdx")
                            )
                        }
                    }
                } else if (selectedMainCategoryIndex == 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Sub-tabs for Bahan Habis Pakai
                    TabRow(
                        selectedTabIndex = subTabBahan,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[subTabBahan]),
                                height = 2.dp,
                                color = Color(0xFF7C3AED)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val subTabs = listOf("Pemakaian", "Restock Baru", "Kedaluwarsa")
                        subTabs.forEachIndexed { sIdx, sTitle ->
                            Tab(
                                selected = subTabBahan == sIdx,
                                onClick = { subTabBahan = sIdx },
                                selectedContentColor = Color(0xFF7C3AED),
                                unselectedContentColor = Color.Gray,
                                text = {
                                    Text(
                                        text = sTitle,
                                        fontSize = 12.sp,
                                        fontWeight = if (subTabBahan == sIdx) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                modifier = Modifier.testTag("sub_tab_bahan_$sIdx")
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            // Search bar row (with Purple static outlined Filter button on the right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LunarisTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            when(selectedMainCategoryIndex) {
                                0 -> "Cari peminjam, kelas, kode..."
                                1 -> "Cari peminta, bahan, kode..."
                                2 -> "Cari aset, penyesuaian..."
                                3 -> "Cari riwayat servis/afkir..."
                                else -> "Cari aktivitas sync/delete..."
                            }
                        ) 
                    },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Cari") },
                    trailingIcon = {
                        IconButton(onClick = { showQrScanner = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = Color(0xFF7C3AED)
                            )
                        }
                    },
                    singleLine = true,
                    isStaticOutline = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7C3AED)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_log_transaksi")
                )
                
                // Show Filter dialog button only for Sirkulasi Alat Category
                if (selectedMainCategoryIndex == 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .background(
                                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { showFilterDialog = true }
                            .testTag("transaksi_filter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED)
                        )
                    }
                }
            }

            if (showQrScanner) {
                SearchQrScanDialog(
                    onDismiss = { showQrScanner = false },
                    onQrScanned = { scannedCode ->
                        showQrScanner = false
                        searchQuery = scannedCode
                    }
                )
            }

            // Decide current list based on Main Category selection
            when (selectedMainCategoryIndex) {
                0 -> { // Category 0: Sirkulasi Alat
                    RenderSirkulasiList(
                        list = sirkulasiFiltered,
                        itemsCache = itemsCache,
                        searchQuery = searchQuery,
                        isDark = isDark
                    )
                }
                1 -> { // Category 1: Bahan Habis Pakai
                    when (subTabBahan) {
                        0 -> RenderPemakaianBahanList(filteredPemakaian, searchQuery, isDark)
                        1 -> RenderRestockList(filteredRestock, searchQuery, isDark)
                        2 -> RenderExpiredList(filteredExpired, searchQuery, isDark)
                    }
                }
                2 -> { // Category 2: Log Manajemen Stok
                    RenderGenericAuditList(
                        list = filteredStokLog,
                        searchQuery = searchQuery,
                        categoryTitle = "Log Manajemen Stok",
                        icon = Icons.Default.Storage,
                        iconColor = Color(0xFF3B82F6),
                        isDark = isDark
                    )
                }
                3 -> { // Category 3: Log Kondisi & Pemeliharaan
                    RenderGenericAuditList(
                        list = filteredPemeliharaanLog,
                        searchQuery = searchQuery,
                        categoryTitle = "Kondisi & Pemeliharaan",
                        icon = Icons.Default.Build,
                        iconColor = Color(0xFFF59E0B),
                        isDark = isDark
                    )
                }
                4 -> { // Category 4: Log Aktivitas Sistem
                    RenderGenericAuditList(
                        list = filteredSistemLog,
                        searchQuery = searchQuery,
                        categoryTitle = "Aktivitas Sistem & Audit",
                        icon = Icons.Default.Sync,
                        iconColor = Color(0xFF8B5CF6),
                        isDark = isDark
                    )
                }
            }
            
            if (showFilterDialog) {
                LunarisFilterDialog(
                    onDismissRequest = {
                        tempSelectedClass = appliedClass
                        tempSelectedCondition = appliedCondition
                        tempSelectedOfficer = appliedOfficer
                        showFilterDialog = false
                    },
                    filterGroups = listOf(
                        FilterGroup(
                            title = "Kelas / Lokasi",
                            options = classOptions,
                            selectedOption = tempSelectedClass,
                            onOptionSelected = { tempSelectedClass = it }
                        ),
                        FilterGroup(
                            title = "Kondisi",
                            options = conditionTxOptions,
                            selectedOption = tempSelectedCondition,
                            onOptionSelected = { tempSelectedCondition = it }
                        ),
                        FilterGroup(
                            title = "Nama Petugas",
                            options = officerOptions,
                            selectedOption = tempSelectedOfficer,
                            onOptionSelected = { tempSelectedOfficer = it }
                        )
                    ),
                    onReset = {
                        tempSelectedClass = "Semua Kelas"
                        tempSelectedCondition = "Semua Kondisi"
                        tempSelectedOfficer = "Semua Petugas"
                    },
                    onApply = {
                        appliedClass = tempSelectedClass
                        appliedCondition = tempSelectedCondition
                        appliedOfficer = tempSelectedOfficer
                        showFilterDialog = false
                    }
                )
            }
        }
    }
}

// Sub Composable: Render Sirkulasi Peminjaman Aset (Category 0)
@Composable
fun RenderSirkulasiList(
    list: List<LoanTransactionEntity>,
    itemsCache: Map<String, List<LoanItemEntity>>,
    searchQuery: String,
    isDark: Boolean
) {
    if (list.isEmpty()) {
        EmptyLogsPlaceholder(searchQuery, "Belum ada transaksi sirkulasi terekam.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxWidth().testTag("log_transaksi_list")
        ) {
            items(list) { tx ->
                val lines = itemsCache[tx.idTransaksi] ?: emptyList()
                LunarisCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8F7FC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        val isReturned = tx.status == "Kembali"
                        val isLate = tx.status == "Dipinjam" && isOverdue(tx.tanggal)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tx.idTransaksi,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val badgeTextColor = when {
                                isReturned -> Color(0xFF115E59)
                                isLate -> Color(0xFF991B1B)
                                else -> Color(0xFF854D0E)
                            }
                            val badgeText = when {
                                isReturned -> "Kembali"
                                isLate -> "Terlambat"
                                else -> "Dipinjam"
                            }
                            Text(
                                text = badgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = tx.namaPeminjam,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kelas: ${tx.kelas}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = "Pinjam",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Detail Peminjaman",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tanggal: ${tx.tanggal} • Waktu: ${tx.waktu} WIB",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Kondisi Pinjam: ${tx.kondisi}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Petugas Pinjam: ${tx.namaPetugas}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!tx.tujuanPeminjaman.isNullOrBlank()) {
                                    Text(
                                        text = "Tujuan Peminjaman: ${tx.tujuanPeminjaman}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!tx.detailTujuan.isNullOrBlank()) {
                                    val detailLabel = if (tx.tujuanPeminjaman == "Kegiatan Belajar Mengajar (KBM)") {
                                        "Guru Pengampu / Mapel"
                                    } else {
                                        "Detail Kegiatan"
                                    }
                                    Text(
                                        text = "$detailLabel: ${tx.detailTujuan}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (lines.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Barang Dipinjam:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                lines.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• ${item.namaBarang} (ID: ${item.idBarang})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${item.jumlah} unit",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (tx.status == "Kembali") Icons.Default.AssignmentReturned else Icons.Default.PendingActions,
                                contentDescription = "Detail Pengembalian",
                                tint = if (tx.status == "Kembali") Color(0xFF115E59) else Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Detail Pengembalian",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.status == "Kembali") Color(0xFF115E59) else Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (tx.status == "Kembali") {
                                    val tglKmb = tx.tanggalKembali.takeIf { !it.isNullOrBlank() } ?: tx.tanggal
                                    val wktKmb = tx.waktuKembali.takeIf { !it.isNullOrBlank() } ?: tx.waktu
                                    val kndKmb = tx.kondisiKembali.takeIf { !it.isNullOrBlank() } ?: "Normal / Baik"
                                    val ptgKmb = tx.petugasKembali.takeIf { !it.isNullOrBlank() } ?: tx.namaPetugas

                                    Text(
                                        text = "Tanggal: $tglKmb • Waktu: $wktKmb WIB",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Kondisi Kembali: $kndKmb",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Petugas Penerima: $ptgKmb",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!tx.keteranganKerusakan.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Rincian / Catatan Pengembalian:\n${tx.keteranganKerusakan}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Status: Belum Dikembalikan",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }

                        if (isLate) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Button(
                                    onClick = {
                                        val phoneNum = tx.whatsappNumber ?: "6285600005719"
                                        val message = "Halo ${tx.namaPeminjam}, kami dari sarpras ingin mengingatkan bahwa peminjaman ${tx.idTransaksi} telah melewati batas pengembalian. Harap segera mengembalikannya ke gudang. Terima kasih!"
                                        val encodedMsg = URLEncoder.encode(message, "UTF-8")
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("https://wa.me/$phoneNum?text=$encodedMsg")
                                        }
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp).testTag("wa_remind_button_log")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "WA",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub Composable: Render Riwayat Pemakaian Bahan (Category 1 Sub-Tab 0)
@Composable
fun RenderPemakaianBahanList(
    list: List<com.example.data.entity.PemakaianBahanEntity>,
    searchQuery: String,
    isDark: Boolean
) {
    if (list.isEmpty()) {
        EmptyLogsPlaceholder(searchQuery, "Belum ada pemakaian bahan terekam.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(list) { pmk ->
                LunarisCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFF7ED)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pmk.idPemakaian,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C)
                            )
                            Text(
                                text = "Pemakaian",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEA580C)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = pmk.namaPeminta,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Jabatan: ${pmk.jabatan}" + if (pmk.kelas != null) " • Kelas: ${pmk.kelas}" else "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(thickness = 0.5.dp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = pmk.namaBarang,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ID: ${pmk.idBarang}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${pmk.jumlahDiambil} ${pmk.satuan}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEA580C)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Catatan: ${pmk.keterangan.ifBlank { "-" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tanggal: ${pmk.tanggalPemakaian}",
                                fontSize = 11.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                            )
                            Text(
                                text = "Petugas: ${pmk.namaPetugas}",
                                fontSize = 11.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Sub Composable: Render Restock/Pembelian Baru (Category 1 Sub-Tab 1)
@Composable
fun RenderRestockList(
    list: List<LoanTransactionEntity>,
    searchQuery: String,
    isDark: Boolean
) {
    if (list.isEmpty()) {
        EmptyLogsPlaceholder(searchQuery, "Belum ada transaksi restock terekam.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(list) { tx ->
                LunarisCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF0FDF4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tx.idTransaksi,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = if (tx.idTransaksi.contains("OPN")) "Opname" else "Restock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = tx.namaPeminjam,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tx.keteranganKerusakan ?: "Pendaftaran stok baru bahan habis pakai.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tanggal: ${tx.tanggal} • ${tx.waktu}",
                                fontSize = 11.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                            )
                            Text(
                                text = "Oleh: ${tx.namaPetugas}",
                                fontSize = 11.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Sub Composable: Render Kedaluwarsa/Afkir (Category 1 Sub-Tab 2)
@Composable
fun RenderExpiredList(
    list: List<com.example.data.entity.BahanAfkirEntity>,
    searchQuery: String,
    isDark: Boolean
) {
    if (list.isEmpty()) {
        EmptyLogsPlaceholder(searchQuery, "Belum ada bahan kedaluwarsa terekam.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(list) { afk ->
                LunarisCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFEF2F2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = afk.idAfkir,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "Kedaluwarsa/Afkir",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFDC2626)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = afk.namaBarang,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bahan ID: ${afk.idBarang} • Jumlah: ${afk.jumlahAfkir} ${afk.satuan}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Alasan: ${afk.alasan}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB91C1C)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tanggal: ${afk.tanggalAfkir}",
                                fontSize = 11.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// Sub Composable: Render Generic System Audit Logs (Category 2, 3, 4)
@Composable
fun RenderGenericAuditList(
    list: List<LoanTransactionEntity>,
    searchQuery: String,
    categoryTitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isDark: Boolean
) {
    if (list.isEmpty()) {
        EmptyLogsPlaceholder(searchQuery, "Belum ada log terekam untuk kategori ini.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(list) { tx ->
                LunarisCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(iconColor.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.idTransaksi,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iconColor
                                )
                                Text(
                                    text = tx.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = iconColor
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = tx.namaPeminjam,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tx.keteranganKerusakan ?: "Audit aktivitas log sistem.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${tx.tanggal} • ${tx.waktu}",
                                    fontSize = 11.sp,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                )
                                Text(
                                    text = "Oleh: ${tx.namaPetugas}",
                                    fontSize = 11.sp,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Empty placeholder UI
@Composable
fun EmptyLogsPlaceholder(searchQuery: String, defaultMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Kosong",
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "Hasil pencarian tidak ditemukan." else defaultMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

private fun isOverdue(tanggalTransaksi: String): Boolean {
    try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val transDate = sdf.parse(tanggalTransaksi) ?: return false
        val calTrans = java.util.Calendar.getInstance().apply { time = transDate }
        calTrans.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calTrans.set(java.util.Calendar.MINUTE, 0)
        calTrans.set(java.util.Calendar.SECOND, 0)
        calTrans.set(java.util.Calendar.MILLISECOND, 0)
        val calToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, 2026)
            set(java.util.Calendar.MONTH, java.util.Calendar.JULY)
            set(java.util.Calendar.DAY_OF_MONTH, 16)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffMs = calToday.timeInMillis - calTrans.timeInMillis
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        return diffDays > 0
    } catch (e: Exception) {
        return false
    }
}
