package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import com.example.ui.theme.DeepPurpleText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(
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
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }



    val sheetsUrl by viewModel.sheetsUrl.collectAsState()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    var urlInput by remember(sheetsUrl) { mutableStateOf(sheetsUrl) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showClearTransactionsDialog by remember { mutableStateOf(false) }

    // Google Apps Script template text for user to copy-paste
    val googleAppsScriptTemplate = """
// 1. BUAT SPREADSHEET GOOGLE BARU
// 2. BUKA EKSTENSI > APPS SCRIPT
// 3. PASTE KODE BERIKUT:

function doGet(e) {
  var action = e.parameter.action;
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  if (action === "getItems") {
    var sheet = getOrCreateSheet(ss, "Barang");
    var data = sheet.getDataRange().getValues();
    var result = [];
    for (var i = 1; i < data.length; i++) {
      result.push({
        idBarang: data[i][0].toString(),
        namaBarang: data[i][1].toString(),
        stokAwal: parseInt(data[i][2]) || 0
      });
    }
    return ContentService.createTextOutput(JSON.stringify(result)).setMimeType(ContentService.MimeType.JSON);
  }
  
  if (action === "getLoans") {
    var sheet = getOrCreateSheet(ss, "Peminjaman");
    var data = sheet.getDataRange().getValues();
    var result = [];
    for (var i = 1; i < data.length; i++) {
      result.push({
        idTransaksi: data[i][0].toString(),
        tanggal: data[i][1].toString(),
        namaPeminjam: data[i][2].toString(),
        kelas: data[i][3].toString(),
        waktu: data[i][4].toString(),
        kondisi: data[i][5].toString(),
        namaPetugas: data[i][6].toString(),
        status: data[i][7].toString(),
        idBarang: data[i][8].toString(),
        namaBarang: data[i][9].toString(),
        jumlah: parseInt(data[i][10]) || 0,
        tanggalKembali: data[i][11] ? data[i][11].toString() : null,
        waktuKembali: data[i][12] ? data[i][12].toString() : null,
        kondisiKembali: data[i][13] ? data[i][13].toString() : null,
        petugasKembali: data[i][14] ? data[i][14].toString() : null
      });
    }
    return ContentService.createTextOutput(JSON.stringify(result)).setMimeType(ContentService.MimeType.JSON);
  }
  
  return ContentService.createTextOutput(JSON.stringify({error: "Invalid action"})).setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  var postData = JSON.parse(e.postData.contents);
  var action = postData.action;
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  if (action === "syncItems") {
    var sheet = getOrCreateSheet(ss, "Barang");
    sheet.clear();
    sheet.appendRow(["ID_Barang", "Nama_Barang", "Stok_Awal"]);
    var items = postData.items || [];
    for (var i = 0; i < items.length; i++) {
      sheet.appendRow([items[i].idBarang, items[i].namaBarang, items[i].stokAwal]);
    }
    return ContentService.createTextOutput(JSON.stringify({success: true, message: "Items synchronized"})).setMimeType(ContentService.MimeType.JSON);
  }
  
  if (action === "addLoan") {
    var sheet = getOrCreateSheet(ss, "Peminjaman");
    if (sheet.getLastRow() === 0) {
      sheet.appendRow([
        "ID_Transaksi", "Tanggal", "Nama_Peminjam", "Kelas", "Waktu", "Kondisi_Awal", "Nama_Petugas_Awal", "Status",
        "ID_Barang", "Nama_Barang", "Jumlah", "Tanggal_Kembali", "Waktu_Kembali", "Kondisi_Kembali", "Nama_Petugas_Kembali"
      ]);
    }
    var loan = postData.loan;
    var items = postData.items || [];
    for (var i = 0; i < items.length; i++) {
      sheet.appendRow([
        loan.idTransaksi, loan.tanggal, loan.namaPeminjam, loan.kelas, loan.waktu, loan.kondisi, loan.namaPetugas, loan.status,
        items[i].idBarang, items[i].namaBarang, items[i].jumlah, "", "", "", ""
      ]);
    }
    return ContentService.createTextOutput(JSON.stringify({success: true, message: "Loan added"})).setMimeType(ContentService.MimeType.JSON);
  }
  
  if (action === "returnLoan") {
    var sheet = getOrCreateSheet(ss, "Peminjaman");
    var data = sheet.getDataRange().getValues();
    var idTransaksi = postData.idTransaksi;
    var updatedCount = 0;
    
    for (var i = 1; i < data.length; i++) {
      if (data[i][0].toString() === idTransaksi) {
        var row = i + 1;
        sheet.getRange(row, 8).setValue("Kembali");
        sheet.getRange(row, 12).setValue(postData.tanggalKembali);
        sheet.getRange(row, 13).setValue(postData.waktuKembali);
        sheet.getRange(row, 14).setValue(postData.kondisiKembali);
        sheet.getRange(row, 15).setValue(postData.petugasKembali);
        updatedCount++;
      }
    }
    return ContentService.createTextOutput(JSON.stringify({success: true, message: "Loan returned"})).setMimeType(ContentService.MimeType.JSON);
  }
  
  return ContentService.createTextOutput(JSON.stringify({error: "Invalid action"})).setMimeType(ContentService.MimeType.JSON);
}

function getOrCreateSheet(ss, name) {
  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    if (name === "Barang") {
      sheet.appendRow(["ID_Barang", "Nama_Barang", "Stok_Awal"]);
    } else if (name === "Peminjaman") {
      sheet.appendRow([
        "ID_Transaksi", "Tanggal", "Nama_Peminjam", "Kelas", "Waktu", "Kondisi_Awal", "Nama_Petugas_Awal", "Status",
        "ID_Barang", "Nama_Barang", "Jumlah", "Tanggal_Kembali", "Waktu_Kembali", "Kondisi_Kembali", "Nama_Petugas_Kembali"
      ]);
    }
  }
  return sheet;
}

// 4. KLIK "TERAPKAN" > "DEPLOYMENT BARU"
// 5. PILIH JENIS DEPLOYMENT: "WEB APP"
// 6. AKSES: "ANYONE / SIAPA SAJA" (PENTING!)
// 7. COPY URL DAN PASTE DI APLIKASI ANDROID!
    """.trimIndent()

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
                                text = "Pengaturan & Integrasi",
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
            if (userRole == "admin") {
                // Section 0: User Management Card
                LunarisCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Manajemen Pengguna & Level Akses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Daftarkan pengguna baru, atur level akses (Super Admin, Admin, Siswa), dan kelola akun terdaftar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Button(
                            onClick = { onNavigateBack(); /* Will be routed via drawer or back navigation */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_open_user_management")
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "User Management")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buka Layar Manajemen Pengguna", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section 1: Google Sheets Sync Setup
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
                            text = "Integrasi Google Sheets (Real-Time)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // URL Web App Input
                        LunarisTextField(
                            value = urlInput,
                            onValueChange = {
                                urlInput = it
                                viewModel.updateSheetsUrl(it.trim())
                            },
                            label = { Text("URL Google Sheets Web App") },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_sheets_url")
                        )

                        // Auto Sync Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Sinkronisasi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Kirim otomatis data ke Google Sheets setiap kali transaksi disimpan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Switch(
                                checked = autoSyncEnabled,
                                onCheckedChange = { viewModel.updateAutoSyncEnabled(it) },
                                modifier = Modifier.testTag("switch_auto_sync")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Sync Stats & Action
                        Text(
                            text = "Sinkronisasi Terakhir: $lastSyncTime",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        if (isSyncing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = syncProgress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!syncError.isNullOrEmpty()) {
                            Text(
                                text = syncError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (sheetsUrl.isEmpty()) {
                                    Toast.makeText(context, "Silakan masukkan URL Web App terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.forceSyncWithSheets()
                            },
                            enabled = !isSyncing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_sinkron_sekarang")
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Sinkron")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tarik & Sinkronkan Sekarang", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section 3: Tutorial / Copy Script Code
                LunarisCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Petunjuk Deployment Google Sheets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("Google Apps Script Gudang", googleAppsScriptTemplate)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Kode Apps Script berhasil disalin ke Clipboard!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.testTag("btn_copy_script")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Kode Script",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "Klik tombol ikon salin di kanan atas untuk mengambil kode Google Apps Script, kemudian tempel di Ekstensi > Apps Script pada Spreadsheet Anda. Setelah itu deploy sebagai Web App dengan izin akses ke 'Anyone' agar database terhubung real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Small scrollable code visualizer box
                        LunarisCard(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = googleAppsScriptTemplate,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Section 4: Maintenance / Clear All Data
                LunarisCard(
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Area Bahaya (Maintenance)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = "Tombol di bawah ini akan membersihkan data transaksi, riwayat peminjaman, pemakaian bahan, afkir, alat rusak, pemeliharaan, dan log transaksi dari database lokal dan Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )

                        Button(
                            onClick = { showClearTransactionsDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_clear_transactions")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Transactions")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kosongkan Seluruh Residu Transaksi (0 Items)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_clear_data")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Clear All")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset & Hapus Semua Data Lokal", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Dibuat oleh:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = stringResource(id = R.string.app_author),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("app_author_credit")
                )
                Text(
                    text = "Versi 1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Clear transactions confirm dialog
        if (showClearTransactionsDialog) {
            AlertDialog(
                onDismissRequest = { showClearTransactionsDialog = false },
                title = { Text("Kosongkan Seluruh Residu Transaksi?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = { Text("Seluruh riwayat peminjaman, pengembalian, pemakaian bahan, bahan afkir, alat rusak, pemeliharaan, dan log transaksi lokal serta di Firestore akan dihapus total (0 items). Master data barang tetap aman.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllTransactionsData {
                                Toast.makeText(context, "Seluruh data residu transaksi berhasil dikosongkan!", Toast.LENGTH_LONG).show()
                                showClearTransactionsDialog = false
                            }
                        },
                        modifier = Modifier.testTag("btn_confirm_clear_transactions")
                    ) {
                        Text("Ya, Kosongkan Total", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearTransactionsDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Reset local database confirm dialog
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = { Text("Reset Seluruh Data Lokal?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = { Text("Tindakan ini tidak dapat dibatalkan! Semua daftar barang dan log transaksi lokal di HP akan dihapus permanen.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllLocalData {
                                Toast.makeText(context, "Database lokal berhasil dikosongkan!", Toast.LENGTH_SHORT).show()
                                showResetConfirmDialog = false
                            }
                        },
                        modifier = Modifier.testTag("btn_confirm_reset")
                    ) {
                        Text("Ya, Hapus", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }


    }
}
