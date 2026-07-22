@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class
)

package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.camera.core.CameraControl
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import android.Manifest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import com.example.ui.theme.DeepPurpleText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeminjamanScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialScannedId: String? = null
) {
    val context = LocalContext.current
    val itemsState by viewModel.itemsWithStock.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()
    val defaultOfficerState by viewModel.defaultOfficer.collectAsState()

    val canUseForm = if (userRole.contains("siswa", ignoreCase = true)) (studentPermissions["peminjaman_form"] == true) else true

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

    // 1. Core Form states
    var tanggalText by remember { mutableStateOf("") }
    var waktuText by remember { mutableStateOf("") }
    
    // Borrower Role/Category States
    var kategoriPeminjam by remember { mutableStateOf("Siswa") }
    var isKategoriExpanded by remember { mutableStateOf(false) }
    var isNamaPeminjamExpanded by remember { mutableStateOf(false) }

    var namaPeminjam by remember { mutableStateOf("") }
    var kelas by remember { mutableStateOf("") }
    var kondisiAwal by remember { mutableStateOf("Baik") }
    var namaPetugas by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }
    var durasiPeminjaman by remember { mutableStateOf("1") }

    var tujuanPeminjaman by remember { mutableStateOf("") }
    var isTujuanExpanded by remember { mutableStateOf(false) }
    var detailTujuan by remember { mutableStateOf("") }
    var isGuruExpanded by remember { mutableStateOf(false) }
    val listGuruMapelRaw by viewModel.guruMapel.collectAsState()
    val listGuruMapel = listGuruMapelRaw.map { rawItem ->
        val parts = rawItem.split("|:|")
        val nama = parts.getOrNull(0) ?: rawItem
        val mapel = if (parts.size >= 3) {
            parts.getOrNull(2) ?: ""
        } else {
            parts.getOrNull(1) ?: ""
        }
        if (mapel.isNotEmpty()) "$nama ($mapel)" else nama
    }

    val listStafRaw by viewModel.staf.collectAsState()
    val listStaf = listStafRaw.map { rawItem ->
        val parts = rawItem.split("|:|")
        val nama = parts.getOrNull(0) ?: rawItem
        val jabatan = if (parts.size >= 3) {
            parts.getOrNull(2) ?: ""
        } else {
            parts.getOrNull(1) ?: ""
        }
        if (jabatan.isNotEmpty()) "$nama ($jabatan)" else nama
    }

    // Multi-item lines states: List of (Item Name or ID, Qty)
    val borrowedLines = remember { mutableStateListOf<Pair<String, String>>() }

    fun consolidateBorrowedLines(): Boolean {
        var hasMerged = false
        val newList = mutableListOf<Pair<String, String>>()
        val keyIndexMap = HashMap<String, Int>()

        for (line in borrowedLines) {
            val rawItem = line.first.trim()
            val qtyVal = line.second.toIntOrNull() ?: 1

            if (rawItem.isBlank()) {
                val hasEmpty = newList.any { it.first.trim().isBlank() }
                if (!hasEmpty) {
                    newList.add(Pair("", line.second))
                } else {
                    hasMerged = true
                }
                continue
            }

            val matched = itemsState.find {
                it.idBarang.equals(rawItem, ignoreCase = true) || it.namaBarang.equals(rawItem, ignoreCase = true)
            }
            val canonicalKey = matched?.idBarang ?: rawItem.lowercase()
            val displayName = matched?.namaBarang ?: rawItem

            if (keyIndexMap.containsKey(canonicalKey)) {
                hasMerged = true
                val existingIdx = keyIndexMap[canonicalKey]!!
                val existingQty = newList[existingIdx].second.toIntOrNull() ?: 1
                val totalQty = existingQty + qtyVal
                newList[existingIdx] = Pair(newList[existingIdx].first.ifBlank { displayName }, totalQty.toString())
            } else {
                keyIndexMap[canonicalKey] = newList.size
                newList.add(Pair(displayName, qtyVal.toString()))
            }
        }

        if (hasMerged && newList.isNotEmpty()) {
            borrowedLines.clear()
            borrowedLines.addAll(newList)
        }
        return hasMerged
    }

    // Init defaults (Current WIB Time: GMT+7 / Asia/Jakarta)
    val calendarWib = remember {
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale("id", "ID"))
    }

    LaunchedEffect(Unit) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val sdfTime = SimpleDateFormat("HH:mm", Locale("id", "ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        tanggalText = sdfDate.format(calendarWib.time)
        waktuText = sdfTime.format(calendarWib.time)

        // Set default officer name from Settings
        if (namaPetugas.isBlank() && defaultOfficerState.isNotEmpty()) {
            namaPetugas = defaultOfficerState
        }

        // Add first empty item row
        if (borrowedLines.isEmpty()) {
            if (!initialScannedId.isNullOrEmpty()) {
                borrowedLines.add(Pair(initialScannedId, "1"))
            } else {
                borrowedLines.add(Pair("", "1"))
            }
        }
    }

    // DatePickerDialog Trigger
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
            tanggalText = sdf.format(selectedCal.time)
        },
        calendarWib.get(Calendar.YEAR),
        calendarWib.get(Calendar.MONTH),
        calendarWib.get(Calendar.DAY_OF_MONTH)
    )

    // TimePickerDialog Trigger
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            waktuText = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
        },
        calendarWib.get(Calendar.HOUR_OF_DAY),
        calendarWib.get(Calendar.MINUTE),
        true
    )

    // Selection helper modal for asset pickers per row
    var pickingLineIndex by remember { mutableStateOf<Int?>(null) }
    var scanningLineIndex by remember { mutableStateOf<Int?>(null) }
    var isKondisiExpanded by remember { mutableStateOf(false) }

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
                                text = "Formulir Peminjaman",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Data General Peminjam & Petugas
            LunarisCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Informasi Transaksi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Baris Tanggal & Waktu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tanggal Form Field
                        LunarisTextField(
                            value = tanggalText,
                            onValueChange = { tanggalText = it },
                            label = { Text("Tanggal Keluar (YYYY-MM-DD)") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { datePickerDialog.show() },
                                    modifier = Modifier.testTag("btn_select_date")
                                ) {
                                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pilih Tanggal")
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Waktu Form Field (WIB)
                        LunarisTextField(
                            value = waktuText,
                            onValueChange = { waktuText = it },
                            label = { Text("Waktu Keluar (WIB)") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { timePickerDialog.show() },
                                    modifier = Modifier.testTag("btn_select_time")
                                ) {
                                    Icon(imageVector = Icons.Default.Schedule, contentDescription = "Pilih Waktu")
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Kategori Peminjam Dropdown (Siswa, Guru, Staf)
                    ExposedDropdownMenuBox(
                        expanded = isKategoriExpanded,
                        onExpandedChange = { isKategoriExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LunarisTextField(
                            value = kategoriPeminjam,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Peminjam") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isKategoriExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("select_kategori_peminjam")
                        )
                        ExposedDropdownMenu(
                            expanded = isKategoriExpanded,
                            onDismissRequest = { isKategoriExpanded = false }
                        ) {
                            listOf("Siswa", "Guru", "Staf").forEach { kategori ->
                                DropdownMenuItem(
                                    text = { Text(kategori) },
                                    onClick = {
                                        if (kategoriPeminjam != kategori) {
                                            kategoriPeminjam = kategori
                                            namaPeminjam = ""
                                            kelas = ""
                                            detailTujuan = ""
                                        }
                                        isKategoriExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Nama Peminjam Form Field (Manual for Siswa, Dropdown for Guru / Staf)
                    if (kategoriPeminjam == "Siswa") {
                        LunarisTextField(
                            value = namaPeminjam,
                            onValueChange = { namaPeminjam = it },
                            label = { Text("Nama Peminjam (Siswa)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_nama_peminjam")
                        )
                    } else if (kategoriPeminjam == "Guru") {
                        ExposedDropdownMenuBox(
                            expanded = isNamaPeminjamExpanded,
                            onExpandedChange = { isNamaPeminjamExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LunarisTextField(
                                value = namaPeminjam,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Nama Peminjam (Guru)") },
                                placeholder = { Text("Pilih Guru...") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNamaPeminjamExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("select_nama_peminjam_guru")
                            )
                            ExposedDropdownMenu(
                                expanded = isNamaPeminjamExpanded,
                                onDismissRequest = { isNamaPeminjamExpanded = false }
                            ) {
                                if (listGuruMapelRaw.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Belum ada data Guru Mapel di Master Data") },
                                        onClick = { isNamaPeminjamExpanded = false }
                                    )
                                } else {
                                    listGuruMapelRaw.forEach { rawItem ->
                                        val parts = rawItem.split("|:|")
                                        val nama = parts.getOrNull(0) ?: rawItem
                                        val mapel = if (parts.size >= 3) parts.getOrNull(2) ?: "" else parts.getOrNull(1) ?: ""
                                        val displayText = if (mapel.isNotEmpty()) "$nama ($mapel)" else nama
                                        
                                        DropdownMenuItem(
                                            text = { Text(displayText) },
                                            onClick = {
                                                namaPeminjam = nama
                                                isNamaPeminjamExpanded = false
                                                if (tujuanPeminjaman == "Kegiatan Belajar Mengajar (KBM)") {
                                                    detailTujuan = displayText
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (kategoriPeminjam == "Staf") {
                        ExposedDropdownMenuBox(
                            expanded = isNamaPeminjamExpanded,
                            onExpandedChange = { isNamaPeminjamExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LunarisTextField(
                                value = namaPeminjam,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Nama Peminjam (Staf)") },
                                placeholder = { Text("Pilih Staf...") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNamaPeminjamExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("select_nama_peminjam_staf")
                            )
                            ExposedDropdownMenu(
                                expanded = isNamaPeminjamExpanded,
                                onDismissRequest = { isNamaPeminjamExpanded = false }
                            ) {
                                if (listStafRaw.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Belum ada data Staf di Master Data") },
                                        onClick = { isNamaPeminjamExpanded = false }
                                    )
                                } else {
                                    listStafRaw.forEach { rawItem ->
                                        val parts = rawItem.split("|:|")
                                        val nama = parts.getOrNull(0) ?: rawItem
                                        val jabatan = if (parts.size >= 3) parts.getOrNull(2) ?: "" else parts.getOrNull(1) ?: ""
                                        val displayText = if (jabatan.isNotEmpty()) "$nama ($jabatan)" else nama
                                        
                                        DropdownMenuItem(
                                            text = { Text(displayText) },
                                            onClick = {
                                                namaPeminjam = nama
                                                isNamaPeminjamExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Kelas Form Field with dynamic label
                    val kelasLabel = when (kategoriPeminjam) {
                        "Siswa" -> "Kelas Peminjam (Wajib)"
                        "Guru" -> "Kelas yang Diajar (Opsional)"
                        "Staf" -> "Unit Kerja / Jabatan (Opsional)"
                        else -> "Kelas Peminjam"
                    }

                    LunarisTextField(
                        value = kelas,
                        onValueChange = { kelas = it },
                        label = { Text(kelasLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_kelas")
                    )

                    // Tujuan Peminjaman Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isTujuanExpanded,
                        onExpandedChange = { isTujuanExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LunarisTextField(
                            value = tujuanPeminjaman,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tujuan Peminjaman") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTujuanExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("select_tujuan_peminjaman")
                        )
                        ExposedDropdownMenu(
                            expanded = isTujuanExpanded,
                            onDismissRequest = { isTujuanExpanded = false }
                        ) {
                            val daftarTujuan = listOf(
                                "Kegiatan Belajar Mengajar (KBM)",
                                "Pelatihan, Workshop, & Pengembangan Staf/Guru",
                                "Kegiatan Ekstrakurikuler & Keorganisasian Siswa",
                                "Acara Formal & Seremonial",
                                "Ujian & Evaluasi",
                                "Lainnya"
                            )
                            daftarTujuan.forEach { opsi ->
                                DropdownMenuItem(
                                    text = { Text(opsi) },
                                    onClick = {
                                        if (tujuanPeminjaman != opsi) {
                                            tujuanPeminjaman = opsi
                                            if (opsi == "Kegiatan Belajar Mengajar (KBM)" && kategoriPeminjam == "Guru") {
                                                val matchedGuru = listGuruMapelRaw.firstOrNull { it.split("|:|").firstOrNull() == namaPeminjam }
                                                val mapel = if (matchedGuru != null) {
                                                    val parts = matchedGuru.split("|:|")
                                                    if (parts.size >= 3) parts.getOrNull(2) ?: "" else parts.getOrNull(1) ?: ""
                                                } else ""
                                                detailTujuan = if (mapel.isNotEmpty()) "$namaPeminjam ($mapel)" else namaPeminjam
                                            } else {
                                                detailTujuan = "" // reset detail when main tujuan changes
                                            }
                                        }
                                        isTujuanExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Logika Kondisional Form (Dinamis)
                    if (tujuanPeminjaman == "Kegiatan Belajar Mengajar (KBM)") {
                        if (kategoriPeminjam != "Guru") {
                            ExposedDropdownMenuBox(
                                expanded = isGuruExpanded,
                                onExpandedChange = { isGuruExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LunarisTextField(
                                    value = detailTujuan,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Guru Pengampu / Mapel (Opsional)") },
                                    placeholder = { Text("Pilih Guru Mapel...") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGuruExpanded) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                        unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("select_guru_pengampu")
                                )
                                ExposedDropdownMenu(
                                    expanded = isGuruExpanded,
                                    onDismissRequest = { isGuruExpanded = false }
                                ) {
                                    if (listGuruMapel.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Belum ada data Guru Mapel di Master Data") },
                                            onClick = { isGuruExpanded = false }
                                        )
                                    } else {
                                        listGuruMapel.forEach { guru ->
                                            DropdownMenuItem(
                                                text = { Text(guru) },
                                                onClick = {
                                                    detailTujuan = guru
                                                    isGuruExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (tujuanPeminjaman.isNotEmpty()) {
                        LunarisTextField(
                            value = detailTujuan,
                            onValueChange = { detailTujuan = it },
                            label = { Text("Keterangan / Detail Kegiatan (Opsional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_detail_kegiatan")
                        )
                    }

                    // WhatsApp Peminjam Form Field (Optional)
                    LunarisTextField(
                        value = whatsappNumber,
                        onValueChange = { whatsappNumber = it },
                        label = { Text("Nomor WhatsApp Peminjam (Opsional)") },
                        placeholder = { Text("Contoh: 08123456789 atau 628123456789") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_whatsapp_number")
                    )

                    // Baris Durasi & Kondisi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Durasi Peminjaman Manual Form Field
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LunarisTextField(
                                value = durasiPeminjaman,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.all { it.isDigit() }) {
                                        durasiPeminjaman = input
                                    }
                                },
                                label = { Text("Durasi Peminjaman") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_durasi_peminjaman")
                            )
                            Text(
                                text = "Hari",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else DeepPurpleText,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        // Kondisi Dropdown Form Field
                        ExposedDropdownMenuBox(
                            expanded = isKondisiExpanded,
                            onExpandedChange = { isKondisiExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            LunarisTextField(
                                value = kondisiAwal,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kondisi Awal Barang") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isKondisiExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                    unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                    focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("select_kondisi")
                            )
                            ExposedDropdownMenu(
                                expanded = isKondisiExpanded,
                                onDismissRequest = { isKondisiExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Baik") },
                                    onClick = {
                                        kondisiAwal = "Baik"
                                        isKondisiExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rusak") },
                                    onClick = {
                                        kondisiAwal = "Rusak"
                                        isKondisiExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Nama Petugas Form Field
                    LunarisTextField(
                        value = namaPetugas,
                        onValueChange = { namaPetugas = it },
                        label = { Text("Nama Petugas Penanggung Jawab") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_petugas")
                    )
                }
            }

            // Section 2: Daftar Multi-Barang ("+ Lainnya")
            LunarisCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Barang yang Dipinjam",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // List each line
                    borrowedLines.forEachIndexed { index, line ->
                        if (index > 0) {
                            androidx.compose.material3.HorizontalDivider(
                                thickness = 0.5.dp,
                                color = dividerColor,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Barang #${index + 1}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                // Delete button (only show if there's more than 1 row)
                                if (borrowedLines.size > 1) {
                                    IconButton(
                                        onClick = { borrowedLines.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Nama Barang Input with "Search assets" quick-picker button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LunarisTextField(
                                    value = line.first,
                                    onValueChange = { newVal ->
                                        borrowedLines[index] = Pair(newVal, line.second)
                                    },
                                    label = { Text("Nama / ID Barang") },
                                    placeholder = { Text("Ketik manual atau pilih...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                        unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { focusState ->
                                            if (!focusState.isFocused) {
                                                consolidateBorrowedLines()
                                            }
                                        }
                                        .testTag("item_input_$index")
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Asset selector button
                                IconButton(
                                    onClick = { pickingLineIndex = index },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .testTag("btn_pick_asset_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Pilih Aset Gudang",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // QR Scanner button
                                IconButton(
                                    onClick = { scanningLineIndex = index },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .testTag("btn_scan_qr_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "Pindai QR Code",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // Qty Input and Satuan Info
                            val matchedItem = itemsState.find {
                                it.idBarang == line.first || it.namaBarang.equals(line.first, ignoreCase = true)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                LunarisTextField(
                                    value = line.second,
                                    onValueChange = { newVal ->
                                        borrowedLines[index] = Pair(line.first, newVal)
                                    },
                                    label = { Text("Jumlah Dipinjam") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText,
                                        unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFD8B4FE),
                                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("item_qty_$index")
                                )

                                if (matchedItem != null) {
                                    Column(
                                        modifier = Modifier.padding(top = 8.dp, start = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Satuan",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = matchedItem.satuan,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.primary else DeepPurpleText
                                        )
                                    }
                                }
                            }

                            // Display current local stock helper if matched
                            if (matchedItem != null) {
                                Text(
                                    text = "Tersedia di Gudang: ${matchedItem.stokTersedia} ${matchedItem.satuan} (Fisik: ${matchedItem.stokAwal})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (matchedItem.stokTersedia > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    // Button + Lainnya (Add row)
                    Button(
                        onClick = {
                            val wasMerged = consolidateBorrowedLines()
                            if (wasMerged) {
                                Toast.makeText(context, "Item dengan ID/nama sama otomatis digabungkan!", Toast.LENGTH_SHORT).show()
                            }
                            val hasEmpty = borrowedLines.any { it.first.trim().isBlank() }
                            if (!hasEmpty) {
                                borrowedLines.add(Pair("", "1"))
                            } else {
                                Toast.makeText(context, "Lengkapi nama/ID barang yang kosong terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_tambah_lainnya")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Barang Lainnya (+ Lainnya)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display prominent stock info above the action button
            if (borrowedLines.isNotEmpty()) {
                val stockListText = remember(borrowedLines.toList(), itemsState) {
                    val mergedMap = LinkedHashMap<String, Int>()
                    val originalNameMap = HashMap<String, String>()

                    for (line in borrowedLines) {
                        val rawItem = line.first.trim()
                        val qtyVal = line.second.toIntOrNull() ?: 1
                        if (rawItem.isBlank()) continue

                        val matched = itemsState.find {
                            it.idBarang.equals(rawItem, ignoreCase = true) || it.namaBarang.equals(rawItem, ignoreCase = true)
                        }
                        val canonicalKey = matched?.idBarang ?: rawItem.lowercase()
                        val displayName = matched?.namaBarang ?: rawItem
                        originalNameMap[canonicalKey] = displayName
                        mergedMap[canonicalKey] = (mergedMap[canonicalKey] ?: 0) + qtyVal
                    }

                    mergedMap.mapNotNull { (key, totalQty) ->
                        val displayName = originalNameMap[key] ?: key
                        val matched = itemsState.find {
                            it.idBarang.equals(key, ignoreCase = true) || it.namaBarang.equals(displayName, ignoreCase = true)
                        }
                        if (matched != null) {
                            val statusNote = if (totalQty > matched.stokTersedia) " ⚠️ (Stok Kurang!)" else ""
                            "${matched.namaBarang}: $totalQty unit diminta (Tersedia: ${matched.stokTersedia} ${matched.satuan})$statusNote"
                        } else null
                    }
                }

                if (stockListText.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "INFORMASI STOK BARANG",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B21A8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        stockListText.forEach { txt ->
                            Text(
                                text = txt,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (txt.contains("Stok Kurang")) Color(0xFFB91C1C) else Color(0xFF1E1B4B)
                            )
                        }
                    }
                }
            }

            // Main Save Button
            val isOutOfStock = remember(borrowedLines.toList(), itemsState) {
                val mergedMap = LinkedHashMap<String, Int>()
                for (line in borrowedLines) {
                    val rawItem = line.first.trim()
                    val qtyVal = line.second.toIntOrNull() ?: 1
                    if (rawItem.isBlank()) continue

                    val matched = itemsState.find {
                        it.idBarang.equals(rawItem, ignoreCase = true) || it.namaBarang.equals(rawItem, ignoreCase = true)
                    }
                    val canonicalKey = matched?.idBarang ?: rawItem.lowercase()
                    mergedMap[canonicalKey] = (mergedMap[canonicalKey] ?: 0) + qtyVal
                }

                if (mergedMap.isEmpty()) false
                else mergedMap.any { (key, totalQty) ->
                    val matched = itemsState.find {
                        it.idBarang.equals(key, ignoreCase = true) || it.namaBarang.equals(key, ignoreCase = true)
                    }
                    matched != null && (matched.stokTersedia <= 0 || totalQty > matched.stokTersedia)
                }
            }

            val buttonEnabled = !isOutOfStock && canUseForm
            val buttonText = if (!canUseForm) {
                "Akses Form Terkunci Total"
            } else if (userRole == "siswa") {
                if (isOutOfStock) "Stok Habis / Tidak Cukup" else "Ajukan Peminjaman"
            } else {
                if (isOutOfStock) "Stok Habis / Tidak Cukup" else "Simpan Transaksi Peminjaman"
            }

            Button(
                onClick = {
                    consolidateBorrowedLines()

                    if (namaPeminjam.trim().isBlank()) {
                        val roleLabel = when (kategoriPeminjam) {
                            "Guru" -> "Guru"
                            "Staf" -> "Staf"
                            else -> "Siswa"
                        }
                        Toast.makeText(context, "Nama Peminjam ($roleLabel) harus diisi atau dipilih!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (kategoriPeminjam == "Siswa" && kelas.trim().isBlank()) {
                        Toast.makeText(context, "Kelas Peminjam wajib diisi untuk kategori Siswa!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Assemble lines and map to integer, auto-merging duplicate rows
                    val mergedMap = LinkedHashMap<String, Int>()
                    val originalNameMap = HashMap<String, String>()

                    for (line in borrowedLines) {
                        val rawItem = line.first.trim()
                        val qtyVal = line.second.toIntOrNull()
                        if (rawItem.isBlank()) {
                            Toast.makeText(context, "Nama barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (qtyVal == null || qtyVal <= 0) {
                            Toast.makeText(context, "Jumlah barang harus lebih besar dari 0!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val matched = itemsState.find { it.idBarang.equals(rawItem, ignoreCase = true) || it.namaBarang.equals(rawItem, ignoreCase = true) }
                        val canonicalKey = matched?.idBarang ?: rawItem.lowercase()
                        val displayName = matched?.namaBarang ?: rawItem
                        originalNameMap[canonicalKey] = displayName
                        mergedMap[canonicalKey] = (mergedMap[canonicalKey] ?: 0) + qtyVal
                    }

                    // Pre-validate stock for merged items
                    for ((key, totalQty) in mergedMap) {
                        val displayName = originalNameMap[key] ?: key
                        val matched = itemsState.find { it.idBarang.equals(key, ignoreCase = true) || it.namaBarang.equals(displayName, ignoreCase = true) }
                        if (matched != null) {
                            if (totalQty > matched.stokTersedia) {
                                Toast.makeText(
                                    context,
                                    "Stok '${matched.namaBarang}' tidak mencukupi! Tersedia: ${matched.stokTersedia} ${matched.satuan}, total diminta: $totalQty",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }
                        }
                    }

                    val linesToSubmit = mergedMap.map { (key, totalQty) ->
                        Pair(originalNameMap[key] ?: key, totalQty)
                    }

                    val durasiVal = durasiPeminjaman.trim().toIntOrNull() ?: 1
                    if (durasiVal <= 0) {
                        Toast.makeText(context, "Durasi peminjaman minimal 1 hari!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.submitLoan(
                        namaPeminjam = namaPeminjam,
                        kelas = kelas,
                        kondisi = kondisiAwal,
                        namaPetugas = namaPetugas,
                        tanggal = tanggalText,
                        waktu = waktuText,
                        whatsappNumber = if (whatsappNumber.isBlank()) null else whatsappNumber,
                        itemsToBorrow = linesToSubmit,
                        durasiHari = durasiVal,
                        tujuanPeminjaman = if (tujuanPeminjaman.isBlank()) null else tujuanPeminjaman,
                        detailTujuan = if (detailTujuan.isBlank()) null else detailTujuan,
                        onSuccess = {
                            Toast.makeText(context, "Peminjaman berhasil disimpan!", Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = buttonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOutOfStock) Color.Gray else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_simpan_peminjaman")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Simpan")
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }

        // Search Asset Picker Modal Dialog
        if (pickingLineIndex != null) {
            val lineIdx = pickingLineIndex!!
            var assetSearchText by remember { mutableStateOf("") }
            var showDialogQrScanner by remember { mutableStateOf(false) }
            val pickingFilteredItems = remember(itemsState, assetSearchText) {
                if (assetSearchText.isBlank()) itemsState
                else itemsState.filter {
                    it.namaBarang.contains(assetSearchText, ignoreCase = true) ||
                            it.idBarang.contains(assetSearchText, ignoreCase = true)
                }
            }

            AlertDialog(
                shape = RoundedCornerShape(16.dp),
                onDismissRequest = { pickingLineIndex = null },
                title = { Text("Pilih Barang Gudang", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(300.dp)
                    ) {
                        LunarisTextField(
                            value = assetSearchText,
                            onValueChange = { assetSearchText = it },
                            placeholder = { Text("Cari barang...") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Cari") },
                            trailingIcon = {
                                IconButton(onClick = { showDialogQrScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan QR",
                                        tint = MaterialTheme.colorScheme.primary
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
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showDialogQrScanner) {
                            SearchQrScanDialog(
                                onDismiss = { showDialogQrScanner = false },
                                onQrScanned = { scannedCode ->
                                    showDialogQrScanner = false
                                    assetSearchText = scannedCode
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (pickingFilteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada barang.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                pickingFilteredItems.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val selectedName = item.namaBarang
                                                val selectedId = item.idBarang
                                                val existingIndex = borrowedLines.indexOfFirst { (name, _) ->
                                                    val clean = name.trim()
                                                    if (clean.isEmpty()) return@indexOfFirst false
                                                    val matched = itemsState.find { it.idBarang.equals(clean, ignoreCase = true) || it.namaBarang.equals(clean, ignoreCase = true) }
                                                    (matched != null && (matched.idBarang == selectedId || matched.namaBarang.equals(selectedName, ignoreCase = true))) ||
                                                            clean.equals(selectedName, ignoreCase = true) ||
                                                            clean.equals(selectedId, ignoreCase = true)
                                                }
                                                if (existingIndex != -1 && existingIndex != lineIdx) {
                                                    val currentQty = borrowedLines[lineIdx].second.toIntOrNull() ?: 1
                                                    val existingQty = borrowedLines[existingIndex].second.toIntOrNull() ?: 1
                                                    val mergedQty = currentQty + existingQty
                                                    borrowedLines[existingIndex] = Pair(borrowedLines[existingIndex].first.ifBlank { selectedName }, mergedQty.toString())
                                                    if (borrowedLines.size > 1) {
                                                        borrowedLines.removeAt(lineIdx)
                                                    }
                                                    Toast.makeText(context, "Item '$selectedName' ($selectedId) sudah ada di baris #${existingIndex + 1}. Kuantitas digabungkan menjadi $mergedQty unit!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    borrowedLines[lineIdx] = Pair(selectedName, borrowedLines[lineIdx].second)
                                                }
                                                pickingLineIndex = null
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp)
                                            .clip(MaterialTheme.shapes.small),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.namaBarang,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "ID: ${item.idBarang} • Tersedia: ${item.stokTersedia}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (item.stokTersedia > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                                    MaterialTheme.shapes.small
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${item.stokTersedia}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.stokTersedia > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { pickingLineIndex = null }) {
                        Text("Tutup")
                    }
                }
            )
        }

        if (scanningLineIndex != null) {
            val currentIndex = scanningLineIndex!!
            PeminjamanQrScanDialog(
                onDismiss = { scanningLineIndex = null },
                onQrScanned = { scannedCode ->
                    val matchedItem = itemsState.find { it.idBarang == scannedCode || it.namaBarang.equals(scannedCode, ignoreCase = true) }
                    val selectedName = matchedItem?.namaBarang ?: scannedCode
                    val selectedId = matchedItem?.idBarang ?: scannedCode

                    val existingIndex = borrowedLines.indexOfFirst { (name, _) ->
                        val clean = name.trim()
                        if (clean.isEmpty()) return@indexOfFirst false
                        val matched = itemsState.find { it.idBarang.equals(clean, ignoreCase = true) || it.namaBarang.equals(clean, ignoreCase = true) }
                        (matched != null && (matched.idBarang == selectedId || matched.namaBarang.equals(selectedName, ignoreCase = true))) ||
                                clean.equals(selectedName, ignoreCase = true) ||
                                clean.equals(selectedId, ignoreCase = true)
                    }

                    if (existingIndex != -1 && existingIndex != currentIndex) {
                        val currentQty = borrowedLines[currentIndex].second.toIntOrNull() ?: 1
                        val existingQty = borrowedLines[existingIndex].second.toIntOrNull() ?: 1
                        val mergedQty = currentQty + existingQty
                        borrowedLines[existingIndex] = Pair(borrowedLines[existingIndex].first.ifBlank { selectedName }, mergedQty.toString())
                        if (borrowedLines.size > 1) {
                            borrowedLines.removeAt(currentIndex)
                        }
                        Toast.makeText(context, "Item '$selectedName' ($selectedId) sudah ada di baris #${existingIndex + 1}. Kuantitas digabungkan menjadi $mergedQty unit!", Toast.LENGTH_LONG).show()
                    } else {
                        borrowedLines[currentIndex] = Pair(selectedName, borrowedLines[currentIndex].second)
                        Toast.makeText(context, "Terdeteksi via QR: $selectedName", Toast.LENGTH_SHORT).show()
                    }
                    scanningLineIndex = null
                }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PeminjamanQrScanDialog(
    onDismiss: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
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
                Text("Pindai QR Code", fontWeight = FontWeight.Bold)
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
