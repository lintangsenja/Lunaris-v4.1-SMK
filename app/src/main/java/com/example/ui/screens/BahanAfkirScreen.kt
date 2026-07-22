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
import com.example.data.entity.BahanAfkirEntity
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BahanAfkirScreen(
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
    val logistikItems = remember(allItems) {
        allItems.filter { it.kategori.equals("Logistik", ignoreCase = true) }
    }
    val historyList by viewModel.allBahanAfkir.collectAsState()
    val defaultOfficerState by viewModel.defaultOfficer.collectAsState()
    val defaultOfficer = defaultOfficerState.ifBlank { "Administrator" }

    var showQrScanner by remember { mutableStateOf(false) }

    // Tab state
    var selectedTabState by remember { mutableStateOf(0) }

    // Form inputs
    var bahanSearchQuery by remember { mutableStateOf("") }
    var selectedBahan by remember { mutableStateOf<ItemWithStock?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var jumlahAfkirInput by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("Kedaluwarsa") }
    var selectedSatuan by remember { mutableStateOf("-") }

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

    // Update Satuan auto-lock
    LaunchedEffect(selectedBahan) {
        if (selectedBahan != null) {
            selectedSatuan = selectedBahan!!.satuan
        } else {
            selectedSatuan = "-"
        }
    }

    // Form validation
    val stokTersedia = selectedBahan?.stokTersedia ?: 0
    val jumlahAfkir = jumlahAfkirInput.toIntOrNull() ?: 0
    val isJumlahInvalid = remember(jumlahAfkirInput, selectedBahan) {
        if (jumlahAfkirInput.isEmpty()) false
        else {
            jumlahAfkir <= 0 || (selectedBahan != null && jumlahAfkir > stokTersedia)
        }
    }

    val canSubmit = selectedBahan != null &&
            jumlahAfkir > 0 &&
            jumlahAfkir <= stokTersedia &&
            !isJumlahInvalid

    // Filtered materials suggestions for searchable dropdown
    val filteredBahan = remember(bahanSearchQuery, logistikItems) {
        if (bahanSearchQuery.isBlank()) {
            logistikItems
        } else {
            logistikItems.filter { it.namaBarang.contains(bahanSearchQuery, ignoreCase = true) }
        }
    }

    // Advanced Data Table States
    var historySearchQuery by remember { mutableStateOf("") }
    var showHistoryQrScanner by remember { mutableStateOf(false) }

    // Confirm dialog states
    var itemToUndo by remember { mutableStateOf<BahanAfkirEntity?>(null) }
    var itemToDeletePermanently by remember { mutableStateOf<BahanAfkirEntity?>(null) }

    // Filter history based on search query
    val filteredHistory = remember(historyList, historySearchQuery) {
        if (historySearchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.namaBarang.contains(historySearchQuery, ignoreCase = true) ||
                it.idAfkir.contains(historySearchQuery, ignoreCase = true) ||
                it.alasan.contains(historySearchQuery, ignoreCase = true) ||
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
                                text = "Manajemen Bahan Afkir",
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 0) selectedTabColor else unselectedTabColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Catat Afkir",
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTabState == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 0) selectedTabColor else unselectedTabColor
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_catat_afkir")
                        )
                        Tab(
                            selected = selectedTabState == 1,
                            onClick = { selectedTabState = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = if (selectedTabState == 1) selectedTabColor else unselectedTabColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Riwayat",
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTabState == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabState == 1) selectedTabColor else unselectedTabColor
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_riwayat_afkir")
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

            AnimatedVisibility(visible = selectedTabState == 0) {
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
                                    text = "Catat Bahan Afkir Baru",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText,
                                    fontSize = 18.sp
                                )

                                // Input A: Nama Bahan (Searchable Dropdown + QR Scanner button)
                                Text(
                                    text = "Nama Bahan",
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
                                            value = if (selectedBahan != null && !dropdownExpanded) selectedBahan!!.namaBarang else bahanSearchQuery,
                                            onValueChange = {
                                                bahanSearchQuery = it
                                                selectedBahan = null // Reset selected item if user types manually
                                                dropdownExpanded = true
                                            },
                                            placeholder = { Text("Ketik nama bahan...") },
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
                                                .testTag("afkir_search_input")
                                        )

                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            properties = PopupProperties(focusable = false),
                                            modifier = Modifier.fillMaxWidth(0.85f)
                                        ) {
                                            if (filteredBahan.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Bahan tidak ditemukan") },
                                                    onClick = { dropdownExpanded = false }
                                                )
                                            } else {
                                                filteredBahan.forEach { item ->
                                                    DropdownMenuItem(
                                                        text = { Text("${item.namaBarang} (Stok: ${item.stokTersedia} ${item.satuan})") },
                                                        onClick = {
                                                            selectedBahan = item
                                                            bahanSearchQuery = item.namaBarang
                                                            dropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // QR Scanner Shortcut
                                    IconButton(
                                        onClick = { showQrScanner = true },
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(
                                                if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF3E8FF),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .testTag("btn_afkir_qr")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Pindai QR",
                                            tint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else DeepPurpleText
                                        )
                                    }
                                }

                                // Real-time stock label
                                if (selectedBahan != null) {
                                    Text(
                                        text = "Stok Tersedia: ${selectedBahan!!.stokTersedia} ${selectedBahan!!.satuan}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedBahan!!.stokTersedia > 0) Color(0xFF059669) else Color.Red,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                // Row 1: Jumlah Afkir & Satuan
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                    verticalAlignment = Alignment.Top
                                                                ) {
                                                                    // Input B: Jumlah Afkir
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = "Jumlah Afkir",
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                                                            fontSize = 14.sp,
                                                                            modifier = Modifier.padding(bottom = 4.dp)
                                                                        )
                                                                        LunarisTextField(
                                                                            value = jumlahAfkirInput,
                                                                            onValueChange = { jumlahAfkirInput = it },
                                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                            placeholder = { Text("Masukkan jumlah") },
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
                                                                                        color = MaterialTheme.colorScheme.error
                                                                                    )
                                                                                }
                                                                            },
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .testTag("afkir_jumlah_input")
                                                                        )
                                                                    }
                                
                                                                    // Input C: Satuan (Locked display)
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = "Satuan",
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                                                            fontSize = 14.sp,
                                                                            modifier = Modifier.padding(bottom = 4.dp)
                                                                        )
                                                                        LunarisTextField(
                                                                            value = selectedSatuan,
                                                                            onValueChange = {},
                                                                            readOnly = true,
                                                                            enabled = false,
                                                                            shape = RoundedCornerShape(16.dp),
                                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                                disabledTextColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else Color.DarkGray,
                                                                                disabledBorderColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                                                                disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.15f)
                                                                            ),
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .testTag("afkir_satuan_input")
                                                                        )
                                                                    }
                                                                }
                                
                                                                // Row 2: Alasan Afkir & Tanggal Pencatatan
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                    verticalAlignment = Alignment.Top
                                                                ) {
                                                                    // Input D: Alasan (Dropdown)
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = "Alasan Afkir",
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else DeepPurpleText,
                                                                            fontSize = 14.sp,
                                                                            modifier = Modifier.padding(bottom = 4.dp)
                                                                        )
                                                                        DynamicDropdownField(
                                                                            label = "Pilih Alasan",
                                                                            selectedValue = selectedReason,
                                                                            options = listOf("Kedaluwarsa", "Rusak Fisik", "Hilang"),
                                                                            onValueChange = { selectedReason = it },
                                                                            testTag = "afkir_alasan_dropdown"
                                                                        )
                                                                    }
                                
                                                                    // Input E: Tanggal Afkir (DatePicker)
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = "Tanggal Pencatatan",
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
                                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF7C3AED),
                                                                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color.Gray.copy(alpha = 0.4f),
                                                                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                                                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                                                            ),
                                                                            trailingIcon = {
                                                                                IconButton(onClick = { datePickerDialog.show() }) {
                                                                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                                                                                }
                                                                            },
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .clickable { datePickerDialog.show() }
                                                                                .testTag("afkir_tanggal_input")
                                                                        )
                                                                    }
                                                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Submit Button
                                Button(
                                    onClick = {
                                        if (!canSubmit) return@Button
                                        viewModel.recordBahanAfkir(
                                            idBarang = selectedBahan!!.idBarang,
                                            namaBarang = selectedBahan!!.namaBarang,
                                            jumlahAfkir = jumlahAfkir,
                                            satuan = selectedSatuan,
                                            alasan = selectedReason,
                                            tanggalAfkir = selectedDate,
                                            onSuccess = {
                                                Toast.makeText(context, "Bahan afkir berhasil dicatat!", Toast.LENGTH_SHORT).show()
                                                // Reset Form inputs
                                                jumlahAfkirInput = ""
                                                selectedBahan = null
                                                bahanSearchQuery = ""
                                                // Automatically navigate to history tab to view history
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
                                        disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.Gray.copy(alpha = 0.4f),
                                        disabledContentColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("btn_simpan_afkir")
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Simpan")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simpan Bahan Afkir", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedTabState == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
                ) {
                    // Search Bar & Row Controller
                    LunarisTextField(
                        value = historySearchQuery,
                        onValueChange = { 
                            historySearchQuery = it
                        },
                        placeholder = { Text("Cari riwayat afkir...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                            )
                        },
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
                            .padding(bottom = 12.dp)
                            .testTag("riwayat_search_bar")
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

                    // Riwayat Data Table
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (filteredHistory.isEmpty()) {
                            item {
                                LunarisCard(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "Kosong",
                                            tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (historySearchQuery.isBlank()) "Belum ada riwayat bahan afkir." else "Pencarian tidak ditemukan.",
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredHistory) { record ->
                                val isCanceled = record.status == "Dibatalkan"
                                val cardBg = if (isDark) {
                                    if (isCanceled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    if (isCanceled) Color(0xFFF1F5F9) else Color(0xFFF8FAFC)
                                }
                                LunarisCard(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("afkir_item_${record.idAfkir}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = record.idAfkir,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCanceled) (if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.Gray) else (if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText),
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = record.tanggalAfkir,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = record.namaBarang,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isCanceled) (if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.Gray) else (if (isDark) MaterialTheme.colorScheme.onSurface else CarbonBlackText)
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "${record.jumlahAfkir} ${record.satuan}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isCanceled) (if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color.Gray) else (if (isDark) MaterialTheme.colorScheme.onSurface else Color.DarkGray)
                                                )

                                                // Status text-only
                                                val statusColor = if (isCanceled) {
                                                    if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else Color(0xFF64748B)
                                                } else {
                                                    when (record.alasan) {
                                                        "Kedaluwarsa" -> if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
                                                        "Rusak Fisik" -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                                        else -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                                    }
                                                }
                                                Text(
                                                    text = if (isCanceled) "Dibatalkan" else record.alasan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Undo Action Button (No background boxes)
                                            IconButton(
                                                onClick = { itemToUndo = record },
                                                enabled = !isCanceled,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_undo_afkir_${record.idAfkir}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Undo,
                                                    contentDescription = "Batalkan Afkir",
                                                    tint = if (isCanceled) (if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f)) else (if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF4F46E5)),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Permanent Physical Delete Button (No background boxes)
                                            IconButton(
                                                onClick = { itemToDeletePermanently = record },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("btn_delete_afkir_${record.idAfkir}")
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

        // Qr Scan Dialog
        if (showQrScanner) {
            BahanAfkirQrScanDialog(
                onDismiss = { showQrScanner = false },
                onQrScanned = { scannedCode ->
                    showQrScanner = false
                    val matched = logistikItems.find { it.idBarang == scannedCode }
                    if (matched != null) {
                        selectedBahan = matched
                        bahanSearchQuery = matched.namaBarang
                        Toast.makeText(context, "Bahan '${matched.namaBarang}' terdeteksi!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Bahan dengan ID '$scannedCode' tidak terdaftar atau bukan bahan habis pakai!", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // 1. Confirm Undo / Kembalikan Dialog
        if (itemToUndo != null) {
            AlertDialog(
                onDismissRequest = { itemToUndo = null },
                title = { Text("Konfirmasi Kembalikan", fontWeight = FontWeight.Bold) },
                text = { 
                    Text("Apakah Anda yakin ingin membatalkan status afkir untuk bahan ini?\n\nStok sebanyak ${itemToUndo!!.jumlahAfkir} ${itemToUndo!!.satuan} akan dikembalikan ke master stok fisik utama, dan status log ini akan diubah menjadi 'Dibatalkan'.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToUndo!!
                            itemToUndo = null
                            viewModel.undoBahanAfkir(
                                idAfkir = record.idAfkir,
                                onSuccess = {
                                    Toast.makeText(context, "Afkir berhasil dibatalkan dan stok dikembalikan!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                    ) {
                        Text("Kembalikan Stok", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToUndo = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // 2. Confirm Permanent Physical Delete Dialog
        if (itemToDeletePermanently != null) {
            AlertDialog(
                onDismissRequest = { itemToDeletePermanently = null },
                title = { Text("Hapus Permanen", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48)) },
                text = { 
                    Text("Apakah Anda yakin ingin menghapus data afkir '${itemToDeletePermanently!!.namaBarang}' secara permanen?\n\nAksi ini tidak dapat dibatalkan, dan bukti penghapusan fisik akan dicatat di Log Transaksi untuk audit.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val record = itemToDeletePermanently!!
                            itemToDeletePermanently = null
                            viewModel.deleteBahanAfkirPermanently(
                                idAfkir = record.idAfkir,
                                namaPetugas = defaultOfficer,
                                onSuccess = {
                                    Toast.makeText(context, "Catatan afkir dihapus permanen & audit dicatat!", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
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
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BahanAfkirQrScanDialog(
    onDismiss: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pindai QR Bahan", fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    val newFlash = !isFlashOn
                    isFlashOn = newFlash
                    cameraControl?.enableTorch(newFlash)
                }) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Senter"
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (cameraPermissionState.status.isGranted) {
                    CameraPreviewView(
                        onQrScanned = { barcode ->
                            onQrScanned(barcode)
                        },
                        isFlashOn = isFlashOn,
                        onCameraControlReady = { cameraControl = it }
                    )
                } else {
                    Text(
                        "Izin kamera diperlukan untuk memindai QR Code.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
