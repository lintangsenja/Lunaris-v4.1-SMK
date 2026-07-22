package com.example.ui.screens
import com.example.ui.components.LunarisCard

import android.app.DatePickerDialog
import android.content.Context
import android.os.Environment
import android.os.Build
import java.io.File
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.LoanItemEntity
import com.example.data.model.ItemWithStock
import com.example.data.model.ReportStats
import com.example.data.model.ReportDetailItem
import kotlinx.coroutines.flow.flowOf
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.PastelLavender
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class BorrowedLineItem(
    val namaPeminjam: String,
    val kelas: String,
    val namaBarang: String,
    val jumlah: Int,
    val tanggal: String,
    val petugas: String,
    val status: String,
    val whatsappNumber: String?,
    val idTransaksi: String,
    val tujuanPeminjaman: String? = null,
    val detailTujuan: String? = null
)

data class ReturnedLineItem(
    val namaPeminjam: String,
    val kelas: String,
    val namaBarang: String,
    val jumlah: Int,
    val tanggalKembali: String,
    val petugasKembali: String,
    val tanggalPinjam: String
)

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun LaporanScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Real-time Data from Room
    val transactions by viewModel.allTransactions.collectAsState()
    val itemsWithStock by viewModel.itemsWithStock.collectAsState()
    val totalStok by viewModel.totalStok.collectAsState()
    val damagedItems by viewModel.allDamagedItems.collectAsState()
    val maintenanceItems by viewModel.maintenanceItems.collectAsState()
    val pemakaianBahanList by viewModel.allPemakaianBahan.collectAsState()
    val bahanAfkirList by viewModel.allBahanAfkir.collectAsState()
    val namaPetugasState by viewModel.defaultOfficer.collectAsState()
    val namaPetugas = namaPetugasState.ifBlank { "Administrator" }

    var selectedTabState by remember { mutableStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }

    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var successFilename by remember { mutableStateOf("") }
    var successFileMimeType by remember { mutableStateOf("") }
    var successFileUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Date Picker States
    val calendarWib = remember {
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale("id", "ID"))
    }
    
    val vmStartDate by viewModel.startDateText.collectAsState()
    val vmEndDate by viewModel.endDateText.collectAsState()

    var startDateText by remember(vmStartDate) { mutableStateOf(vmStartDate) }
    var endDateText by remember(vmEndDate) { mutableStateOf(vmEndDate) }

    var refreshTrigger by remember { mutableStateOf(0) }

    var selectedStatus by remember { mutableStateOf<String?>(null) }

    val reportStats by remember(startDateText, endDateText) {
        viewModel.fetchReportStats(startDateText, endDateText)
    }.collectAsState(initial = null)

    LaunchedEffect(startDateText, endDateText) {
        android.util.Log.d("LaporanScreen", "Date range updated to $startDateText ... $endDateText. Force-refreshing all database observation flows simultaneously.")
        selectedStatus = null
        refreshTrigger++
    }

    // Keep cache of loan items details per transaction
    val itemsCache = remember { mutableStateMapOf<String, List<LoanItemEntity>>() }

    // Preload item details on display
    LaunchedEffect(transactions) {
        transactions.forEach { tx ->
            if (!itemsCache.containsKey(tx.idTransaksi)) {
                val list = viewModel.getItemsForTransaction(tx.idTransaksi)
                itemsCache[tx.idTransaksi] = list
            }
        }
    }

    // Dynamic filtering based on date range
    val filteredBorrowed = remember(transactions, itemsCache, startDateText, endDateText, refreshTrigger) {
        val list = mutableListOf<BorrowedLineItem>()
        transactions.forEach { tx ->
            if (tx.tanggal >= startDateText && tx.tanggal <= endDateText) {
                val lines = itemsCache[tx.idTransaksi] ?: emptyList()
                lines.forEach { item ->
                    list.add(
                        BorrowedLineItem(
                            namaPeminjam = tx.namaPeminjam,
                            kelas = tx.kelas,
                            namaBarang = item.namaBarang,
                            jumlah = item.jumlah,
                            tanggal = tx.tanggal,
                            petugas = tx.namaPetugas,
                            status = tx.status,
                            whatsappNumber = tx.whatsappNumber,
                            idTransaksi = tx.idTransaksi,
                            tujuanPeminjaman = tx.tujuanPeminjaman,
                            detailTujuan = tx.detailTujuan
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.tanggal }
    }

    // Acuan mingguan statis dihapus agar grafik dinamis menggunakan range tanggal yang dipilih

    val filteredReturned = remember(transactions, itemsCache, startDateText, endDateText, refreshTrigger) {
        val list = mutableListOf<ReturnedLineItem>()
        transactions.filter { it.status == "Kembali" }.forEach { tx ->
            val tglKembali = tx.tanggalKembali ?: ""
            if (tglKembali >= startDateText && tglKembali <= endDateText) {
                val lines = itemsCache[tx.idTransaksi] ?: emptyList()
                lines.forEach { item ->
                    list.add(
                        ReturnedLineItem(
                            namaPeminjam = tx.namaPeminjam,
                            kelas = tx.kelas,
                            namaBarang = item.namaBarang,
                            jumlah = item.jumlah,
                            tanggalKembali = tglKembali,
                            petugasKembali = tx.petugasKembali ?: "-",
                            tanggalPinjam = tx.tanggal
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.tanggalKembali }
    }

    val filteredDamaged = remember(damagedItems, startDateText, endDateText, refreshTrigger) {
        damagedItems.filter { 
            it.tanggalKerusakan >= startDateText && 
            it.tanggalKerusakan <= endDateText && 
            (it.status == "Rusak (Perlu Tindakan)" || it.status.isBlank() || it.status == "Rusak") 
        }.sortedByDescending { it.tanggalKerusakan }
    }

    val filteredMaintenance = remember(maintenanceItems, startDateText, endDateText, refreshTrigger) {
        val filtered = maintenanceItems.filter { 
            it.tanggalKerusakan >= startDateText && 
            it.tanggalKerusakan <= endDateText
        }.sortedByDescending { it.tanggalKerusakan }
        
        if (filtered.isEmpty()) {
            android.util.Log.w("LaporanScreen", "No maintenance records found in range $startDateText to $endDateText. Unfiltered maintenance count: ${maintenanceItems.size}")
        } else {
            android.util.Log.d("LaporanScreen", "Found ${filtered.size} maintenance records in range $startDateText to $endDateText")
        }
        filtered
    }

    val filteredPemakaian = remember(pemakaianBahanList, startDateText, endDateText, refreshTrigger) {
        pemakaianBahanList.filter { it.tanggalPemakaian >= startDateText && it.tanggalPemakaian <= endDateText }
            .sortedByDescending { it.tanggalPemakaian }
    }

    val filteredAfkir = remember(bahanAfkirList, startDateText, endDateText, refreshTrigger) {
        bahanAfkirList.filter { it.tanggalAfkir >= startDateText && it.tanggalAfkir <= endDateText }
            .sortedByDescending { it.tanggalAfkir }
    }

    val storagePermissionState = com.google.accompanist.permissions.rememberPermissionState(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    val doActualExport: (String) -> Unit = { format ->
        scope.launch {
            isExporting = true
            delay(700) // Beautiful progress animation delay

            val dateStr = SimpleDateFormat("dd_MM_yyyy", Locale("id", "ID")).format(Date())
            val folderName = when (selectedTabState) {
                0 -> "Ringkasan"
                1 -> "Alat"
                2 -> "Bahan"
                3 -> "Bahan Afkir"
                4 -> "Peminjaman"
                5 -> "Pengembalian"
                else -> "Alat Rusak"
            }

            val filename = "Laporan_${folderName.replace(" ", "_")}_$dateStr.${if (format == "Excel") "xlsx" else if (format == "CSV") "csv" else "pdf"}"

            val title = when (selectedTabState) {
                0 -> "RINGKASAN INVENTARIS LUNARIS"
                1 -> "DAFTAR INVENTARIS ALAT"
                2 -> "DAFTAR INVENTARIS BAHAN"
                3 -> "LAPORAN BAHAN AFKIR"
                4 -> "LAPORAN PEMINJAMAN ALAT"
                5 -> "LAPORAN PENGEMBALIAN ALAT"
                else -> "LAPORAN KONDISI ALAT RUSAK"
            }

            val headers = listOf("Nama Barang", "Jumlah", "Satuan", "Peminta/Kondisi", "Petugas", "Tanggal", "Keterangan")
            val rows = when (selectedTabState) {
                0 -> itemsWithStock.map {
                    listOf(
                        it.namaBarang,
                        it.stokTersedia.toString(),
                        it.satuan.ifBlank { "Pcs" },
                        "Kondisi: ${it.kondisi}",
                        "-",
                        "-",
                        "Kategori: ${it.kategori}, Lokasi: ${it.ruang}"
                    )
                }
                1 -> {
                    itemsWithStock.filter { it.type == "ALAT" }.map {
                        listOf(
                            it.namaBarang,
                            it.stokTersedia.toString(),
                            it.satuan.ifBlank { "Pcs" },
                            "Kondisi: ${it.kondisi}",
                            "-",
                            "-",
                            "Kategori: ${it.kategori}, Ruang: ${it.ruang}"
                        )
                    }
                }
                2 -> {
                    itemsWithStock.filter { it.type == "BAHAN" }.map {
                        listOf(
                            it.namaBarang,
                            it.stokTersedia.toString(),
                            it.satuan.ifBlank { "Pcs" },
                            "Stok Awal: ${it.stokAwal}",
                            "-",
                            "-",
                            "Ruang: ${it.ruang}"
                        )
                    }
                }
                3 -> filteredAfkir.map {
                    listOf(it.namaBarang, it.jumlahAfkir.toString(), it.satuan, it.alasan, "-", it.tanggalAfkir, "Bahan Afkir")
                }
                4 -> filteredBorrowed.map {
                    listOf(it.namaBarang, it.jumlah.toString(), "Pcs", "${it.namaPeminjam} (${it.kelas})", it.petugas, it.tanggal, "Peminjaman")
                }
                5 -> filteredReturned.map {
                    listOf(it.namaBarang, it.jumlah.toString(), "Pcs", "${it.namaPeminjam} (${it.kelas})", it.petugasKembali, it.tanggalKembali, "Pengembalian")
                }
                else -> filteredDamaged.map {
                    listOf(it.namaBarang, it.jumlah.toString(), "Pcs", "Kondisi Rusak", "-", it.tanggalKerusakan, it.keteranganKerusakan)
                }
            }

            val bytes = when (format) {
                "CSV" -> generateCsvBytes(title, headers, rows)
                "Excel" -> generateExcelBytes(title, headers, rows)
                else -> generatePdfBytes(context, title, "$startDateText s/d $endDateText", headers, rows)
            }

            val mimeType = when (format) {
                "CSV" -> "text/csv"
                "Excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                else -> "application/pdf"
            }

            val savedFile = saveReportToAutoPath(context, folderName, filename, bytes)
            if (savedFile != null) {
                successFilename = savedFile.name
                successFileMimeType = mimeType
                successFileUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    savedFile
                )
                
                val saveLocationDesc = if (savedFile.absolutePath.contains("Download")) {
                    "Penyimpanan Internal/Download/Lunaris/Unduh Laporan/$folderName/"
                } else if (savedFile.absolutePath.contains("Android/data")) {
                    "Penyimpanan Internal (Folder Aplikasi)"
                } else {
                    "Penyimpanan Internal/Lunaris/Unduh Laporan/$folderName/"
                }
                
                Toast.makeText(context, "Laporan disimpan di: $saveLocationDesc", Toast.LENGTH_LONG).show()
                showExportSuccessDialog = true
            } else {
                Toast.makeText(context, "Gagal mengekspor laporan!", Toast.LENGTH_SHORT).show()
            }
            isExporting = false
        }
    }

    val handleExport: (String) -> Unit = { format ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            doActualExport(format)
        } else {
            if (storagePermissionState.status.isGranted) {
                doActualExport(format)
            } else {
                Toast.makeText(context, "Memerlukan izin penyimpanan untuk menyimpan laporan", Toast.LENGTH_SHORT).show()
                storagePermissionState.launchPermissionRequest()
            }
        }
    }

    // Date Picker Dialog Helpers
    val startDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID")).format(cal.time)
            startDateText = dateStr
            viewModel.updateDateFilter(dateStr, endDateText)
        },
        calendarWib.get(Calendar.YEAR),
        calendarWib.get(Calendar.MONTH),
        calendarWib.get(Calendar.DAY_OF_MONTH)
    )

    val endDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID")).format(cal.time)
            endDateText = dateStr
            viewModel.updateDateFilter(startDateText, dateStr)
        },
        calendarWib.get(Calendar.YEAR),
        calendarWib.get(Calendar.MONTH),
        calendarWib.get(Calendar.DAY_OF_MONTH)
    )

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
            colors = listOf(Color(0xFFE9D5FF), Color(0xFFBFDBFE))
        )
    }
    val appBarContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText
    val selectedTabColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
    val unselectedTabColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.8f)
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 2.dp,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color(0xFF3B82F6).copy(alpha = 0.9f),
                                    androidx.compose.ui.graphics.Color(0xFF2DD4BF).copy(alpha = 0.9f)
                                )
                            )
                        )
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(40.dp).testTag("back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lunaris Reporting Analytics",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Sub-Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp)
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabState,
                        containerColor = Color.Transparent,
                        contentColor = selectedTabColor,
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                                height = 3.dp,
                                color = selectedTabColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val tabs = listOf(
                            "📊 Ringkasan" to "laporan_tab_summary",
                            "🔧 Alat" to "laporan_tab_alat",
                            "🧪 Bahan" to "laporan_tab_bahan",
                            "⚠️ Afkir" to "laporan_tab_afkir",
                            "📤 Peminjaman" to "laporan_tab_borrowed",
                            "📥 Pengembalian" to "laporan_tab_returned",
                            "❌ Alat Rusak" to "laporan_tab_damaged",
                            "🛠️ Pemeliharaan" to "laporan_tab_maintenance"
                        )
                        tabs.forEachIndexed { index, (title, tag) ->
                            Tab(
                                selected = selectedTabState == index,
                                onClick = { selectedTabState = index },
                                selectedContentColor = selectedTabColor,
                                unselectedContentColor = unselectedTabColor,
                                text = { 
                                    Text(
                                        text = title, 
                                        fontWeight = if (selectedTabState == index) FontWeight.Bold else FontWeight.Medium, 
                                        fontSize = 15.sp
                                    ) 
                                },
                                modifier = Modifier.testTag(tag)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Saring Tanggal (Glassmorphism Card Style)
                LunarisCard(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Filter Rentang Tanggal",
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { startDialog.show() }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Dari",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        startDateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { endDialog.show() }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Sampai",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        endDateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // EXPORT CONTROL ROW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { handleExport("CSV") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEDE9FE),
                            contentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) MaterialTheme.colorScheme.outline else PastelLavender
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("btn_export_csv_v")
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            tint = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "CSV",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { handleExport("Excel") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEDE9FE),
                            contentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) MaterialTheme.colorScheme.outline else PastelLavender
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("btn_export_excel")
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Excel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { handleExport("PDF") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                            contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("btn_export_pdf")
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "PDF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Dynamic Content Area with Elegant Animation Transitions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    when (selectedTabState) {
                        0 -> SummaryTabContent(
                            stats = reportStats,
                            transactionsCount = transactions.size,
                            selectedStatus = selectedStatus,
                            onSelectedStatusChange = { selectedStatus = it },
                            startDateText = startDateText,
                            endDateText = endDateText,
                            viewModel = viewModel,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        1 -> AlatListTabContent(
                            items = itemsWithStock,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        2 -> BahanListTabContent(
                            items = itemsWithStock,
                            pemakaian = filteredPemakaian,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        3 -> AfkirTabContent(
                            afkir = filteredAfkir,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        4 -> PeminjamanListTabContent(
                            borrowed = filteredBorrowed,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        5 -> PengembalianListTabContent(
                            returned = filteredReturned,
                            transactions = transactions,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        6 -> AlatRusakListTabContent(
                            damaged = filteredDamaged,
                            onNavigateToTab = { selectedTabState = it }
                        )
                        7 -> PemeliharaanTabContent(
                            maintenance = filteredMaintenance,
                            items = itemsWithStock,
                            onNavigateToTab = { selectedTabState = it }
                        )
                    }
                }
            }

            // High Contrast Loading Spinner Overlay
            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7C3AED))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Memproses file laporan...", fontWeight = FontWeight.Bold, color = DeepPurpleText)
                        }
                    }
                }
            }
        }

        // Beautiful Actionable Success Dialog
        if (showExportSuccessDialog && successFileUri != null) {
            val isDark = false
            val dialogBgColor = if (isDark) Color(0xFF1E1E2C) else Color.White
            val textColor = if (isDark) Color.White else Color(0xFF1F2937)
            val accentColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
            val secondaryBtnBg = if (isDark) Color(0xFF2D2D3F) else Color(0xFFEDE9FE)
            val secondaryBtnText = if (isDark) Color(0xFFA78BFA) else Color(0xFF4C1D95)

            AlertDialog(
                onDismissRequest = { showExportSuccessDialog = false },
                containerColor = dialogBgColor,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Laporan Berhasil Disimpan",
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "File laporan Anda telah berhasil diekspor langsung ke folder penyimpanan internal:",
                            fontWeight = FontWeight.Normal,
                            color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563),
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDark) Color(0xFF11111E) else Color(0xFFF3F4F6),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = successFilename,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Lokasi: /Lunaris/Unduh Laporan/",
                                    color = if (isDark) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Text(
                            text = "Gunakan tombol di bawah untuk membuka dokumen secara langsung atau membagikannya.",
                            color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563),
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            openFile(context, successFileUri!!, successFileMimeType)
                            showExportSuccessDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_btn_buka_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF1E1E2C) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Buka File",
                            color = if (isDark) Color(0xFF1E1E2C) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            shareFile(context, successFileUri!!, successFileMimeType)
                            showExportSuccessDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryBtnBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_btn_bagikan_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = secondaryBtnText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bagikan",
                            color = secondaryBtnText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    }
}

// ==========================================
// TAB 0: RINGKASAN CONTENT
// ==========================================
@Composable
fun SummaryTabContent(
    stats: ReportStats?,
    transactionsCount: Int,
    selectedStatus: String?,
    onSelectedStatusChange: (String?) -> Unit,
    startDateText: String,
    endDateText: String,
    viewModel: InventoryViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    if (stats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF7C3AED))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Memuat ringkasan data...",
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    val detailItems by remember(selectedStatus, startDateText, endDateText) {
        if (selectedStatus != null) {
            viewModel.fetchReportDetailItems(selectedStatus, startDateText, endDateText)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = null)

    val isDark = false

    val itemsList by viewModel.itemsWithStock.collectAsState()

    val stokAmanCount = remember(itemsList) { itemsList.count { it.stokTersedia > 2 } }
    val perluPengadaanCount = remember(itemsList) { itemsList.count { it.stokTersedia == 0 } }
    val stokKritisCount = remember(itemsList) { itemsList.count { it.stokTersedia in 1..2 } }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED BAR CHART CARD (Now presenting a modern Stock Status horizontal Bar Chart)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Distribusi Status Stok",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
                Text(
                    text = "Klik pada batang status untuk menyaring rincian data di bawah",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
                StockStatusBarChart(
                    stokAmanCount = stokAmanCount,
                    perluPengadaanCount = perluPengadaanCount,
                    stokKritisCount = stokKritisCount,
                    selectedStatus = selectedStatus,
                    onBarClick = { category ->
                        onSelectedStatusChange(if (selectedStatus == category) null else category)
                    }
                )
            }
        }

        // SCROLLABLE DRILL-DOWN TABLE (now part of the parent scrollable layout)
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedStatus != null) "Detail Data: $selectedStatus" else "Rincian Data",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    if (selectedStatus != null) {
                        TextButton(
                            onClick = { onSelectedStatusChange(null) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Reset Filter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedStatus == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED).copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Klik bagian grafik untuk melihat rincian",
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val itemsList = detailItems
                    if (itemsList == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF7C3AED), modifier = Modifier.size(24.dp))
                        }
                    } else if (itemsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada data untuk status [$selectedStatus] dalam rentang tanggal yang dipilih",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Table Headers (Fixed)
                        val label1 = "Nama Alat"
                        val label2 = when (selectedStatus) {
                            "Tersedia" -> "Ruang"
                            "Dipinjam" -> "Peminjam"
                            "Perbaikan" -> "Kerusakan"
                            "Afkir" -> "Alasan Afkir"
                            "Stok Aman", "Perlu Pengadaan", "Stok Kritis" -> "Ruang"
                            else -> "Kategori"
                        }
                        val label3 = "Jumlah"
                        val label4 = when (selectedStatus) {
                            "Tersedia" -> "Kondisi"
                            "Dipinjam" -> "Tgl Pinjam"
                            "Perbaikan" -> "Tgl Masuk"
                            "Afkir" -> "Tgl Afkir"
                            "Stok Aman", "Perlu Pengadaan", "Stok Kritis" -> "Status"
                            else -> "Tanggal"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label1, modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText)
                            Text(text = label2, modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText)
                            Text(text = label3, modifier = Modifier.weight(0.9f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, textAlign = TextAlign.End)
                            Text(text = label4, modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepPurpleText, textAlign = TextAlign.End)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsList.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.name, modifier = Modifier.weight(2f), fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Color(0xFF1E293B))
                                    Text(text = item.categoryOrRoom, modifier = Modifier.weight(1.8f), fontSize = 11.sp, color = Color(0xFF475569))
                                    Text(
                                        text = "${item.quantity} ${item.extra}", 
                                        modifier = Modifier.weight(0.9f), 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A), 
                                        textAlign = TextAlign.End
                                    )
                                    Text(text = item.dateOrStatus, modifier = Modifier.weight(1.3f), fontSize = 11.sp, color = Color(0xFF64748B), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// NEW TABS DEFINITIONS (ALAT, BAHAN, PEMINJAMAN, PENGEMBALIAN, ALAT RUSAK)
// ==========================================
@Composable
fun AlatListTabContent(
    items: List<ItemWithStock>,
    onNavigateToTab: (Int) -> Unit
) {
    val toolsOnly = remember(items) {
        items.filter { !it.kategori.equals("Logistik", ignoreCase = true) }
    }

    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }

    // Group items by room and map to Triple(Baik, PerluPerawatan, Rusak)
    val roomConditionData = remember(toolsOnly) {
        val groups = toolsOnly.groupBy { it.ruang.ifBlank { "Lainnya" } }
        groups.mapValues { (_, roomItems) ->
            var baik = 0f
            var perawatan = 0f
            var rusak = 0f
            roomItems.forEach { item ->
                val r = item.stokRusak.toFloat()
                rusak += r
                
                val rem = (item.stokAwal - item.stokRusak).coerceAtLeast(0).toFloat()
                if (item.kondisi.equals("Baik", ignoreCase = true) || item.kondisi.isBlank()) {
                    baik += rem
                } else if (item.kondisi.equals("Perlu Perawatan", ignoreCase = true) || item.kondisi.equals("Pemeliharaan", ignoreCase = true)) {
                    perawatan += rem
                } else {
                    rusak += rem
                }
            }
            Triple(baik, perawatan, rusak)
        }
    }

    val filteredTools = remember(toolsOnly, selectedRoomFilter) {
        if (selectedRoomFilter == null) {
            toolsOnly
        } else {
            toolsOnly.filter { (it.ruang.ifBlank { "Lainnya" }) == selectedRoomFilter }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED STACKED BAR CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Kondisi Alat per Ruangan",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Klik pada bar ruangan untuk menyaring daftar alat",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                StackedBarChart(
                    data = roomConditionData,
                    onBarClick = { room ->
                        selectedRoomFilter = if (selectedRoomFilter == room) null else room
                    }
                )
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Inventaris Alat", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedRoomFilter != null) {
                SuggestionChip(
                    onClick = { selectedRoomFilter = null },
                    label = { Text("Ruang: $selectedRoomFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredTools.isEmpty()) {
            EmptyStateView("Tidak ada data inventaris alat untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredTools.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF3E8FF), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.namaBarang,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Ruang: ${item.ruang.ifBlank { "Lainnya" }} | Kondisi: ${item.kondisi.ifBlank { "Baik" }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${item.stokTersedia} / ${item.stokAwal}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = DeepPurpleText
                                )
                                Text("Tersedia", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun BahanListTabContent(
    items: List<ItemWithStock>,
    pemakaian: List<com.example.data.entity.PemakaianBahanEntity>,
    onNavigateToTab: (Int) -> Unit
) {
    val bahanOnly = remember(items) {
        items.filter { it.kategori.equals("Logistik", ignoreCase = true) }
    }

    var selectedMonthFilter by remember { mutableStateOf<String?>(null) }

    // Group usage by month chronologically
    val monthlyUsageData = remember(pemakaian) {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
        val usageMap = java.util.TreeMap<String, Float>()
        
        // Ensure there's a baseline of current year months
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        for (i in (currentMonth - 5).coerceAtLeast(0)..currentMonth) {
            val key = String.format(Locale.US, "%02d %s", i + 1, monthNames[i])
            usageMap[key] = 0f
        }

        pemakaian.forEach { p ->
            try {
                val parts = p.tanggalPemakaian.split("-")
                if (parts.size >= 2) {
                    val monthIndex = parts[1].toInt() - 1
                    if (monthIndex in 0..11) {
                        val mName = monthNames[monthIndex]
                        val key = String.format(Locale.US, "%02d %s", monthIndex + 1, mName)
                        usageMap[key] = (usageMap[key] ?: 0f) + p.jumlahDiambil.toFloat()
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }
        usageMap.mapKeys { it.key.substring(3) }
    }

    val filteredBahan = remember(bahanOnly, pemakaian, selectedMonthFilter) {
        if (selectedMonthFilter == null) {
            bahanOnly
        } else {
            // Find item IDs consumed in the selected month
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
            val targetItemIds = pemakaian.filter { p ->
                try {
                    val parts = p.tanggalPemakaian.split("-")
                    if (parts.size >= 2) {
                        val mIdx = parts[1].toInt() - 1
                        mIdx in 0..11 && monthNames[mIdx] == selectedMonthFilter
                    } else false
                } catch (e: Exception) { false }
            }.map { it.idBarang }.toSet()

            bahanOnly.filter { it.idBarang in targetItemIds }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED LINE CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tren Konsumsi Bahan Bulanan",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text(
                    text = "Klik pada label bulan untuk menyaring daftar bahan",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                LineChart(
                    data = monthlyUsageData,
                    lineColor = Color(0xFF3B82F6),
                    onPointClick = { month ->
                        selectedMonthFilter = if (selectedMonthFilter == month) null else month
                    }
                )
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Inventaris Bahan", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedMonthFilter != null) {
                SuggestionChip(
                    onClick = { selectedMonthFilter = null },
                    label = { Text("Bulan: $selectedMonthFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredBahan.isEmpty()) {
            EmptyStateView("Tidak ada data inventaris bahan yang dikonsumsi pada bulan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredBahan.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.namaBarang,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Ruang: ${item.ruang} | Satuan: ${item.satuan}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${item.stokTersedia}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = DeepPurpleText
                                    )
                                    Text("Sisa Stok", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

fun isReturnLate(tanggalPinjam: String, tanggalKembali: String?): Boolean {
    if (tanggalKembali == null) return false
    return tanggalPinjam != tanggalKembali
}

fun isLaporanOverdue(tanggalTransaksi: String): Boolean {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val transDate = sdf.parse(tanggalTransaksi) ?: return false
        val calTrans = Calendar.getInstance().apply { time = transDate }
        calTrans.set(Calendar.HOUR_OF_DAY, 0)
        calTrans.set(Calendar.MINUTE, 0)
        calTrans.set(Calendar.SECOND, 0)
        calTrans.set(Calendar.MILLISECOND, 0)
        val calToday = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 16)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = calToday.timeInMillis - calTrans.timeInMillis
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        return diffDays > 0
    } catch (e: Exception) {
        return false
    }
}

@Composable
fun PeminjamanListTabContent(
    borrowed: List<BorrowedLineItem>,
    onNavigateToTab: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedItemFilter by remember { mutableStateOf<String?>(null) }

    // Calculate Top 5 most borrowed tools in the selected date range
    val topBorrowedData = remember(borrowed) {
        borrowed.groupBy { it.namaBarang }
            .mapValues { entry -> entry.value.sumOf { it.jumlah }.toFloat() }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()
    }

    val filteredBorrowed = remember(borrowed, selectedItemFilter) {
        if (selectedItemFilter == null) {
            borrowed
        } else {
            borrowed.filter { it.namaBarang == selectedItemFilter }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED HORIZONTAL BAR CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Statistik Peminjaman Alat",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada bar alat untuk menyaring daftar transaksi", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (topBorrowedData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tidak ada aktivitas peminjaman pada rentang tanggal terpilih",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    InteractiveHorizontalBarChart(
                        data = topBorrowedData,
                        barColor = Color(0xFF3B82F6),
                        onBarClick = { itemName ->
                            selectedItemFilter = if (selectedItemFilter == itemName) null else itemName
                        }
                    )
                }
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat Peminjaman Alat", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedItemFilter != null) {
                SuggestionChip(
                    onClick = { selectedItemFilter = null },
                    label = { Text("Alat: $selectedItemFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredBorrowed.isEmpty()) {
            EmptyStateView("Tidak ada riwayat peminjaman untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredBorrowed.forEach { item ->
                    val isLate = item.status == "Dipinjam" && isLaporanOverdue(item.tanggal)
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isLate) Color(0xFFFCA5A5) else Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(if (isLate) Color(0xFFFEE2E2) else Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLate) Icons.Default.Warning else Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = if (isLate) Color(0xFFEF4444) else Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${item.namaBarang} (${item.jumlah} Pcs)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepPurpleText,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        
                                        if (isLate) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Terlambat",
                                                    color = Color(0xFF991B1B),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Peminjam: ${item.namaPeminjam} (${item.kelas})", style = MaterialTheme.typography.bodySmall)
                                    if (!item.tujuanPeminjaman.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Tujuan: ${item.tujuanPeminjaman}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (!item.detailTujuan.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val detailLabel = if (item.tujuanPeminjaman == "Kegiatan Belajar Mengajar (KBM)") {
                                            "Guru/Mapel"
                                        } else {
                                            "Detail"
                                        }
                                        Text("$detailLabel: ${item.detailTujuan}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Petugas: ${item.petugas}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(item.tanggal, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                            
                            if (isLate) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            val phoneNum = item.whatsappNumber ?: "6285600005719"
                                            val message = "Halo ${item.namaPeminjam}, kami dari sarpras ingin mengingatkan bahwa ${item.namaBarang} (${item.jumlah} Pcs) yang Anda pinjam telah melewati batas pengembalian. Harap segera mengembalikannya ke gudang. Terima kasih!"
                                            val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                data = android.net.Uri.parse("https://wa.me/$phoneNum?text=$encodedMsg")
                                            }
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp).testTag("wa_remind_button_laporan")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "WA",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kirim Pengingat WA", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PengembalianListTabContent(
    returned: List<ReturnedLineItem>,
    transactions: List<com.example.data.entity.LoanTransactionEntity>,
    onNavigateToTab: (Int) -> Unit
) {
    var selectedTimelinessFilter by remember { mutableStateOf<String?>(null) }

    // Calculate Tepat Waktu vs Terlambat counts based on transactions
    val (tepatWaktuCount, terlambatCount) = remember(transactions) {
        var tepat = 0f
        var lambat = 0f
        transactions.forEach { tx ->
            if (tx.status == "Kembali") {
                val isLate = isReturnLate(tx.tanggal, tx.tanggalKembali)
                if (isLate) lambat += 1f else tepat += 1f
            } else if (tx.status == "Dipinjam") {
                if (isLaporanOverdue(tx.tanggal)) {
                    lambat += 1f
                }
            }
        }
        Pair(tepat, lambat)
    }

    val filteredReturned = remember(returned, selectedTimelinessFilter) {
        if (selectedTimelinessFilter == null) {
            returned
        } else {
            val filterForLate = selectedTimelinessFilter == "Terlambat"
            returned.filter { isReturnLate(it.tanggalPinjam, it.tanggalKembali) == filterForLate }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED DISCIPLINE BAR CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Statistik Pengembalian Alat",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada bar untuk menyaring daftar pengembalian", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                DisciplineBarChart(
                    tepatWaktu = tepatWaktuCount,
                    terlambat = terlambatCount,
                    onBarClick = { filter ->
                        selectedTimelinessFilter = if (selectedTimelinessFilter == filter) null else filter
                    }
                )
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat Pengembalian Alat", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedTimelinessFilter != null) {
                SuggestionChip(
                    onClick = { selectedTimelinessFilter = null },
                    label = { Text("Filter: $selectedTimelinessFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredReturned.isEmpty()) {
            EmptyStateView("Tidak ada riwayat pengembalian untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredReturned.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFECFDF5), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.namaBarang} (${item.jumlah} Pcs)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Peminjam: ${item.namaPeminjam} (${item.kelas})", style = MaterialTheme.typography.bodySmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Petugas: ${item.petugasKembali}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text(item.tanggalKembali, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun AlatRusakListTabContent(
    damaged: List<com.example.data.entity.DamagedItemEntity>,
    onNavigateToTab: (Int) -> Unit
) {
    var selectedMonthFilter by remember { mutableStateOf<String?>(null) }

    // Group damage count by month chronologically
    val monthlyDamageData = remember(damaged) {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
        val damageMap = java.util.TreeMap<String, Float>()
        
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        for (i in (currentMonth - 5).coerceAtLeast(0)..currentMonth) {
            val key = String.format(Locale.US, "%02d %s", i + 1, monthNames[i])
            damageMap[key] = 0f
        }

        damaged.forEach { d ->
            try {
                val parts = d.tanggalKerusakan.split("-")
                if (parts.size >= 2) {
                    val monthIndex = parts[1].toInt() - 1
                    if (monthIndex in 0..11) {
                        val mName = monthNames[monthIndex]
                        val key = String.format(Locale.US, "%02d %s", monthIndex + 1, mName)
                        damageMap[key] = (damageMap[key] ?: 0f) + d.jumlah.toFloat()
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }
        damageMap.mapKeys { it.key.substring(3) }
    }

    val filteredDamaged = remember(damaged, selectedMonthFilter) {
        if (selectedMonthFilter == null) {
            damaged
        } else {
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
            damaged.filter { d ->
                try {
                    val parts = d.tanggalKerusakan.split("-")
                    if (parts.size >= 2) {
                        val mIdx = parts[1].toInt() - 1
                        mIdx in 0..11 && monthNames[mIdx] == selectedMonthFilter
                    } else false
                } catch (e: Exception) { false }
            }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED DAMAGE AREA CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daftar Alat Rusak",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada label bulan untuk menyaring log kerusakan", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                LineChart(
                    data = monthlyDamageData,
                    lineColor = Color(0xFFEF4444), // Crimson/Red for damage
                    onPointClick = { month ->
                        selectedMonthFilter = if (selectedMonthFilter == month) null else month
                    }
                )
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Kondisi Alat Rusak", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedMonthFilter != null) {
                SuggestionChip(
                    onClick = { selectedMonthFilter = null },
                    label = { Text("Bulan: $selectedMonthFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredDamaged.isEmpty()) {
            EmptyStateView("Tidak ada laporan alat rusak untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredDamaged.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.namaBarang} (${item.jumlah} Pcs Rusak)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Keterangan: ${item.keteranganKerusakan}", style = MaterialTheme.typography.bodySmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Tanggal Lapor", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(item.tanggalKerusakan, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun PemeliharaanTabContent(
    maintenance: List<com.example.data.entity.DamagedItemEntity>,
    items: List<ItemWithStock>,
    onNavigateToTab: (Int) -> Unit
) {
    var selectedRoomFilter by remember { mutableStateOf<String?>(null) }

    // Map each item ID to its room
    val itemRoomMap = remember(items) {
        items.associate { it.idBarang to it.ruang }
    }

    // Group maintenance items by room
    val roomMaintenanceData = remember(maintenance, itemRoomMap) {
        val grouped = maintenance.groupBy { itemRoomMap[it.idBarang]?.ifBlank { "Lainnya" } ?: "Lainnya" }
            .mapValues { entry -> entry.value.sumOf { it.jumlah }.toFloat() }
        
        if (grouped.isEmpty()) {
            android.util.Log.e("PemeliharaanTabContent", "Maintenance data map for Bar Chart is EMPTY! Unfiltered maintenance list size is: ${maintenance.size}")
        } else {
            android.util.Log.d("PemeliharaanTabContent", "Successfully mapped maintenance data for Bar Chart: $grouped")
        }
        grouped
    }

    val filteredMaintenance = remember(maintenance, selectedRoomFilter, itemRoomMap) {
        if (selectedRoomFilter == null) {
            maintenance
        } else {
            maintenance.filter { (itemRoomMap[it.idBarang]?.ifBlank { "Lainnya" } ?: "Lainnya") == selectedRoomFilter }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED LOCATION BAR CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Jadwal Pemeliharaan Alat",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada bar ruangan untuk menyaring daftar pemeliharaan", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (roomMaintenanceData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tidak ada alat dalam status pemeliharaan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    InteractiveHorizontalBarChart(
                        data = roomMaintenanceData,
                        barColor = Color(0xFFF59E0B), // Amber for maintenance
                        onBarClick = { room ->
                            selectedRoomFilter = if (selectedRoomFilter == room) null else room
                        }
                    )
                }
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Pemeliharaan Alat (Servis Luar)", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedRoomFilter != null) {
                SuggestionChip(
                    onClick = { selectedRoomFilter = null },
                    label = { Text("Ruang: $selectedRoomFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredMaintenance.isEmpty()) {
            EmptyStateView("Tidak ada alat dalam status pemeliharaan untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredMaintenance.forEach { item ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.namaBarang} (${item.jumlah} Pcs di-Servis)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Keterangan Awal: ${item.keteranganKerusakan}", style = MaterialTheme.typography.bodySmall)
                                    if (item.statusKeterangan.isNotBlank()) {
                                        Text("Catatan Pemeliharaan: ${item.statusKeterangan}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Petugas: ${item.namaPetugas}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(item.tanggalKerusakan, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// TAB 3: AFKIR CONTENT
// ==========================================
@Composable
fun AfkirTabContent(
    afkir: List<com.example.data.entity.BahanAfkirEntity>,
    onNavigateToTab: (Int) -> Unit
) {
    var selectedReasonFilter by remember { mutableStateOf<String?>(null) }

    // Reason Pie Chart Mapping
    val reasonCounts = remember(afkir) {
        afkir.groupBy { it.alasan.ifBlank { "Lainnya" } }
            .mapValues { entry -> entry.value.sumOf { it.jumlahAfkir }.toFloat() }
    }

    val chartColors = listOf(
        Color(0xFFEF4444), // Red for Rusak/Damage
        Color(0xFFF59E0B), // Amber for Expired
        Color(0xFF6B7280)  // Gray for Lost
    )

    val filteredAfkir = remember(afkir, selectedReasonFilter) {
        if (selectedReasonFilter == null || selectedReasonFilter == "Semua") {
            afkir
        } else {
            afkir.filter { it.alasan.ifBlank { "Lainnya" } == selectedReasonFilter }
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIXED PIE CHART CARD
        LunarisCard(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Data Inventaris Afkir",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepPurpleText,
                    fontSize = 16.sp
                )
                Text("Klik pada label alasan untuk menyaring log", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                InteractivePieChart(
                    data = reasonCounts,
                    colors = chartColors,
                    onSliceClick = { reason ->
                        selectedReasonFilter = if (reason == "Semua" || selectedReasonFilter == reason) null else reason
                    }
                )
            }
        }

        // Header Row for the list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Log Riwayat Bahan Afkir", fontWeight = FontWeight.ExtraBold, color = DeepPurpleText, fontSize = 16.sp)
            if (selectedReasonFilter != null) {
                SuggestionChip(
                    onClick = { selectedReasonFilter = null },
                    label = { Text("Alasan: $selectedReasonFilter ✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (filteredAfkir.isEmpty()) {
            EmptyStateView("Tidak ada data log bahan afkir untuk penyaringan ini.")
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredAfkir.forEach { log ->
                    LunarisCard(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.namaBarang, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepPurpleText)
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("${log.jumlahAfkir} ${log.satuan}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Alasan Afkir: ${log.alasan}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ID Barang: ${log.idBarang}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(log.tanggalAfkir, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ==========================================
// CUSTOM INTERACTIVE GRAPHIC DRAWINGS (CANVAS)
// ==========================================
@Composable
fun GlassmorphicPieChart(
    data: Map<String, Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    if (total == 0f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada data untuk grafik", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Canvas(modifier = Modifier.size(110.dp)) {
            var startAngle = 0f
            data.entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / total) * 360f
                val color = colors[index % colors.size]
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = size
                )
                startAngle += sweepAngle
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            data.entries.forEachIndexed { index, entry ->
                val color = colors[index % colors.size]
                val percentage = if (total > 0) (entry.value / total) * 100 else 0f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.key}: ${entry.value.toInt()} (${String.format(Locale.US, "%.1f", percentage)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText
                    )
                }
            }
        }
    }
}

@Composable
fun GlassmorphicBarChart(
    data: Map<String, Float>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = data.values.maxOrNull() ?: 0f
    if (maxVal == 0f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada data untuk grafik", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.entries.forEach { entry ->
            val fraction = entry.value / maxVal
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(entry.key, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = DeepPurpleText)
                    Text("${entry.value.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(barColor, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// FILE OPERATIONS & SCOPED STORAGE HELPER
// ==========================================
fun saveReportToAutoPath(
    context: Context,
    folderName: String,
    filename: String,
    bytes: ByteArray
): File? {
    try {
        // 1. Target directory: /storage/emulated/0/Lunaris/Unduh Laporan/[folderName]
        // In Indonesian storage naming, external storage root is the "Penyimpanan Internal"
        val storageRoot = Environment.getExternalStorageDirectory()
        val targetDir = File(storageRoot, "Lunaris/Unduh Laporan/$folderName")
        
        var finalDir = targetDir
        var canWrite = false
        
        try {
            if (!finalDir.exists()) {
                finalDir.mkdirs()
            }
            val testFile = File(finalDir, ".test")
            if (testFile.createNewFile()) {
                testFile.delete()
                canWrite = true
            }
        } catch (e: Exception) {
            canWrite = false
        }
        
        // 2. Fallback to /storage/emulated/0/Download/Lunaris/Unduh Laporan/[folderName]
        if (!canWrite) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            finalDir = File(downloadsDir, "Lunaris/Unduh Laporan/$folderName")
            try {
                if (!finalDir.exists()) {
                    finalDir.mkdirs()
                }
                val testFile = File(finalDir, ".test")
                if (testFile.createNewFile()) {
                    testFile.delete()
                    canWrite = true
                }
            } catch (e: Exception) {
                canWrite = false
            }
        }
        
        // 3. Fallback to external files dir: /storage/emulated/0/Android/data/[package]/files/Lunaris/...
        if (!canWrite) {
            finalDir = File(context.getExternalFilesDir(null), "Lunaris/Unduh Laporan/$folderName")
            if (!finalDir.exists()) {
                finalDir.mkdirs()
            }
        }
        
        val destFile = File(finalDir, filename)
        destFile.outputStream().use { os ->
            os.write(bytes)
        }
        return destFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun writeBytesToUri(context: Context, uri: android.net.Uri, bytes: ByteArray): Boolean {
    return try {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(bytes)
            true
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun shareFile(context: Context, uri: android.net.Uri, mimeType: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Laporan"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun openFile(context: Context, uri: android.net.Uri, mimeType: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini. Silakan pasang aplikasi penampil PDF/Excel/CSV terlebih dahulu.", Toast.LENGTH_LONG).show()
    }
}

// ==========================================
// NATIVE FORMAT GENERATORS (CSV, EXCEL, PDF)
// ==========================================
fun generateCsvBytes(title: String, headers: List<String>, rows: List<List<String>>): ByteArray {
    val bos = java.io.ByteArrayOutputStream()
    bos.write(0xEF)
    bos.write(0xBB)
    bos.write(0xBF) // UTF-8 BOM
    val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(bos, Charsets.UTF_8))
    
    writer.write("\"$title\"\n\n")
    writer.write(headers.joinToString(separator = ",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
    rows.forEach { row ->
        writer.write(row.joinToString(separator = ",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
    }
    writer.flush()
    return bos.toByteArray()
}

fun generateExcelBytes(title: String, headers: List<String>, rows: List<List<String>>): ByteArray {
    val bos = java.io.ByteArrayOutputStream()
    bos.write(0xEF)
    bos.write(0xBB)
    bos.write(0xBF) // UTF-8 BOM
    val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(bos, Charsets.UTF_8))
    
    writer.write("sep=;\n") // Force Excel semicolon recognition
    writer.write("\"$title\";\n\n")
    writer.write(headers.joinToString(separator = ";") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
    rows.forEach { row ->
        writer.write(row.joinToString(separator = ";") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
    }
    writer.flush()
    return bos.toByteArray()
}

fun generatePdfBytes(
    context: Context,
    title: String,
    period: String,
    headers: List<String>,
    rows: List<List<String>>
): ByteArray {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    
    val pageWidth = 595 // A4 standard width
    val pageHeight = 842 // A4 standard height
    
    var pageNumber = 1
    var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var currentPage = pdfDocument.startPage(pageInfo)
    var canvas = currentPage.canvas
    
    val paintText = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 8.5f
        isAntiAlias = true
    }
    
    val paintHeader = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#3B0764") // DeepPurpleText
        textSize = 14f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    val paintSub = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 9.5f
        isAntiAlias = true
    }
    
    val paintTableHead = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 8.5f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    val paintTableHeadBg = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#6D28D9") // Deep Purple
    }
    
    val paintBorder = android.graphics.Paint().apply {
        color = android.graphics.Color.LTGRAY
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 0.5f
    }
    
    val paintBgAlternate = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#FAF5FF")
    }

    var y = 50f
    
    // Header Title
    canvas.drawText(title, 40f, y, paintHeader)
    y += 18f
    canvas.drawText("Periode: $period", 40f, y, paintSub)
    y += 30f
    
    // Columns distribution widths
    val colWidths = floatArrayOf(110f, 40f, 40f, 110f, 70f, 65f, 80f)
    val colPositions = FloatArray(7)
    var currentX = 40f
    for (i in 0..6) {
        colPositions[i] = currentX
        currentX += colWidths[i]
    }
    
    // Draw Header Table
    canvas.drawRect(40f, y, 555f, y + 20f, paintTableHeadBg)
    for (i in 0..6) {
        canvas.drawText(headers[i], colPositions[i] + 4f, y + 13f, paintTableHead)
    }
    y += 20f
    
    // Draw Rows
    rows.forEachIndexed { rowIndex, row ->
        if (y + 24f > pageHeight - 50f) {
            pdfDocument.finishPage(currentPage)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            y = 50f
            
            // Repeat Header
            canvas.drawRect(40f, y, 555f, y + 20f, paintTableHeadBg)
            for (i in 0..6) {
                canvas.drawText(headers[i], colPositions[i] + 4f, y + 13f, paintTableHead)
            }
            y += 20f
        }
        
        // Alternate Background Draw
        if (rowIndex % 2 == 1) {
            canvas.drawRect(40f, y, 555f, y + 20f, paintBgAlternate)
        }
        
        // Border Rectangle
        canvas.drawRect(40f, y, 555f, y + 20f, paintBorder)
        for (i in 1..6) {
            canvas.drawLine(colPositions[i], y, colPositions[i], y + 20f, paintBorder)
        }
        
        // Write cells values
        for (i in 0..6) {
            val rawValue = row.getOrNull(i) ?: ""
            val paint = paintText
            val availableWidth = colWidths[i] - 8f
            var textToDraw = rawValue
            if (paint.measureText(textToDraw) > availableWidth) {
                while (textToDraw.isNotEmpty() && paint.measureText("$textToDraw...") > availableWidth) {
                    textToDraw = textToDraw.substring(0, textToDraw.length - 1)
                }
                textToDraw = "$textToDraw..."
            }
            canvas.drawText(textToDraw, colPositions[i] + 4f, y + 13f, paintText)
        }
        y += 20f
    }
    
    pdfDocument.finishPage(currentPage)
    
    val bos = java.io.ByteArrayOutputStream()
    pdfDocument.writeTo(bos)
    pdfDocument.close()
    
    return bos.toByteArray()
}

fun saveFileToDownloads(
    context: Context, 
    filename: String, 
    mimeType: String, 
    bytes: ByteArray,
    onSuccess: (android.net.Uri) -> Unit = {}
) {
    try {
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { os ->
                    os.write(bytes)
                }
                Toast.makeText(context, "Berhasil diekspor ke folder Downloads: $filename", Toast.LENGTH_LONG).show()
                onSuccess(uri)
            } else {
                Toast.makeText(context, "Gagal membuat file!", Toast.LENGTH_SHORT).show()
            }
        } else {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadDir, filename)
            java.io.FileOutputStream(file).use { os ->
                os.write(bytes)
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.lintang.lunaris.fileprovider",
                file
            )
            Toast.makeText(context, "Berhasil diekspor ke folder Downloads: $filename", Toast.LENGTH_LONG).show()
            onSuccess(uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
