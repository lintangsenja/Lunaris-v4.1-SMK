package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField
import com.example.ui.components.FilterGroup
import com.example.ui.components.LunarisFilterDialog

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryEntity
import com.example.data.entity.ItemEntity
import com.example.data.entity.UnitEntity
import com.example.data.model.ItemWithStock
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GlassWhiteMore
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.SoftGoldText
import com.example.ui.theme.CarbonBlackText
import com.example.ui.theme.PastelLavender
import java.io.BufferedReader
import java.io.InputStreamReader
import android.Manifest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlatScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
    val appBarContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    val allItems by viewModel.itemsWithStock.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var tempSelectedCategory by remember { mutableStateOf("Semua Kategori") }
    var tempSelectedRoom by remember { mutableStateOf("Semua Ruang") }
    var tempSelectedCondition by remember { mutableStateOf("Semua Kondisi") }
    
    var appliedCategory by remember { mutableStateOf("Semua Kategori") }
    var appliedRoom by remember { mutableStateOf("Semua Ruang") }
    var appliedCondition by remember { mutableStateOf("Semua Kondisi") }
    
    // Filter items to only show durable goods / Alat (type == "ALAT")
    val filteredItems = remember(allItems, searchQuery, appliedCategory, appliedRoom, appliedCondition, userRole) {
        allItems.filter { it.type == "ALAT" }
            .filter { it.namaBarang.contains(searchQuery, ignoreCase = true) || it.idBarang.contains(searchQuery, ignoreCase = true) }
            .filter { appliedCategory == "Semua Kategori" || it.kategori == appliedCategory }
            .filter { appliedRoom == "Semua Ruang" || it.ruang == appliedRoom }
            .filter { appliedCondition == "Semua Kondisi" || it.kondisi == appliedCondition }
            .filter { userRole == "admin" || it.isBorrowable }
    }
    
    val lazyListState = rememberLazyListState()
    
    val allCategories by viewModel.allCategories.collectAsState()
    // Filter categories for Alat
    val categories = remember(allCategories) {
        allCategories.filter { it.name != "Logistik" }
    }

    val categoriesOptions = remember(categories) {
        listOf("Semua Kategori") + categories.map { it.name }
    }
    
    val roomOptions = remember(allItems) {
        val uniqueRooms = allItems.filter { it.kategori != "Logistik" && it.ruang.isNotEmpty() }.map { it.ruang }.distinct().sorted()
        listOf("Semua Ruang") + uniqueRooms
    }
    
    val conditionOptions = listOf("Semua Kondisi", "Normal", "Rusak", "Perbaikan")
    
    val units by viewModel.allUnits.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val csvLines = mutableListOf<List<String>>()
                    var line = reader.readLine()
                    
                    var delimiter = ','
                    if (line != null) {
                        if (line.contains(";") && !line.contains(",")) {
                            delimiter = ';'
                        } else if (line.count { it == ';' } > line.count { it == ',' }) {
                            delimiter = ';'
                        }
                    }
                    
                    while (line != null) {
                        if (line.isNotBlank()) {
                            csvLines.add(parseCsvLine(line, delimiter))
                        }
                        line = reader.readLine()
                    }
                    
                    if (csvLines.isNotEmpty()) {
                        viewModel.importCsvData(
                            csvLines = csvLines,
                            defaultType = "ALAT",
                            onSuccess = { added, updated ->
                                Toast.makeText(context, "Berhasil impor CSV! Baru: $added, Update: $updated", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "Error Impor: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "File CSV kosong!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRepairDialog by remember { mutableStateOf(false) }

    val merekAlatList by viewModel.merekAlat.collectAsState()
    val ruangList by viewModel.ruang.collectAsState()
    val sumberDanaList by viewModel.sumberDana.collectAsState()
    val kondisiList by viewModel.kondisi.collectAsState()

    var selectedItemForEdit by remember { mutableStateOf<ItemWithStock?>(null) }
    var selectedItemForDelete by remember { mutableStateOf<ItemWithStock?>(null) }
    var selectedItemForRepair by remember { mutableStateOf<ItemWithStock?>(null) }

    // Add Form State
    var showQuickAddType by remember { mutableStateOf<String?>(null) }
    var quickAddInputValue by remember { mutableStateOf("") }

    var nameInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }
    var unitInput by remember { mutableStateOf("") }
    var initialStockInput by remember { mutableStateOf("1") }
    var merekAlatInput by remember { mutableStateOf("") }
    var ruangInput by remember { mutableStateOf("") }
    var sumberDanaInput by remember { mutableStateOf("Belum Diketahui / Kosongkan") }
    var kondisiInput by remember { mutableStateOf("Normal") }
    var keteranganInput by remember { mutableStateOf("") }
    var isBorrowableInput by remember { mutableStateOf(false) }

    // Edit Form State
    var editNameInput by remember { mutableStateOf("") }
    var editCategoryInput by remember { mutableStateOf("") }
    var editUnitInput by remember { mutableStateOf("") }
    var editStockInput by remember { mutableStateOf("1") }
    var editMerekAlatInput by remember { mutableStateOf("") }
    var editRuangInput by remember { mutableStateOf("") }
    var editSumberDanaInput by remember { mutableStateOf("Belum Diketahui / Kosongkan") }
    var editKondisiInput by remember { mutableStateOf("Normal") }
    var editKeteranganInput by remember { mutableStateOf("") }
    var editIsBorrowableInput by remember { mutableStateOf(selectedItemForEdit?.isBorrowable ?: false) }

    LaunchedEffect(selectedItemForEdit, showEditDialog) {
        if (showEditDialog) {
            selectedItemForEdit?.let { item ->
                editNameInput = item.namaBarang
                editCategoryInput = item.kategori
                editUnitInput = item.satuan
                editStockInput = item.stokAwal.toString()
                editMerekAlatInput = item.merekAlat
                editRuangInput = item.ruang
                editSumberDanaInput = item.sumberDana ?: "Belum Diketahui / Kosongkan"
                editKondisiInput = item.kondisi
                editKeteranganInput = item.keterangan
                editIsBorrowableInput = item.isBorrowable
            }
        }
    }

    var repairQtyInput by remember { mutableStateOf("") }

    // Auto-generate ID estimate preview (use global items for sequence)
    val estimatedNextId = remember(allItems) {
        var maxIdNum = 0
        allItems.forEach { item ->
            if (item.idBarang.startsWith("BRG-")) {
                val numPart = item.idBarang.substringAfter("BRG-").toIntOrNull()
                if (numPart != null && numPart > maxIdNum) {
                    maxIdNum = numPart
                }
            }
        }
        "BRG-${String.format(Locale.US, "%03d", maxIdNum + 1)}"
    }

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
                                text = "Kelola Alat & Inventaris",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
                androidx.compose.material3.HorizontalDivider(
                    thickness = 1.2.dp,
                    color = androidx.compose.ui.graphics.Color.Transparent
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val isAddStockInvalid = initialStockInput.trim().toIntOrNull() == null || (initialStockInput.trim().toIntOrNull() ?: 0) < 1
            val isEditStockInvalid = editStockInput.trim().toIntOrNull() == null || (editStockInput.trim().toIntOrNull() ?: 0) < 1

            Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 0.dp)) {
                if (userRole == "admin") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. IMPOR (Kiri)
                        LunarisCard(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC),
                                contentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { csvLauncher.launch("*/*") }
                                .testTag("btn_impor_csv")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = "Impor CSV",
                                    tint = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Impor CSV",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 2. TAMBAH (Tengah)
                        LunarisCard(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF5F3FF),
                                contentColor = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else DeepPurpleText
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    categoryInput = categories.firstOrNull()?.name ?: ""
                                    unitInput = units.firstOrNull()?.name ?: ""
                                    merekAlatInput = merekAlatList.firstOrNull() ?: ""
                                    ruangInput = ruangList.firstOrNull() ?: ""
                                    sumberDanaInput = "Belum Diketahui / Kosongkan"
                                    kondisiInput = kondisiList.firstOrNull() ?: "Normal"
                                    nameInput = ""
                                    initialStockInput = "1"
                                    keteranganInput = ""
                                    showAddDialog = true
                                }
                                .testTag("btn_tambah_barang_baru")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah",
                                    tint = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tambah Alat",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 3. UNDUH (Kanan)
                        LunarisCard(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC),
                                contentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val templateFilename = "Template_Impor_Alat_Lunaris.csv"
                                    val templateMimeType = "text/csv"
                                    val templateContent = "nama_alat,kategori,merek,ruang,satuan,stok_awal,sumber_dana,kondisi,keterangan\n" +
                                            "Laptop ASUS Core i5,Elektronik,ASUS,Lab Komputer 1,Unit,15,BOS Reguler,Sangat Baik,Laptop untuk ujian\n" +
                                            "Proyektor Epson EB-X400,Elektronik,Epson,Aula Utama,Unit,15,BOS Kinerja,Baik (Siap Pakai),Proyektor presentasi"
                                    saveFileToDownloads(
                                        context = context,
                                        filename = templateFilename,
                                        mimeType = templateMimeType,
                                        bytes = templateContent.toByteArray(Charsets.UTF_8)
                                    ) {
                                        Toast.makeText(context, "Template berhasil diunduh ke folder Download!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .testTag("btn_unduh_template_csv")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Unduh Template",
                                    tint = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Unduh Contoh",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LunarisTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                        },
                        placeholder = { Text("Cari alat...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                        trailingIcon = {
                            IconButton(onClick = { showQrScanner = true }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
                                )
                            }
                        },
                        singleLine = true,
                        isStaticOutline = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("alat_search_bar")
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(
                                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { showFilterDialog = true }
                            .testTag("alat_filter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED)
                        )
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

                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = "Kosong",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Tidak ada alat yang cocok dengan pencarian." else "Belum ada alat. Silakan tambah alat baru.", 
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredItems, key = { it.idBarang }) { item ->
                            LunarisCard(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.namaBarang,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "ID: ${item.idBarang}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (item.merekAlat.isNotEmpty() || item.ruang.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = buildString {
                                                        if (item.merekAlat.isNotEmpty()) append("Merek: ${item.merekAlat}")
                                                        if (item.ruang.isNotEmpty()) {
                                                            if (isNotEmpty()) append(" | ")
                                                            append("Ruang: ${item.ruang}")
                                                        }
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }
                                            if (item.kondisi.isNotEmpty() || !item.sumberDana.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = buildString {
                                                        if (item.kondisi.isNotEmpty()) append("Kondisi: ${item.kondisi}")
                                                        if (!item.sumberDana.isNullOrEmpty()) {
                                                            if (isNotEmpty()) append(" | ")
                                                            append("Dana: ${item.sumberDana}")
                                                        }
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }
                                            if (item.keterangan.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Keterangan: ${item.keterangan}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }
                                        }

                                        if (userRole == "admin") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // Repair Button
                                                if (item.stokRusak > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            selectedItemForRepair = item
                                                            repairQtyInput = item.stokRusak.toString()
                                                            showRepairDialog = true
                                                        },
                                                        modifier = Modifier.testTag("repair_barang_${item.idBarang}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Build,
                                                            contentDescription = "Perbaiki",
                                                            tint = Color(0xFFD97706)
                                                        )
                                                    }
                                                }

                                                // Edit Button
                                                IconButton(
                                                    onClick = {
                                                        selectedItemForEdit = item
                                                        editNameInput = item.namaBarang
                                                        editCategoryInput = item.kategori
                                                        editUnitInput = item.satuan
                                                        editStockInput = item.stokAwal.toString()
                                                        editMerekAlatInput = item.merekAlat
                                                        editRuangInput = item.ruang
                                                        editSumberDanaInput = item.sumberDana ?: "Belum Diketahui / Kosongkan"
                                                        editKondisiInput = item.kondisi
                                                        editKeteranganInput = item.keterangan
                                                        editIsBorrowableInput = item.isBorrowable
                                                        showEditDialog = true
                                                    },
                                                    modifier = Modifier.testTag("edit_barang_${item.idBarang}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }

                                                // Delete Button
                                                IconButton(
                                                    onClick = {
                                                        selectedItemForDelete = item
                                                        showDeleteConfirmDialog = true
                                                    },
                                                    modifier = Modifier.testTag("delete_barang_${item.idBarang}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Info Kategori & Satuan
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (item.kategori.isNotEmpty()) {
                                                Text(
                                                    text = "Kategori: ${item.kategori}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF7C3AED)
                                                )
                                            }
                                            if (item.satuan.isNotEmpty()) {
                                                Text(
                                                    text = "Satuan: ${item.satuan}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }
                                        }

                                        // Stock Info
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(text = "Stok Fisik", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${item.stokAwal} ${item.satuan.ifEmpty { "Pcs" }}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E40AF)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(text = "Tersedia", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val (stokText, stokColor) = when {
                                                    item.stokTersedia > 2 -> {
                                                        "${item.stokTersedia} ${item.satuan.ifEmpty { "Pcs" }} (Aman)" to Color(0xFF047857) // Hijau
                                                    }
                                                    item.stokTersedia in 1..2 -> {
                                                        "${item.stokTersedia} ${item.satuan.ifEmpty { "Pcs" }} (Menipis)" to Color(0xFFD97706) // Oranye
                                                    }
                                                    else -> {
                                                        "Stok Habis" to Color(0xFFB91C1C) // Merah
                                                    }
                                                }
                                                Text(
                                                    text = stokText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = stokColor
                                                )
                                            }
                                            if (item.stokRusak > 0) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(text = "Rusak", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${item.stokRusak} ${item.satuan.ifEmpty { "Pcs" }}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB91C1C)
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

            // Dialog Tambah Barang Baru
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF8FAFC),
                    title = { Text("Tambah Alat Baru", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Estimasi ID Otomatis: $estimatedNextId",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            // 1. Nama Alat
                            LunarisTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Nama Alat") },
                                placeholder = { Text("Contoh: Laptop Asus") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("input_barang_nama")
                            )

                            // 2. Kategori
                            DynamicDropdownField(
                                label = "Kategori",
                                selectedValue = categoryInput,
                                options = categories.map { it.name },
                                onValueChange = { categoryInput = it },
                                testTag = "input_barang_kategori",
                                onQuickAddClick = { showQuickAddType = "Kategori" }
                            )

                            // 3. Merek Alat
                            DynamicDropdownField(
                                label = "Merek Alat",
                                selectedValue = merekAlatInput,
                                options = merekAlatList,
                                onValueChange = { merekAlatInput = it },
                                testTag = "input_barang_merek",
                                onQuickAddClick = { showQuickAddType = "Merek" }
                            )

                            // 4. Ruang
                            DynamicDropdownField(
                                label = "Ruang",
                                selectedValue = ruangInput,
                                options = ruangList,
                                onValueChange = { ruangInput = it },
                                testTag = "input_barang_ruang",
                                onQuickAddClick = { showQuickAddType = "Ruang" }
                            )

                            // Sumber Dana & Kondisi (Grid 2-Kolom)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // 5. Sumber Dana Column
                                Column(modifier = Modifier.weight(1f)) {
                                    DynamicDropdownField(
                                        label = "Sumber Dana (Opsional)",
                                        selectedValue = sumberDanaInput,
                                        options = listOf("Belum Diketahui / Kosongkan") + sumberDanaList,
                                        onValueChange = { sumberDanaInput = it },
                                        testTag = "input_barang_sumber_dana",
                                        onQuickAddClick = { showQuickAddType = "Sumber Dana" }
                                    )
                                }

                                // 6. Kondisi Column
                                Column(modifier = Modifier.weight(1f)) {
                                    DynamicDropdownField(
                                        label = "Kondisi",
                                        selectedValue = kondisiInput,
                                        options = kondisiList,
                                        onValueChange = { kondisiInput = it },
                                        testTag = "input_barang_kondisi"
                                    )
                                }
                            }

                            // Stok Awal & Satuan (Grid 2-Kolom)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // 8. Stok Awal Column
                                Column(modifier = Modifier.weight(1f)) {
                                    LunarisTextField(
                                        value = initialStockInput,
                                        onValueChange = { initialStockInput = it },
                                        label = { Text("Stok Awal Fisik") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        isError = isAddStockInvalid,
                                        supportingText = {
                                            if (isAddStockInvalid) {
                                                Text("Stok awal min 1!", color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("input_barang_stok")
                                    )
                                }

                                // 7. Satuan Column
                                Column(modifier = Modifier.weight(1f)) {
                                    DynamicDropdownField(
                                        label = "Satuan",
                                        selectedValue = unitInput,
                                        options = units.map { it.name },
                                        onValueChange = { unitInput = it },
                                        testTag = "input_barang_satuan"
                                    )
                                }
                            }

                            // 9. Keterangan
                            LunarisTextField(
                                value = keteranganInput,
                                onValueChange = { keteranganInput = it },
                                label = { Text("Keterangan") },
                                placeholder = { Text("Keterangan tambahan...") },
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("input_barang_keterangan")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isBorrowableInput = !isBorrowableInput }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Switch(
                                    checked = isBorrowableInput,
                                    onCheckedChange = { isBorrowableInput = it }
                                )
                                Text(
                                    text = "Barang ini boleh dipinjam oleh Siswa",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val stock = initialStockInput.toIntOrNull()
                                if (nameInput.isBlank()) {
                                    Toast.makeText(context, "Nama barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (stock == null || stock < 1) {
                                    Toast.makeText(context, "Stok awal minimal harus diisi angka 1!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.registerNewItem(
                                    name = nameInput.trim(),
                                    stokAwal = stock,
                                    kategori = categoryInput,
                                    satuan = unitInput,
                                    merekAlat = merekAlatInput,
                                    ruang = ruangInput,
                                    sumberDana = if (sumberDanaInput == "Belum Diketahui / Kosongkan" || sumberDanaInput.isBlank()) null else sumberDanaInput,
                                    kondisi = kondisiInput,
                                    keterangan = keteranganInput,
                                    isBorrowable = isBorrowableInput,
                                    onSuccess = {
                                        Toast.makeText(context, "Alat baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                        showAddDialog = false
                                        isBorrowableInput = false // Reset on success
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Gagal: $err", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isAddStockInvalid && nameInput.isNotBlank(),
                            modifier = Modifier.testTag("dialog_btn_simpan_barang")
                        ) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Dialog Quick Add
            if (showQuickAddType != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showQuickAddType = null 
                        quickAddInputValue = ""
                    },
                    shape = RoundedCornerShape(16.dp),
                    title = { Text("Tambah ${showQuickAddType} Baru", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Masukkan nama ${showQuickAddType?.lowercase()} baru yang ingin ditambahkan.",
                                fontSize = 14.sp
                            )
                            LunarisTextField(
                                value = quickAddInputValue,
                                onValueChange = { quickAddInputValue = it },
                                label = { Text("Nama ${showQuickAddType}") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("quick_add_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val value = quickAddInputValue.trim()
                                if (value.isEmpty()) {
                                    Toast.makeText(context, "Nama tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val isDuplicate = when (showQuickAddType) {
                                    "Kategori" -> categories.any { it.name.equals(value, ignoreCase = true) }
                                    "Merek" -> merekAlatList.any { it.equals(value, ignoreCase = true) }
                                    "Ruang" -> ruangList.any { it.equals(value, ignoreCase = true) }
                                    "Sumber Dana" -> sumberDanaList.any { it.equals(value, ignoreCase = true) }
                                    else -> false
                                }
                                if (isDuplicate) {
                                    Toast.makeText(context, "Nama ${showQuickAddType?.lowercase()} sudah ada/terdaftar!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                when (showQuickAddType) {
                                    "Kategori" -> {
                                        viewModel.addCategory(
                                            name = value,
                                            onSuccess = {
                                                categoryInput = value
                                                showQuickAddType = null
                                                quickAddInputValue = ""
                                                Toast.makeText(context, "Kategori berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Gagal: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    "Merek" -> {
                                        val updated = (viewModel.merekAlat.value + value).distinct()
                                        viewModel.updateMerekAlat(updated)
                                        merekAlatInput = value
                                        showQuickAddType = null
                                        quickAddInputValue = ""
                                        Toast.makeText(context, "Merek berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                    }
                                    "Ruang" -> {
                                        val updated = (viewModel.ruang.value + value).distinct()
                                        viewModel.updateRuang(updated)
                                        ruangInput = value
                                        showQuickAddType = null
                                        quickAddInputValue = ""
                                        Toast.makeText(context, "Ruang berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                    }
                                    "Sumber Dana" -> {
                                        val updated = (viewModel.sumberDana.value + value).distinct()
                                        viewModel.updateSumberDana(updated)
                                        sumberDanaInput = value
                                        showQuickAddType = null
                                        quickAddInputValue = ""
                                        Toast.makeText(context, "Sumber Dana berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("quick_add_btn_simpan")
                        ) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showQuickAddType = null 
                            quickAddInputValue = ""
                        }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Dialog Edit Barang
            if (showEditDialog && selectedItemForEdit != null) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Ubah Data Alat", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "ID Alat: ${selectedItemForEdit!!.idBarang}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )

                            LunarisCard(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = GlassWhiteMore),
                                border = BorderStroke(1.5.dp, PastelLavender.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 1. Nama Alat
                                    Text(
                                        text = "Nama Alat",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    LunarisTextField(
                                        value = editNameInput,
                                        onValueChange = { editNameInput = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("edit_barang_nama")
                                    )

                                    // 2. Kategori
                                    Text(
                                        text = "Kategori",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    DynamicDropdownField(
                                        label = "Pilih Kategori",
                                        selectedValue = editCategoryInput,
                                        options = categories.map { it.name },
                                        onValueChange = { editCategoryInput = it },
                                        testTag = "edit_barang_kategori",
                                        onQuickAddClick = { showQuickAddType = "Kategori" }
                                    )
                                    // 3. Merek Alat
                                    Text(
                                        text = "Merek Alat",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    DynamicDropdownField(
                                        label = "Pilih Merek Alat",
                                        selectedValue = editMerekAlatInput,
                                        options = merekAlatList,
                                        onValueChange = { editMerekAlatInput = it },
                                        testTag = "edit_barang_merek",
                                        onQuickAddClick = { showQuickAddType = "Merek" }
                                    )

                                    // 4. Ruang
                                    Text(
                                        text = "Ruang",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    DynamicDropdownField(
                                        label = "Pilih Ruang",
                                        selectedValue = editRuangInput,
                                        options = ruangList,
                                        onValueChange = { editRuangInput = it },
                                        testTag = "edit_barang_ruang",
                                        onQuickAddClick = { showQuickAddType = "Ruang" }
                                    )

                                    // 5. Sumber Dana
                                    Text(
                                        text = "Sumber Dana",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    DynamicDropdownField(
                                        label = "Pilih Sumber Dana (Opsional)",
                                        selectedValue = editSumberDanaInput,
                                        options = listOf("Belum Diketahui / Kosongkan") + sumberDanaList,
                                        onValueChange = { editSumberDanaInput = it },
                                        testTag = "edit_barang_sumber_dana",
                                        onQuickAddClick = { showQuickAddType = "Sumber Dana" }
                                    )

                                    // 6. Kondisi
                                    Text(
                                        text = "Kondisi",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    DynamicDropdownField(
                                        label = "Pilih Kondisi",
                                        selectedValue = editKondisiInput,
                                        options = kondisiList,
                                        onValueChange = { editKondisiInput = it },
                                        testTag = "edit_barang_kondisi"
                                    )

                                    // 7. Satuan
                                    Text(
                                        text = "Satuan",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    DynamicDropdownField(
                                        label = "Pilih Satuan",
                                        selectedValue = editUnitInput,
                                        options = units.map { it.name },
                                        onValueChange = { editUnitInput = it },
                                        testTag = "edit_barang_satuan"
                                    )

                                    // 8. Stok Awal
                                    Text(
                                        text = "Stok Awal Fisik",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    LunarisTextField(
                                        value = editStockInput,
                                        onValueChange = { editStockInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        isError = isEditStockInvalid,
                                        supportingText = {
                                            if (isEditStockInvalid) {
                                                Text("Stok awal minimal harus diisi angka 1!", color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_barang_stok")
                                    )

                                    // 9. Keterangan
                                    Text(
                                        text = "Keterangan",
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    LunarisTextField(
                                        value = editKeteranganInput,
                                        onValueChange = { editKeteranganInput = it },
                                        placeholder = { Text("Keterangan tambahan...") },
                                        minLines = 3,
                                        maxLines = 5,
                                        modifier = Modifier.fillMaxWidth().testTag("edit_barang_keterangan")
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { editIsBorrowableInput = !editIsBorrowableInput }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                     ) {
                                         Switch(
                                             checked = editIsBorrowableInput,
                                             onCheckedChange = { editIsBorrowableInput = it }
                                         )
                                         Text(
                                             text = "Barang ini boleh dipinjam oleh Siswa",
                                             fontWeight = FontWeight.Medium,
                                             color = MaterialTheme.colorScheme.onSurface
                                         )
                                     }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val stock = editStockInput.toIntOrNull()
                                if (editNameInput.isBlank()) {
                                    Toast.makeText(context, "Nama barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (stock == null || stock < 1) {
                                    Toast.makeText(context, "Stok awal minimal harus diisi angka 1!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.updateItemDetails(
                                    idBarang = selectedItemForEdit!!.idBarang,
                                    namaBarang = editNameInput.trim(),
                                    kategori = editCategoryInput,
                                    satuan = editUnitInput,
                                    stokAwal = stock,
                                    merekAlat = editMerekAlatInput,
                                    ruang = editRuangInput,
                                    sumberDana = if (editSumberDanaInput == "Belum Diketahui / Kosongkan" || editSumberDanaInput.isBlank()) null else editSumberDanaInput,
                                    kondisi = editKondisiInput,
                                    keterangan = editKeteranganInput,
                                    isBorrowable = editIsBorrowableInput,
                                    onSuccess = {
                                        Toast.makeText(context, "Data alat berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                        showEditDialog = false
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isEditStockInvalid && editNameInput.isNotBlank(),
                            modifier = Modifier.testTag("dialog_btn_update_barang")
                        ) {
                            Text("Simpan Perubahan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Dialog Konfirmasi Hapus Barang (Berproteksi)
            if (showDeleteConfirmDialog && selectedItemForDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Hapus Alat", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("Apakah Anda yakin ingin menghapus alat '${selectedItemForDelete!!.namaBarang}'? Tindakan ini tidak dapat dibatalkan.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteItem(
                                    idBarang = selectedItemForDelete!!.idBarang,
                                    onSuccess = {
                                        Toast.makeText(context, "Alat berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                        showDeleteConfirmDialog = false
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Gagal menghapus: $err", Toast.LENGTH_LONG).show()
                                        showDeleteConfirmDialog = false
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("dialog_btn_konfirmasi_hapus")
                        ) {
                            Text("Hapus")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Dialog Perbaiki Barang Rusak
            if (showRepairDialog && selectedItemForRepair != null) {
                AlertDialog(
                    onDismissRequest = { showRepairDialog = false },
                    title = { Text("Perbaiki Alat Rusak", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Barang: ${selectedItemForRepair!!.namaBarang}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Stok Rusak Saat Ini: ${selectedItemForRepair!!.stokRusak} ${selectedItemForRepair!!.satuan.ifEmpty { "Pcs" }}",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )

                            val isRepairQtyInvalid = repairQtyInput.trim().toIntOrNull() == null || 
                                    (repairQtyInput.trim().toIntOrNull() ?: 0) <= 0 ||
                                    (repairQtyInput.trim().toIntOrNull() ?: 0) > selectedItemForRepair!!.stokRusak

                            LunarisTextField(
                                value = repairQtyInput,
                                onValueChange = { repairQtyInput = it },
                                label = { Text("Jumlah Diperbaiki") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = isRepairQtyInvalid,
                                supportingText = {
                                    if (isRepairQtyInvalid) {
                                        Text("Masukkan jumlah valid (1 s/d ${selectedItemForRepair!!.stokRusak})", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("repair_barang_qty")
                            )
                        }
                    },
                    confirmButton = {
                        val isQtyValid = repairQtyInput.trim().toIntOrNull() != null && 
                                (repairQtyInput.trim().toIntOrNull() ?: 0) > 0 &&
                                (repairQtyInput.trim().toIntOrNull() ?: 0) <= selectedItemForRepair!!.stokRusak

                        Button(
                            onClick = {
                                val qtyToRepair = repairQtyInput.trim().toIntOrNull() ?: 0
                                viewModel.repairStokRusak(selectedItemForRepair!!.idBarang, qtyToRepair) {
                                    Toast.makeText(context, "Stok rusak berhasil diperbaiki!", Toast.LENGTH_SHORT).show()
                                    showRepairDialog = false
                                }
                            },
                            enabled = isQtyValid,
                            modifier = Modifier.testTag("dialog_btn_repair_barang")
                        ) {
                            Text("Perbaiki")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRepairDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }
            
            if (showFilterDialog) {
                LunarisFilterDialog(
                    onDismissRequest = {
                        tempSelectedCategory = appliedCategory
                        tempSelectedRoom = appliedRoom
                        tempSelectedCondition = appliedCondition
                        showFilterDialog = false
                    },
                    filterGroups = listOf(
                        FilterGroup(
                            title = "Kategori",
                            options = categoriesOptions,
                            selectedOption = tempSelectedCategory,
                            onOptionSelected = { tempSelectedCategory = it }
                        ),
                        FilterGroup(
                            title = "Ruang / Lokasi",
                            options = roomOptions,
                            selectedOption = tempSelectedRoom,
                            onOptionSelected = { tempSelectedRoom = it }
                        ),
                        FilterGroup(
                            title = "Status Kondisi",
                            options = conditionOptions,
                            selectedOption = tempSelectedCondition,
                            onOptionSelected = { tempSelectedCondition = it }
                        )
                    ),
                    onReset = {
                        tempSelectedCategory = "Semua Kategori"
                        tempSelectedRoom = "Semua Ruang"
                        tempSelectedCondition = "Semua Kondisi"
                    },
                    onApply = {
                        appliedCategory = tempSelectedCategory
                        appliedRoom = tempSelectedRoom
                        appliedCondition = tempSelectedCondition
                        showFilterDialog = false
                    }
                )
            }
        }
    }
}

private fun parseCsvLine(line: String, delimiter: Char): List<String> {
    val result = mutableListOf<String>()
    var current = java.lang.StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '\"') {
            inQuotes = !inQuotes
        } else if (c == delimiter && !inQuotes) {
            result.add(current.toString().trim().removeSurrounding("\""))
            current = java.lang.StringBuilder()
        } else {
            current.append(c)
        }
        i++
    }
    result.add(current.toString().trim().removeSurrounding("\""))
    return result
}
