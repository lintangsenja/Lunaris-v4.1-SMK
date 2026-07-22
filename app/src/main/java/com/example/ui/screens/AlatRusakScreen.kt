package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.data.entity.DamagedItemEntity
import com.example.data.model.ItemWithStock
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.theme.GlassWhiteMore
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.CarbonBlackText
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlatRusakScreen(
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
    val alatItems = remember(allItems) {
        allItems.filter { !it.kategori.equals("Logistik", ignoreCase = true) }
    }
    val rawHistoryList by viewModel.allDamagedItems.collectAsState()
    // Filter history specifically for broken tools (Alat Rusak)
    val historyList = remember(rawHistoryList) {
        rawHistoryList.filter {
            !it.status.equals("Servis Luar/Pemeliharaan", ignoreCase = true) &&
            !it.status.equals("Pemeliharaan", ignoreCase = true) &&
            !it.status.equals("Normal (Tersedia)", ignoreCase = true)
        }
    }
    val defaultOfficerState by viewModel.defaultOfficer.collectAsState()
    val defaultOfficer = defaultOfficerState.ifBlank { "Administrator" }

    var showQrScanner by remember { mutableStateOf(false) }

    // Tab state
    var selectedTabState by remember { mutableStateOf(0) }

    // Form inputs
    var alatSearchQuery by remember { mutableStateOf("") }
    var selectedAlat by remember { mutableStateOf<ItemWithStock?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var jumlahRusakInput by remember { mutableStateOf("") }
    var keteranganInput by remember { mutableStateOf("") }
    var petugasInput by remember { mutableStateOf(defaultOfficer) }

    // DatePicker setup
    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)) }
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Form validation
    val stokTersedia = selectedAlat?.stokTersedia ?: 0
    val jumlahRusak = jumlahRusakInput.toIntOrNull() ?: 0
    val isJumlahInvalid = remember(jumlahRusakInput, selectedAlat) {
        if (jumlahRusakInput.isEmpty()) false
        else {
            jumlahRusak <= 0 || (selectedAlat != null && jumlahRusak > stokTersedia)
        }
    }

    val canSubmit = selectedAlat != null &&
            jumlahRusak > 0 &&
            jumlahRusak <= stokTersedia &&
            !isJumlahInvalid &&
            keteranganInput.isNotBlank() &&
            petugasInput.isNotBlank()

    // Filtered assets suggestions for searchable dropdown
    val filteredAlat = remember(alatSearchQuery, alatItems) {
        if (alatSearchQuery.isBlank()) {
            alatItems
        } else {
            alatItems.filter { it.namaBarang.contains(alatSearchQuery, ignoreCase = true) }
        }
    }

    // Advanced Data Table States
    var historySearchQuery by remember { mutableStateOf("") }
    var showHistoryQrScanner by remember { mutableStateOf(false) }

    // Confirm dialog states
    var itemToNormal by remember { mutableStateOf<DamagedItemEntity?>(null) }
    var itemToMaintenance by remember { mutableStateOf<DamagedItemEntity?>(null) }
    var itemToDeletePermanently by remember { mutableStateOf<DamagedItemEntity?>(null) }

    // Maintenance action states
    var maintenanceNote by remember { mutableStateOf("") }
    var maintenanceOfficer by remember { mutableStateOf(defaultOfficer) }

    // Filter history based on search query
    val filteredHistory = remember(historyList, historySearchQuery) {
        if (historySearchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.namaBarang.contains(historySearchQuery, ignoreCase = true) ||
                it.idBarang.contains(historySearchQuery, ignoreCase = true) ||
                it.keteranganKerusakan.contains(historySearchQuery, ignoreCase = true) ||
                it.status.contains(historySearchQuery, ignoreCase = true)
            }
        }
    }

    val selectedTabColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
    val unselectedTabColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.8f)

    // Main layout
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
                                text = "Kelola Alat Rusak",
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
                    TabRow(
                        selectedTabIndex = selectedTabState,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                                height = 3.dp,
                                color = selectedTabColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTabState == 0,
                            onClick = { selectedTabState = 0 },
                            text = {
                                Text(
                                    text = "Tambah Alat Rusak",
                                    fontWeight = if (selectedTabState == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabState == 0) selectedTabColor else unselectedTabColor,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_tambah_alat_rusak")
                        )
                        Tab(
                            selected = selectedTabState == 1,
                            onClick = { selectedTabState = 1 },
                            text = {
                                Text(
                                    text = "Riwayat Alat Rusak",
                                    fontWeight = if (selectedTabState == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabState == 1) selectedTabColor else unselectedTabColor,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_riwayat_alat_rusak")
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabState) {
                0 -> {
                    // Form Tab Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            LunarisCard(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Form Lapor Kerusakan Alat",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                                        fontSize = 18.sp
                                    )

                                    // Input A: Nama Alat (Searchable Dropdown + QR Button)
                                    Text(
                                        text = "Pilih Alat",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            LunarisTextField(
                                                value = if (selectedAlat != null && !dropdownExpanded) selectedAlat!!.namaBarang else alatSearchQuery,
                                                onValueChange = {
                                                    alatSearchQuery = it
                                                    selectedAlat = null
                                                    dropdownExpanded = true
                                                },
                                                placeholder = { Text("Ketik nama alat...") },
                                                trailingIcon = {
                                                    IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                                        Icon(
                                                            imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                            contentDescription = "Pilih"
                                                        )
                                                    }
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(16.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("alat_rusak_select_input")
                                            )

                                            DropdownMenu(
                                                expanded = dropdownExpanded,
                                                onDismissRequest = { dropdownExpanded = false },
                                                properties = PopupProperties(focusable = false),
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                if (filteredAlat.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Alat tidak ditemukan") },
                                                        onClick = { dropdownExpanded = false }
                                                    )
                                                } else {
                                                    filteredAlat.forEach { item ->
                                                        DropdownMenuItem(
                                                            text = { Text("${item.namaBarang} (Stok Ready: ${item.stokTersedia} ${item.satuan})") },
                                                            onClick = {
                                                                selectedAlat = item
                                                                alatSearchQuery = item.namaBarang
                                                                dropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // QR Code Scanner button
                                        IconButton(
                                            onClick = { showQrScanner = true },
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(
                                                    if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF3E8FF),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .testTag("btn_alat_rusak_qr")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = "Pindai QR",
                                                tint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else DeepPurpleText
                                            )
                                        }
                                    }

                                    if (selectedAlat != null) {
                                        Text(
                                            text = "Stok Tersedia: ${selectedAlat!!.stokTersedia} ${selectedAlat!!.satuan}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedAlat!!.stokTersedia > 0) Color(0xFF059669) else Color.Red,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }

                                    // Row 1: Jumlah Rusak & Tanggal Kerusakan
                                                                        Row(
                                                                            modifier = Modifier.fillMaxWidth(),
                                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                            verticalAlignment = Alignment.Top
                                                                        ) {
                                                                            // Input B: Jumlah Rusak
                                                                            Column(modifier = Modifier.weight(1f)) {
                                                                                Text(
                                                                                    text = "Jumlah Rusak",
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                                                                    fontSize = 14.sp,
                                                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                                                )
                                                                                LunarisTextField(
                                                                                    value = jumlahRusakInput,
                                                                                    onValueChange = { jumlahRusakInput = it },
                                                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                                    placeholder = { Text("Jumlah rusak") },
                                                                                    singleLine = true,
                                                                                    isError = isJumlahInvalid,
                                                                                    shape = RoundedCornerShape(16.dp),
                                                                                    colors = OutlinedTextFieldDefaults.colors(
                                                                                        focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                                                        unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                                                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                                                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                                                    ),
                                                                                    supportingText = {
                                                                                        if (isJumlahInvalid) {
                                                                                            Text(
                                                                                                "Jumlah harus > 0 dan tidak boleh melebihi stok tersedia ($stokTersedia)!",
                                                                                                color = MaterialTheme.colorScheme.error,
                                                                                                fontSize = 11.sp
                                                                                            )
                                                                                        }
                                                                                    },
                                                                                    modifier = Modifier
                                                                                        .fillMaxWidth()
                                                                                        .testTag("alat_rusak_jumlah_input")
                                                                                )
                                                                            }
                                    
                                                                            // Input D: Tanggal Kerusakan
                                                                            Column(modifier = Modifier.weight(1f)) {
                                                                                Text(
                                                                                    text = "Tanggal Kerusakan",
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                                                                    fontSize = 14.sp,
                                                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                                                )
                                                                                LunarisTextField(
                                                                                    value = selectedDate,
                                                                                    onValueChange = {},
                                                                                    readOnly = true,
                                                                                    shape = RoundedCornerShape(16.dp),
                                                                                    trailingIcon = {
                                                                                        IconButton(onClick = { datePickerDialog.show() }) {
                                                                                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pilih Tanggal")
                                                                                        }
                                                                                    },
                                                                                    colors = OutlinedTextFieldDefaults.colors(
                                                                                        focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                                                        unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                                                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                                                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                                                    ),
                                                                                    modifier = Modifier
                                                                                        .fillMaxWidth()
                                                                                        .clickable { datePickerDialog.show() }
                                                                                        .testTag("alat_rusak_tanggal_input")
                                                                                )
                                                                            }
                                                                        }
                                    
                                                                        // Input C: Keterangan / Kronologi Kerusakan (Full Width)
                                                                        Text(
                                                                            text = "Kronologi / Keterangan Kerusakan",
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                                                            fontSize = 14.sp,
                                                                            modifier = Modifier.padding(top = 4.dp)
                                                                        )
                                                                        LunarisTextField(
                                                                            value = keteranganInput,
                                                                            onValueChange = { keteranganInput = it },
                                                                            placeholder = { Text("Contoh: Gagang retak setelah jatuh dari meja laboratorium") },
                                                                            minLines = 2,
                                                                            shape = RoundedCornerShape(16.dp),
                                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                                            ),
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .testTag("alat_rusak_keterangan_input")
                                                                        )

                                    // Input E: Nama Petugas
                                    Text(
                                        text = "Nama Petugas",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                        fontSize = 14.sp
                                    )
                                    LunarisTextField(
                                        value = petugasInput,
                                        onValueChange = { petugasInput = it },
                                        placeholder = { Text("Nama petugas pelapor") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("alat_rusak_petugas_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Submit Button
                                    Button(
                                        onClick = {
                                            val tool = selectedAlat!!
                                            val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
                                            val currentTime = timeFormat.format(Date())

                                            viewModel.recordDamagedReport(
                                                idBarang = tool.idBarang,
                                                namaBarang = tool.namaBarang,
                                                jumlah = jumlahRusak,
                                                tanggalKerusakan = selectedDate,
                                                waktuKerusakan = currentTime,
                                                keteranganKerusakan = keteranganInput,
                                                namaPetugas = petugasInput,
                                                kondisiBaru = "Rusak",
                                                onSuccess = {
                                                    Toast.makeText(context, "Kerusakan berhasil dilaporkan!", Toast.LENGTH_LONG).show()
                                                    // Reset Form
                                                    selectedAlat = null
                                                    alatSearchQuery = ""
                                                    jumlahRusakInput = ""
                                                    keteranganInput = ""
                                                    petugasInput = defaultOfficer
                                                    // Move to History Tab
                                                    selectedTabState = 1
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        enabled = canSubmit,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.Gray.copy(alpha = 0.3f),
                                            disabledContentColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("btn_submit_alat_rusak")
                                    ) {
                                        Text(
                                            text = "Laporkan Kerusakan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // History Tab Content (Data Table with Search, Pagination, Row Controller)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search Bar & Row Controller Row
                        LunarisTextField(
                            value = historySearchQuery,
                            onValueChange = { 
                                historySearchQuery = it
                            },
                            placeholder = { Text("Cari riwayat alat rusak...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                            trailingIcon = {
                                IconButton(onClick = { showHistoryQrScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan QR",
                                        tint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED)
                                    )
                                }
                            },
                            singleLine = true,
                            isStaticOutline = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("alat_rusak_search_bar")
                        )

                        if (showHistoryQrScanner) {
                            SearchQrScanDialog(
                                onDismiss = { showHistoryQrScanner = false },
                                onQrScanned = { scannedCode ->
                                    showHistoryQrScanner = false
                                    historySearchQuery = scannedCode
                                }
                            )
                        }

                        // Data Table Content
                        if (filteredHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada riwayat alat rusak yang cocok.",
                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredHistory) { item ->
                                    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
                                    LunarisCard(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
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
                                                        color = if (isDark) MaterialTheme.colorScheme.onSurface else CarbonBlackText
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "ID: ${item.idBarang} | Jumlah: ${item.jumlah} Pcs",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                    )
                                                }

                                                // Status Indicator Text-only
                                                Text(
                                                    text = item.status,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
                                                )
                                            }

                                            Text(
                                                text = "Kronologi: ${item.keteranganKerusakan}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.DarkGray
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Lapor: ${item.tanggalKerusakan} (${item.namaPetugas})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )

                                                // Actions Buttons Row (no background boxes)
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Normal (Kembali ke Stok)
                                                    IconButton(
                                                        onClick = { itemToNormal = item },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .testTag("btn_normal_${item.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Normal (Kembalikan ke Stok)",
                                                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    // Servis / Pemeliharaan
                                                    IconButton(
                                                        onClick = { 
                                                            itemToMaintenance = item
                                                            maintenanceNote = ""
                                                            maintenanceOfficer = defaultOfficer
                                                        },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .testTag("btn_servis_${item.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Build,
                                                            contentDescription = "Servis / Pemeliharaan",
                                                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    // Hapus Permanen
                                                    IconButton(
                                                        onClick = { itemToDeletePermanently = item },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .testTag("btn_hapus_${item.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Hapus Permanen",
                                                            tint = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444),
                                                            modifier = Modifier.size(20.dp)
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
            }
        }

        // QR Scanner Dialog
        if (showQrScanner) {
            BahanAfkirQrScanDialog(
                onDismiss = { showQrScanner = false },
                onQrScanned = { scannedCode ->
                    showQrScanner = false
                    val matched = alatItems.find { it.idBarang == scannedCode }
                    if (matched != null) {
                        selectedAlat = matched
                        alatSearchQuery = matched.namaBarang
                        Toast.makeText(context, "Alat '${matched.namaBarang}' terdeteksi!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Alat dengan ID '$scannedCode' tidak terdaftar atau merupakan bahan habis pakai!", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // Dialog 1: Confirmation to Return to Stock (Normal)
        if (itemToNormal != null) {
            AlertDialog(
                onDismissRequest = { itemToNormal = null },
                title = { Text("Konfirmasi Kembali Normal", fontWeight = FontWeight.Bold) },
                text = { 
                    Text("Apakah Anda yakin alat '${itemToNormal!!.namaBarang}' sudah kembali normal?\n\nAlat sebanyak ${itemToNormal!!.jumlah} unit akan ditarik dari daftar alat rusak dan dikembalikan ke stok aktif.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToNormal!!
                            itemToNormal = null
                            viewModel.cancelDamagedReport(
                                id = record.id,
                                onSuccess = {
                                    Toast.makeText(context, "Alat berhasil dikembalikan ke stok normal!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Kembalikan ke Stok", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToNormal = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog 2: Confirmation & Note Input for Servis / Pemeliharaan
        if (itemToMaintenance != null) {
            AlertDialog(
                onDismissRequest = { itemToMaintenance = null },
                title = { Text("Kirim ke Pemeliharaan", fontWeight = FontWeight.Bold) },
                text = { 
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Silakan lengkapi catatan pemeliharaan untuk alat '${itemToMaintenance!!.namaBarang}':")
                        
                        LunarisTextField(
                            value = maintenanceNote,
                            onValueChange = { maintenanceNote = it },
                            placeholder = { Text("Tuliskan catatan pemeliharaan (misal: perlu teknisi khusus, ganti suku cadang)") },
                            label = { Text("Catatan Pemeliharaan") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        LunarisTextField(
                            value = maintenanceOfficer,
                            onValueChange = { maintenanceOfficer = it },
                            placeholder = { Text("Nama petugas pemeliharaan") },
                            label = { Text("Petugas") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToMaintenance!!
                            itemToMaintenance = null
                            val note = maintenanceNote.ifBlank { "Proses Servis / Pemeliharaan" }
                            viewModel.updateDamagedStatus(
                                damagedId = record.id,
                                newStatus = "Servis Luar/Pemeliharaan",
                                alasan = note,
                                namaPetugas = maintenanceOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Alat berhasil dipindahkan ke modul Pemeliharaan!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text("Kirim ke Pemeliharaan", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToMaintenance = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog 3: Permanent physical delete confirmation (Hapus)
        if (itemToDeletePermanently != null) {
            AlertDialog(
                onDismissRequest = { itemToDeletePermanently = null },
                title = { Text("Hapus Permanen", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
                text = { 
                    Text("Apakah Anda yakin ingin menghapus fisik alat '${itemToDeletePermanently!!.namaBarang}' sebanyak ${itemToDeletePermanently!!.jumlah} unit secara permanen?\n\nAksi ini akan mengurangi jumlah total aset fisik Anda secara permanen. Tindakan ini akan dicatat di Log Transaksi untuk audit.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToDeletePermanently!!
                            itemToDeletePermanently = null
                            viewModel.deleteDamagedItemPermanently(
                                id = record.id,
                                namaPetugas = defaultOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Aset dihapus secara permanen dan dicatat di log transaksi!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Hapus Permanen", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDeletePermanently = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
