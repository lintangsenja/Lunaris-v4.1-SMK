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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPurpleText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.example.ui.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun consolidateLoanItems(items: List<LoanItemEntity>): List<LoanItemEntity> {
    val map = LinkedHashMap<String, LoanItemEntity>()
    for (item in items) {
        val key = if (item.idBarang.isNotBlank()) item.idBarang else item.namaBarang.trim().lowercase()
        if (map.containsKey(key)) {
            val existing = map[key]!!
            map[key] = existing.copy(jumlah = existing.jumlah + item.jumlah)
        } else {
            map[key] = item
        }
    }
    return map.values.toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengembalianScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialScannedId: String? = null
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

    val coroutineScope = rememberCoroutineScope()

    val activeTxState by viewModel.activeTransactions.collectAsState()
    val defaultOfficerState by viewModel.defaultOfficer.collectAsState()
    val listGuruMapelRaw by viewModel.guruMapel.collectAsState()
    val listStafRaw by viewModel.staf.collectAsState()

    val listGuruNames = remember(listGuruMapelRaw) {
        listGuruMapelRaw.map { it.split("|:|").first().trim() }
    }
    val listStafNames = remember(listStafRaw) {
        listStafRaw.map { it.split("|:|").first().trim() }
    }

    // 1. Form States
    var tanggalText by remember { mutableStateOf("") }
    var waktuText by remember { mutableStateOf("") }
    var selectedBorrowerName by remember { mutableStateOf("") }
    var kelasAutofill by remember { mutableStateOf("") }
    var selectedTransaction by remember { mutableStateOf<LoanTransactionEntity?>(null) }
    var borrowedItemsList = remember { mutableStateOf<List<LoanItemEntity>>(emptyList()) }
    var kondisiKembali by remember { mutableStateOf("Normal") }
    var namaPetugasKembali by remember { mutableStateOf("") }

    var itemConditionsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var itemDamagedCountsMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var itemAllDamagedMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var itemNotesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val kategoriPeminjam = remember(selectedTransaction, listGuruNames, listStafNames) {
        if (selectedTransaction == null) ""
        else {
            val name = selectedTransaction?.namaPeminjam ?: ""
            when {
                listGuruNames.contains(name) -> "Guru"
                listStafNames.contains(name) -> "Staf"
                else -> "Siswa"
            }
        }
    }

    // QR scanner & explanation states
    var scannedFilterIdBarang by remember { mutableStateOf<String?>(null) }
    var showQrScanner by remember { mutableStateOf(false) }
    var activeTxItemsMap by remember { mutableStateOf<Map<String, List<LoanItemEntity>>>(emptyMap()) }
    var keteranganKerusakan by remember { mutableStateOf("") }

    // Dropdowns
    var isBorrowerExpanded by remember { mutableStateOf(false) }
    var isKondisiExpanded by remember { mutableStateOf(false) }

    val calendarWib = remember {
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"), Locale("id", "ID"))
    }

    LaunchedEffect(activeTxState) {
        val map = mutableMapOf<String, List<LoanItemEntity>>()
        activeTxState.forEach { tx ->
            map[tx.idTransaksi] = consolidateLoanItems(viewModel.getItemsForTransaction(tx.idTransaksi))
        }
        activeTxItemsMap = map
    }

    LaunchedEffect(borrowedItemsList.value) {
        val consolidated = consolidateLoanItems(borrowedItemsList.value)
        val initialConditions = consolidated.associate { it.idBarang to (itemConditionsMap[it.idBarang] ?: "Rusak Ringan") }
        itemConditionsMap = initialConditions
        val initialNotes = consolidated.associate { it.idBarang to (itemNotesMap[it.idBarang] ?: "") }
        itemNotesMap = initialNotes
        val initialDamagedCounts = consolidated.associate { it.idBarang to (itemDamagedCountsMap[it.idBarang] ?: 0) }
        itemDamagedCountsMap = initialDamagedCounts
        val initialAllDamaged = consolidated.associate { it.idBarang to (itemAllDamagedMap[it.idBarang] ?: false) }
        itemAllDamagedMap = initialAllDamaged
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

        if (namaPetugasKembali.isBlank() && defaultOfficerState.isNotEmpty()) {
            namaPetugasKembali = defaultOfficerState
        }
    }

    LaunchedEffect(initialScannedId, activeTxState) {
        if (!initialScannedId.isNullOrEmpty() && activeTxState.isNotEmpty()) {
            for (tx in activeTxState) {
                val itemsInTx = viewModel.getItemsForTransaction(tx.idTransaksi)
                if (itemsInTx.any { it.idBarang == initialScannedId }) {
                    selectedTransaction = tx
                    selectedBorrowerName = tx.namaPeminjam
                    kelasAutofill = tx.kelas
                    borrowedItemsList.value = itemsInTx
                    break
                }
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

    // Selection helper for active transactions dialog (if borrower has multiple active borrow records)
    var showActiveTxSelectionDialog by remember { mutableStateOf(false) }

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
                                text = "Formulir Pengembalian",
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
            // General Details
            LunarisCard(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Informasi Pengembalian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Row 1: Tanggal & Waktu Kembali
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Tanggal Form Field
                                            Column(modifier = Modifier.weight(1f)) {
                                                LunarisTextField(
                                                    value = tanggalText,
                                                    onValueChange = { tanggalText = it },
                                                    label = { Text("Tanggal Kembali (YYYY-MM-DD)") },
                                                    trailingIcon = {
                                                        IconButton(
                                                            onClick = { datePickerDialog.show() },
                                                            modifier = Modifier.testTag("btn_select_date")
                                                        ) {
                                                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pilih Tanggal")
                                                        }
                                                    },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                    
                                            // Waktu Form Field (WIB)
                                            Column(modifier = Modifier.weight(1f)) {
                                                LunarisTextField(
                                                    value = waktuText,
                                                    onValueChange = { waktuText = it },
                                                    label = { Text("Waktu Kembali (WIB)") },
                                                    trailingIcon = {
                                                        IconButton(
                                                            onClick = { timePickerDialog.show() },
                                                            modifier = Modifier.testTag("btn_select_time")
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Schedule, contentDescription = "Pilih Waktu")
                                                        }
                                                    },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                    // QR filter indicator badge
                    if (scannedFilterIdBarang != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Filter aktif",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Menyaring peminjam barang: $scannedFilterIdBarang",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = { scannedFilterIdBarang = null },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Bersihkan", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Nama Peminjam Dropdown with Scan QR button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = isBorrowerExpanded,
                                onExpandedChange = { isBorrowerExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LunarisTextField(
                                    value = selectedBorrowerName,
                                    onValueChange = { selectedBorrowerName = it },
                                    label = { Text("Pilih Nama Peminjam Aktif") },
                                    placeholder = { Text("Ketik nama atau pilih...") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBorrowerExpanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                                        .testTag("select_borrower")
                                )

                                val filteredActiveBorrowers = activeTxState
                                    .filter { tx ->
                                        val matchesSearch = tx.namaPeminjam.contains(selectedBorrowerName, ignoreCase = true)
                                        val matchesScanned = scannedFilterIdBarang == null || activeTxItemsMap[tx.idTransaksi]?.any {
                                            it.idBarang == scannedFilterIdBarang || it.namaBarang.equals(scannedFilterIdBarang, ignoreCase = true)
                                        } == true
                                        matchesSearch && matchesScanned
                                    }
                                    .distinctBy { it.namaPeminjam }

                                if (filteredActiveBorrowers.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = isBorrowerExpanded,
                                        onDismissRequest = { isBorrowerExpanded = false }
                                    ) {
                                        filteredActiveBorrowers.forEach { tx ->
                                            DropdownMenuItem(
                                                text = { Text(tx.namaPeminjam + " (${tx.kelas})") },
                                                onClick = {
                                                    selectedBorrowerName = tx.namaPeminjam
                                                    isBorrowerExpanded = false

                                                    // Filter transactions for this borrower
                                                    val borrowerTxs = activeTxState.filter { txItem ->
                                                        val matchesBorrower = txItem.namaPeminjam == tx.namaPeminjam
                                                        val matchesScanned = scannedFilterIdBarang == null || activeTxItemsMap[txItem.idTransaksi]?.any {
                                                            it.idBarang == scannedFilterIdBarang || it.namaBarang.equals(scannedFilterIdBarang, ignoreCase = true)
                                                        } == true
                                                        matchesBorrower && matchesScanned
                                                    }
                                                    if (borrowerTxs.size > 1) {
                                                        // Prompt dialog to choose which transaction
                                                        showActiveTxSelectionDialog = true
                                                    } else if (borrowerTxs.size == 1) {
                                                        val singleTx = borrowerTxs.first()
                                                        selectedTransaction = singleTx
                                                        kelasAutofill = singleTx.kelas
                                                        coroutineScope.launch {
                                                            borrowedItemsList.value = viewModel.getItemsForTransaction(singleTx.idTransaksi)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Return QR Scanner button
                        IconButton(
                            onClick = { showQrScanner = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    MaterialTheme.shapes.small
                                )
                                .testTag("btn_scan_qr_return")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Pindai QR Barang",
                                tint = MaterialTheme.colorScheme.secondary
                             )
                        }
                    }

                    if (selectedTransaction != null) {
                        // Kategori Peminjam (Siswa / Guru / Staf)
                        LunarisTextField(
                            value = kategoriPeminjam,
                            onValueChange = {},
                            label = { Text("Kategori Peminjam") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    MaterialTheme.shapes.small
                                )
                                .testTag("autofill_kategori_peminjam")
                        )

                        // Kelas or Unit Kerja / Jabatan dynamic label
                        val labelText = when (kategoriPeminjam) {
                            "Staf" -> "Unit Kerja / Jabatan"
                            else -> "Kelas"
                        }
                        LunarisTextField(
                            value = kelasAutofill,
                            onValueChange = {},
                            label = { Text("$labelText (Auto-Fill)") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    MaterialTheme.shapes.small
                                )
                                .testTag("autofill_kelas")
                        )

                        // Tujuan Peminjaman and Detail Kegiatan info card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.shapes.medium
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Konteks Penggunaan Alat",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tujuan Peminjaman: ${selectedTransaction?.tujuanPeminjaman ?: "-"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Guru Pengampu / Detail Kegiatan: ${selectedTransaction?.detailTujuan ?: "-"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        // Fallback/Placeholder if no transaction selected yet
                        LunarisTextField(
                            value = kelasAutofill,
                            onValueChange = {},
                            label = { Text("Kelas (Auto-Fill)") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    MaterialTheme.shapes.small
                                )
                                .testTag("autofill_kelas")
                        )
                    }
                }
            }

            // Autofilled List of Borrowed Items Section
            if (selectedTransaction != null) {
                LunarisCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Daftar Barang Dipinjam (Auto-Fill)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Kode Transaksi: ${selectedTransaction?.idTransaksi}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Tanggal Pinjam: ${selectedTransaction?.tanggal} pukul ${selectedTransaction?.waktu} WIB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val consolidatedList = remember(borrowedItemsList.value) { consolidateLoanItems(borrowedItemsList.value) }
                        consolidatedList.forEach { item ->
                            val totalQty = item.jumlah
                            val isAllDamaged = itemAllDamagedMap[item.idBarang] ?: false
                            val rawDamaged = itemDamagedCountsMap[item.idBarang] ?: 0
                            val damagedQty = if (isAllDamaged) totalQty else rawDamaged.coerceIn(0, totalQty)
                            val goodQty = (totalQty - damagedQty).coerceAtLeast(0)
                            val currentCondition = itemConditionsMap[item.idBarang] ?: "Rusak Ringan"
                            val currentNote = itemNotesMap[item.idBarang] ?: ""

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                        MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Item Header & Summary Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            text = "ID: ${item.idBarang}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    val (badgeBg, badgeText, badgeColor) = when {
                                        damagedQty == 0 -> Triple(Color(0xFFDCFCE7), "$totalQty unit Baik", Color(0xFF166534))
                                        goodQty == 0 -> Triple(Color(0xFFFEE2E2), "$totalQty unit Rusak Total", Color(0xFF991B1B))
                                        else -> Triple(Color(0xFFFEF3C7), "$goodQty Baik / $damagedQty Rusak", Color(0xFF92400E))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(badgeBg, MaterialTheme.shapes.small)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                }

                                // Checkbox: Semua unit rusak total
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newChecked = !isAllDamaged
                                            itemAllDamagedMap = itemAllDamagedMap.toMutableMap().apply { put(item.idBarang, newChecked) }
                                            if (newChecked) {
                                                itemDamagedCountsMap = itemDamagedCountsMap.toMutableMap().apply { put(item.idBarang, totalQty) }
                                            } else {
                                                itemDamagedCountsMap = itemDamagedCountsMap.toMutableMap().apply { put(item.idBarang, 0) }
                                            }
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = isAllDamaged,
                                        onCheckedChange = { checked ->
                                            itemAllDamagedMap = itemAllDamagedMap.toMutableMap().apply { put(item.idBarang, checked) }
                                            if (checked) {
                                                itemDamagedCountsMap = itemDamagedCountsMap.toMutableMap().apply { put(item.idBarang, totalQty) }
                                            } else {
                                                itemDamagedCountsMap = itemDamagedCountsMap.toMutableMap().apply { put(item.idBarang, 0) }
                                            }
                                        },
                                        modifier = Modifier.testTag("checkbox_all_damaged_${item.idBarang}")
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Semua unit rusak total",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                // Counter & Sub-Input for Damaged vs Good Units
                                if (!isAllDamaged) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), MaterialTheme.shapes.small)
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Jumlah Unit Rusak / Bermasalah:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        val newCount = (damagedQty - 1).coerceAtLeast(0)
                                                        itemDamagedCountsMap = itemDamagedCountsMap.toMutableMap().apply { put(item.idBarang, newCount) }
                                                    },
                                                    enabled = damagedQty > 0,
                                                    modifier = Modifier.size(32.dp).testTag("btn_minus_${item.idBarang}")
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Kurangi Unit Rusak")
                                                }
                                                Text(
                                                    text = "$damagedQty",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 12.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val newCount = (damagedQty + 1).coerceAtMost(totalQty)
                                                        itemDamagedCountsMap = itemDamagedCountsMap.toMutableMap().apply { put(item.idBarang, newCount) }
                                                    },
                                                    enabled = damagedQty < totalQty,
                                                    modifier = Modifier.size(32.dp).testTag("btn_plus_${item.idBarang}")
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Tambah Unit Rusak")
                                                }
                                            }
                                        }

                                        // Dynamic Sub-Input Sisa Counter
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (goodQty == totalQty) Color(0xFFDCFCE7) else if (goodQty > 0) Color(0xFFFEF3C7) else Color(0xFFFEE2E2),
                                                    MaterialTheme.shapes.extraSmall
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Sisa Unit Baik (Otomatis):",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = if (goodQty == totalQty) Color(0xFF166534) else if (goodQty > 0) Color(0xFF92400E) else Color(0xFF991B1B)
                                            )
                                            Text(
                                                text = "$goodQty unit",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (goodQty == totalQty) Color(0xFF166534) else if (goodQty > 0) Color(0xFF92400E) else Color(0xFF991B1B)
                                            )
                                        }
                                    }
                                }

                                // Jenis Kerusakan Dropdown & Catatan (If damagedQty > 0)
                                if (damagedQty > 0) {
                                    var itemKondisiExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = itemKondisiExpanded,
                                        onExpandedChange = { itemKondisiExpanded = it },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LunarisTextField(
                                            value = currentCondition,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Jenis Kerusakan / Status") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemKondisiExpanded) },
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                .testTag("select_kondisi_${item.idBarang}")
                                        )
                                        ExposedDropdownMenu(
                                            expanded = itemKondisiExpanded,
                                            onDismissRequest = { itemKondisiExpanded = false }
                                        ) {
                                            listOf("Rusak Ringan", "Rusak Berat", "Hilang", "Pemeliharaan").forEach { conditionOption ->
                                                DropdownMenuItem(
                                                    text = { Text(conditionOption) },
                                                    onClick = {
                                                        itemConditionsMap = itemConditionsMap.toMutableMap().apply {
                                                            put(item.idBarang, conditionOption)
                                                        }
                                                        itemKondisiExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Note/Keterangan for this item
                                    LunarisTextField(
                                        value = currentNote,
                                        onValueChange = { newNote ->
                                            itemNotesMap = itemNotesMap.toMutableMap().apply {
                                                put(item.idBarang, newNote)
                                            }
                                        },
                                        label = { Text("Catatan Kerusakan ($damagedQty unit)") },
                                        placeholder = { Text("Contoh: Tombol retak, 1 kabel hilang, dll.") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("note_${item.idBarang}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Return Conditions & Receiver
            LunarisCard(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Petugas Penerima",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Calculated overall condition
                    val consolidated = consolidateLoanItems(borrowedItemsList.value)
                    val anyDamaged = consolidated.any { item ->
                        val isAll = itemAllDamagedMap[item.idBarang] ?: false
                        val count = if (isAll) item.jumlah else (itemDamagedCountsMap[item.idBarang] ?: 0)
                        count > 0
                    }
                    val allDamagedTotal = consolidated.isNotEmpty() && consolidated.all { item ->
                        val isAll = itemAllDamagedMap[item.idBarang] ?: false
                        val count = if (isAll) item.jumlah else (itemDamagedCountsMap[item.idBarang] ?: 0)
                        count == item.jumlah
                    }

                    val calculatedOverallStatus = when {
                        !anyDamaged -> "Normal / Baik Total"
                        allDamagedTotal -> "Rusak Total / Total Loss"
                        else -> "Kondisional (Parsial: Sebagian Baik, Sebagian Rusak)"
                    }

                    LunarisTextField(
                        value = calculatedOverallStatus,
                        onValueChange = {},
                        label = { Text("Status Kondisi Transaksi (Otomatis)") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                MaterialTheme.shapes.small
                            )
                            .testTag("computed_overall_status")
                    )

                    // Nama Petugas Penerima Form Field
                    LunarisTextField(
                        value = namaPetugasKembali,
                        onValueChange = { namaPetugasKembali = it },
                        label = { Text("Nama Petugas Penerima") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_petugas_kembali")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Save Button
            Button(
                onClick = {
                    val txId = selectedTransaction?.idTransaksi
                    if (txId.isNullOrEmpty()) {
                        Toast.makeText(context, "Silakan pilih peminjaman aktif terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (namaPetugasKembali.isBlank()) {
                        Toast.makeText(context, "Nama petugas penerima harus diisi!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val consolidated = consolidateLoanItems(borrowedItemsList.value)
                    val anyDamaged = consolidated.any { item ->
                        val isAll = itemAllDamagedMap[item.idBarang] ?: false
                        val count = if (isAll) item.jumlah else (itemDamagedCountsMap[item.idBarang] ?: 0)
                        count > 0
                    }
                    val allDamagedTotal = consolidated.isNotEmpty() && consolidated.all { item ->
                        val isAll = itemAllDamagedMap[item.idBarang] ?: false
                        val count = if (isAll) item.jumlah else (itemDamagedCountsMap[item.idBarang] ?: 0)
                        count == item.jumlah
                    }

                    val calculatedKondisiKembali = when {
                        !anyDamaged -> "Normal"
                        allDamagedTotal -> "Rusak"
                        else -> "Kondisional"
                    }

                    val detailedNotes = StringBuilder()
                    detailedNotes.append("Kategori: $kategoriPeminjam\n")
                    detailedNotes.append("Tujuan: ${selectedTransaction?.tujuanPeminjaman ?: "-"}\n")
                    detailedNotes.append("Rincian Kondisi Pengembalian:\n")
                    consolidated.forEach { item ->
                        val totalQty = item.jumlah
                        val isAll = itemAllDamagedMap[item.idBarang] ?: false
                        val damagedQty = if (isAll) totalQty else (itemDamagedCountsMap[item.idBarang] ?: 0).coerceIn(0, totalQty)
                        val goodQty = totalQty - damagedQty
                        val cond = itemConditionsMap[item.idBarang] ?: "Rusak Ringan"
                        val note = itemNotesMap[item.idBarang]?.trim() ?: ""

                        if (damagedQty == 0) {
                            detailedNotes.append("- ${item.namaBarang}: $totalQty unit Baik\n")
                        } else if (goodQty == 0) {
                            detailedNotes.append("- ${item.namaBarang}: $totalQty unit $cond" + (if (note.isNotEmpty()) " (Catatan: $note)" else "") + "\n")
                        } else {
                            detailedNotes.append("- ${item.namaBarang}: $goodQty unit Baik, $damagedQty unit $cond" + (if (note.isNotEmpty()) " (Catatan: $note)" else "") + "\n")
                        }
                    }
                    val finalKeteranganKerusakan = detailedNotes.toString().trim()

                    viewModel.submitReturn(
                        idTransaksi = txId,
                        kondisiKembali = calculatedKondisiKembali,
                        namaPetugas = namaPetugasKembali,
                        tanggalKembali = tanggalText,
                        waktuKembali = waktuText,
                        keteranganKerusakan = finalKeteranganKerusakan,
                        itemConditions = itemConditionsMap,
                        itemDamagedCounts = itemDamagedCountsMap,
                        itemNotes = itemNotesMap,
                        onSuccess = {
                            Toast.makeText(context, "Pengembalian barang berhasil disimpan!", Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_simpan_pengembalian")
            ) {
                Icon(imageVector = Icons.Default.Undo, contentDescription = "Kembalikan")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Transaksi Pengembalian", fontWeight = FontWeight.Bold)
            }
        }

        // Active Transaction Selection Dialog
        if (showActiveTxSelectionDialog) {
            val borrowerTxs = activeTxState.filter { it.namaPeminjam == selectedBorrowerName }
            AlertDialog(
                onDismissRequest = { showActiveTxSelectionDialog = false },
                title = { Text("Pilih Record Peminjaman", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text("Peminjam memiliki beberapa peminjaman aktif. Silakan pilih salah satu:")
                        borrowerTxs.forEach { tx ->
                            LunarisCard(
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTransaction = tx
                                        kelasAutofill = tx.kelas
                                        coroutineScope.launch {
                                            borrowedItemsList.value = viewModel.getItemsForTransaction(tx.idTransaksi)
                                        }
                                        showActiveTxSelectionDialog = false
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Transaksi: ${tx.idTransaksi}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tanggal: ${tx.tanggal} • Jam: ${tx.waktu}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Kondisi Pinjam: ${tx.kondisi}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showActiveTxSelectionDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (showQrScanner) {
            PengembalianQrScanDialog(
                onDismiss = { showQrScanner = false },
                onQrScanned = { scannedCode ->
                    showQrScanner = false
                    // Check active transactions containing this scannedCode (ID or name)
                    val matchingTxs = activeTxState.filter { tx ->
                        activeTxItemsMap[tx.idTransaksi]?.any {
                            it.idBarang == scannedCode || it.namaBarang.equals(scannedCode, ignoreCase = true)
                        } == true
                    }

                    if (matchingTxs.size == 1) {
                        val singleTx = matchingTxs.first()
                        selectedTransaction = singleTx
                        selectedBorrowerName = singleTx.namaPeminjam
                        kelasAutofill = singleTx.kelas
                        coroutineScope.launch {
                            borrowedItemsList.value = activeTxItemsMap[singleTx.idTransaksi] ?: emptyList()
                        }
                        scannedFilterIdBarang = scannedCode
                        Toast.makeText(context, "Berhasil memindai: Otomatis memilih peminjaman ${singleTx.namaPeminjam}", Toast.LENGTH_SHORT).show()
                    } else if (matchingTxs.isEmpty()) {
                        Toast.makeText(context, "Tidak ada peminjaman aktif untuk barang '$scannedCode'!", Toast.LENGTH_LONG).show()
                    } else {
                        scannedFilterIdBarang = scannedCode
                        selectedBorrowerName = "" // clear search query so filtered list shows up
                        selectedTransaction = null
                        kelasAutofill = ""
                        borrowedItemsList.value = emptyList()
                        isBorrowerExpanded = true // expand dropdown for user to pick
                        Toast.makeText(context, "Ditemukan beberapa peminjam. Silakan pilih dari daftar yang disaring.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PengembalianQrScanDialog(
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
