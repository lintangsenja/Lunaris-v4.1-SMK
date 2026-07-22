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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.data.entity.CategoryEntity
import com.example.data.entity.PemakaianBahanEntity
import com.example.data.entity.UnitEntity
import com.example.data.model.ItemWithStock
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.GlassWhiteMore
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.PastelLavender
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PemakaianBahanScreen(
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

    var selectedTab by remember { mutableIntStateOf(0) }

    val allItems by viewModel.itemsWithStock.collectAsState()
    val logistikItems = remember(allItems) {
        allItems.filter { it.kategori.equals("Logistik", ignoreCase = true) }
    }
    val historyList by viewModel.allPemakaianBahan.collectAsState()
    
    var historySearchQuery by remember { mutableStateOf("") }
    var showHistoryQrScanner by remember { mutableStateOf(false) }

    val filteredHistory = remember(historyList, historySearchQuery) {
        historyList.filter { record ->
            record.namaBarang.contains(historySearchQuery, ignoreCase = true) ||
            record.idPemakaian.contains(historySearchQuery, ignoreCase = true) ||
            record.namaPeminta.contains(historySearchQuery, ignoreCase = true)
        }
    }

    val units by viewModel.allUnits.collectAsState()

    var showQrScanner by remember { mutableStateOf(false) }

    // Form inputs
    var bahanSearchQuery by remember { mutableStateOf("") }
    var selectedBahan by remember { mutableStateOf<ItemWithStock?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var jumlahDiambilInput by remember { mutableStateOf("") }
    var selectedSatuan by remember { mutableStateOf("") }
    var namaPeminta by remember { mutableStateOf("") }
    var jabatan by remember { mutableStateOf("") }
    var kelas by remember { mutableStateOf("") }
    var namaPetugasInput by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }

    val defaultOfficerState by viewModel.defaultOfficer.collectAsState()
    LaunchedEffect(defaultOfficerState) {
        if (namaPetugasInput.isEmpty()) {
            namaPetugasInput = defaultOfficerState
        }
    }

    // Set default satuan if a material gets selected
    LaunchedEffect(selectedBahan) {
        if (selectedBahan != null) {
            selectedSatuan = selectedBahan!!.satuan
        }
    }

    // DatePicker State setup
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

    // Form Validations
    val stokTersedia = selectedBahan?.stokTersedia ?: 0
    val jumlahDiambil = jumlahDiambilInput.toIntOrNull() ?: 0
    val isJumlahInvalid = remember(jumlahDiambilInput, selectedBahan) {
        if (jumlahDiambilInput.isEmpty()) false
        else {
            jumlahDiambil <= 0 || (selectedBahan != null && jumlahDiambil > stokTersedia)
        }
    }

    val canSubmit = selectedBahan != null &&
            jumlahDiambil > 0 &&
            jumlahDiambil <= stokTersedia &&
            namaPeminta.isNotBlank() &&
            namaPetugasInput.isNotBlank() &&
            !isJumlahInvalid

    // Filtered materials suggestions for searchable dropdown
    val filteredBahan = remember(bahanSearchQuery, logistikItems) {
        if (bahanSearchQuery.isBlank()) {
            logistikItems
        } else {
            logistikItems.filter { it.namaBarang.contains(bahanSearchQuery, ignoreCase = true) }
        }
    }

    val selectedTabColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
    val unselectedTabColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.8f)

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
                                text = "Pemakaian Bahan Habis Pakai",
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
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = selectedTabColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = "Form Pemakaian",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) selectedTabColor else unselectedTabColor,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_form_pemakaian")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = "Riwayat Pemakaian",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) selectedTabColor else unselectedTabColor,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_riwayat_pemakaian")
                        )
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
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
        ) {
            if (selectedTab == 0) {
                // Form Pemakaian
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        LunarisCard(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = GlassWhiteMore),
                            border = BorderStroke(1.5.dp, PastelLavender.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Nama Bahan (Searchable Dropdown + QR Scan)
                                Text(
                                    text = "Nama Bahan",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
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
                                                selectedBahan = null // Reset selection if they edit/type
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
                                            modifier = Modifier.fillMaxWidth().testTag("pemakaian_search_input")
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

                                    // QR Scanner Button
                                    IconButton(
                                        onClick = { showQrScanner = true },
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(PastelLavender, RoundedCornerShape(12.dp))
                                            .testTag("btn_pemakaian_qr")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Pindai QR",
                                            tint = DeepPurpleText
                                        )
                                    }
                                }

                                // Real-time available stock label
                                if (selectedBahan != null) {
                                    Text(
                                        text = "Stok Tersedia: ${selectedBahan!!.stokTersedia} ${selectedBahan!!.satuan}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedBahan!!.stokTersedia > 0) Color(0xFF059669) else Color.Red,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                // 2-Kolom: Jumlah Diambil & Satuan (Otomatis)
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                    verticalAlignment = Alignment.Top
                                                                ) {
                                                                    // 2. Jumlah Diambil
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = "Jumlah Diambil",
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = DeepPurpleText,
                                                                            fontSize = 14.sp,
                                                                            modifier = Modifier.padding(bottom = 4.dp)
                                                                        )
                                                                        LunarisTextField(
                                                                            value = jumlahDiambilInput,
                                                                            onValueChange = { jumlahDiambilInput = it },
                                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                            placeholder = { Text("Contoh: 5") },
                                                                            singleLine = true,
                                                                            isError = isJumlahInvalid,
                                                                            supportingText = {
                                                                                if (isJumlahInvalid) {
                                                                                    Text(
                                                                                        "Jumlah harus > 0 dan tidak boleh melebihi stok tersedia ($stokTersedia)!",
                                                                                        color = MaterialTheme.colorScheme.error
                                                                                    )
                                                                                }
                                                                            },
                                                                            modifier = Modifier.fillMaxWidth().testTag("pemakaian_jumlah_input")
                                                                        )
                                                                    }
                                
                                                                    // 3. Satuan
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = "Satuan (Otomatis)",
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = DeepPurpleText,
                                                                            fontSize = 14.sp,
                                                                            modifier = Modifier.padding(bottom = 4.dp)
                                                                        )
                                                                        LunarisTextField(
                                                                            value = selectedSatuan.ifBlank { "Otomatis terisi..." },
                                                                            onValueChange = {},
                                                                            readOnly = true,
                                                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = DeepPurpleText
                                                                            ),
                                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                                focusedContainerColor = Color(0xFFF3F4F6),
                                                                                unfocusedContainerColor = Color(0xFFF3F4F6),
                                                                                disabledContainerColor = Color(0xFFF3F4F6),
                                                                                focusedBorderColor = PastelLavender,
                                                                                unfocusedBorderColor = PastelLavender.copy(alpha = 0.5f)
                                                                            ),
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .testTag("pemakaian_satuan")
                                                                        )
                                                                    }
                                                                }
                                
                                                                // 4. Nama Peminta
                                                                Text(
                                                                    text = "Nama Peminta",
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = DeepPurpleText,
                                                                    fontSize = 14.sp
                                                                )
                                                                LunarisTextField(
                                                                    value = namaPeminta,
                                                                    onValueChange = { namaPeminta = it },
                                                                    placeholder = { Text("Contoh: Ahmad Subarjo") },
                                                                    singleLine = true,
                                                                    modifier = Modifier.fillMaxWidth().testTag("pemakaian_peminta_input")
                                                                )
                                
                                                                // 5. Jabatan
                                                                Text(
                                                                    text = "Jabatan",
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = DeepPurpleText,
                                                                    fontSize = 14.sp
                                                                )
                                                                LunarisTextField(
                                                                    value = jabatan,
                                                                    onValueChange = { jabatan = it },
                                                                    placeholder = { Text("Contoh: Guru / Siswa / Wakasek") },
                                                                    singleLine = true,
                                                                    modifier = Modifier.fillMaxWidth().testTag("pemakaian_jabatan_input")
                                                                )
                                
                                                                // 2-Kolom: Tanggal Pemakaian & Kelas (Opsional)
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                    verticalAlignment = Alignment.Top
                                                                ) {
                                                                    // 8. Tanggal Pemakaian
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = "Tanggal Pemakaian",
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = DeepPurpleText,
                                                                            fontSize = 14.sp,
                                                                            modifier = Modifier.padding(bottom = 4.dp)
                                                                        )
                                                                        LunarisTextField(
                                                                            value = selectedDate,
                                                                            onValueChange = {},
                                                                            readOnly = true,
                                                                            trailingIcon = {
                                                                                IconButton(onClick = { datePickerDialog.show() }) {
                                                                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                                                                                }
                                                                            },
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .clickable { datePickerDialog.show() }
                                                                                .testTag("pemakaian_tanggal")
                                                                        )
                                                                    }
                                
                                                                    // 6. Kelas
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = "Kelas (Opsional)",
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = DeepPurpleText,
                                                                            fontSize = 14.sp,
                                                                            modifier = Modifier.padding(bottom = 4.dp)
                                                                        )
                                                                        LunarisTextField(
                                                                            value = kelas,
                                                                            onValueChange = { kelas = it },
                                                                            placeholder = { Text("Diisi jika siswa") },
                                                                            singleLine = true,
                                                                            modifier = Modifier.fillMaxWidth().testTag("pemakaian_kelas_input")
                                                                        )
                                                                    }
                                                                }
                                
                                                                // 7. Nama Petugas
                                                                Text(
                                                                    text = "Nama Petugas",
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = DeepPurpleText,
                                                                    fontSize = 14.sp
                                                                )
                                                                LunarisTextField(
                                                                    value = namaPetugasInput,
                                                                    onValueChange = { namaPetugasInput = it },
                                                                    placeholder = { Text("Nama petugas inventaris...") },
                                                                    singleLine = true,
                                                                    modifier = Modifier.fillMaxWidth().testTag("pemakaian_petugas_input")
                                                                )

                                // 9. Keterangan (Multiline)
                                Text(
                                    text = "Keterangan",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepPurpleText,
                                    fontSize = 14.sp
                                )
                                LunarisTextField(
                                    value = keterangan,
                                    onValueChange = { keterangan = it },
                                    placeholder = { Text("Keterangan keperluan pemakaian...") },
                                    minLines = 3,
                                    maxLines = 5,
                                    modifier = Modifier.fillMaxWidth().testTag("pemakaian_keterangan_input")
                                )
                            }
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (!canSubmit) return@Button
                                viewModel.recordPemakaian(
                                    idBarang = selectedBahan!!.idBarang,
                                    namaBarang = selectedBahan!!.namaBarang,
                                    jumlahDiambil = jumlahDiambil,
                                    satuan = selectedSatuan,
                                    namaPeminta = namaPeminta,
                                    jabatan = jabatan,
                                    kelas = kelas,
                                    namaPetugas = namaPetugasInput,
                                    tanggalPemakaian = selectedDate,
                                    keterangan = keterangan,
                                    onSuccess = {
                                        Toast.makeText(context, "Pemakaian bahan berhasil dicatat!", Toast.LENGTH_SHORT).show()
                                        // Reset Form
                                        jumlahDiambilInput = ""
                                        namaPeminta = ""
                                        jabatan = ""
                                        kelas = ""
                                        keterangan = ""
                                        selectedBahan = null
                                        bahanSearchQuery = ""
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = canSubmit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_simpan_pemakaian")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Pemakaian", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else {
                    // Riwayat Pemakaian
                    Column(modifier = Modifier.fillMaxSize()) {
                        LunarisTextField(
                            value = historySearchQuery,
                            onValueChange = { 
                                historySearchQuery = it
                            },
                            placeholder = { Text("Cari riwayat pemakaian...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                            trailingIcon = {
                                IconButton(onClick = { showHistoryQrScanner = true }) {
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
                                .fillMaxWidth()
                                .testTag("pemakaian_history_search_bar")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (showHistoryQrScanner) {
                            SearchQrScanDialog(
                                onDismiss = { showHistoryQrScanner = false },
                                onQrScanned = { scannedCode ->
                                    showHistoryQrScanner = false
                                    historySearchQuery = scannedCode
                                }
                            )
                        }

                        if (filteredHistory.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Kosong",
                                        tint = Color.Gray.copy(alpha = 0.5f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (historySearchQuery.isNotEmpty()) "Tidak ada riwayat yang cocok dengan pencarian." else "Belum ada riwayat pemakaian bahan.", 
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                items(filteredHistory) { record ->
                                    LunarisCard(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pemakaian_item_${record.idPemakaian}")
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = record.idPemakaian,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = DeepPurpleText,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = record.tanggalPemakaian,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = record.namaBarang,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Jumlah: ${record.jumlahDiambil} ${record.satuan}",
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Petugas: ${record.namaPetugas}",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Peminta: ${record.namaPeminta} (${record.jabatan}${if (!record.kelas.isNullOrEmpty()) ", Kelas " + record.kelas else ""})",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (record.keterangan.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Keterangan: ${record.keterangan}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
            PemakaianQrScanDialog(
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
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PemakaianQrScanDialog(
    onDismiss: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    val context = LocalContext.current

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
