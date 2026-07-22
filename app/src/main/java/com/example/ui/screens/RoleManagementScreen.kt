package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LunarisCard
import com.example.ui.theme.CarbonBlackText
import com.example.ui.theme.DeepPurpleText
import com.example.ui.theme.pastelGradientBackground
import com.example.ui.viewmodel.InventoryViewModel

data class PermissionSubItemData(
    val key: String,
    val title: String,
    val description: String,
    val defaultVal: Boolean
)

data class PermissionParentItemData(
    val parentKey: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconColor: Color,
    val subItems: List<PermissionSubItemData>
)

data class PermissionGroupData(
    val groupTitle: String,
    val groupSubtitle: String,
    val groupIcon: ImageVector,
    val items: List<PermissionParentItemData>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagementScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val studentPermissions by viewModel.studentPermissions.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    // Map to track expanded state of each parent menu card
    var expandedParents by remember { mutableStateOf(mapOf<String, Boolean>()) }

    val permissionGroups = remember {
        listOf(
            PermissionGroupData(
                groupTitle = "1. Sirkulasi & Peminjaman",
                groupSubtitle = "Fitur transaksi keluar masuk, QR code, dan log sirkulasi",
                groupIcon = Icons.Default.CloudSync,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "peminjaman",
                        title = "Menu Peminjaman Alat",
                        description = "Pengajuan & riwayat transaksi peminjaman alat",
                        icon = Icons.Default.Assignment,
                        iconBgColor = Color(0xFFD1FAE5),
                        iconColor = Color(0xFF059669),
                        subItems = listOf(
                            PermissionSubItemData("peminjaman_form", "Form Ajukan Peminjaman", "Mengisi formulir pengajuan peminjaman alat", false),
                            PermissionSubItemData("peminjaman_riwayat", "Riwayat Peminjaman", "Melihat riwayat & status peminjaman aktif/selesai", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "pengembalian",
                        title = "Menu Pengembalian Alat",
                        description = "Pengembalian barang terpinjam & pelaporan kondisi",
                        icon = Icons.Default.AssignmentReturn,
                        iconBgColor = Color(0xFFE0E7FF),
                        iconColor = Color(0xFF4F46E5),
                        subItems = listOf(
                            PermissionSubItemData("pengembalian_normal", "Pengembalian Normal", "Proses pengembalian alat dalam kondisi baik", false),
                            PermissionSubItemData("pengembalian_parsial", "Pengembalian Parsial / Rusak", "Proses pengembalian barang rusak / bertahap", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "qr_group",
                        title = "Grup QR Code",
                        description = "Pemindaian scanner & pembuat kode QR barang",
                        icon = Icons.Default.QrCode,
                        iconBgColor = Color(0xFFFCE7F3),
                        iconColor = Color(0xFFDB2777),
                        subItems = listOf(
                            PermissionSubItemData("scan_qr", "Pindai / Scan QR Code", "Memindai QR barang untuk pencarian & transaksi cepat", false),
                            PermissionSubItemData("generate_qr", "Buat / Generate QR Code", "Membuat dan mencetak label QR barang baru", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "log_transaksi",
                        title = "Menu Log Transaksi",
                        description = "Catatan rekam jejak & audit seluruh aktivitas sirkulasi",
                        icon = Icons.Default.CloudSync,
                        iconBgColor = Color(0xFFCCFBF1),
                        iconColor = Color(0xFF0D9488),
                        subItems = listOf(
                            PermissionSubItemData("log_sirkulasi", "Sirkulasi Alat", "Catatan transaksi peminjaman & pengembalian alat", false),
                            PermissionSubItemData("log_bahan_habis", "Bahan Habis", "Catatan transaksi pemakaian bahan habis pakai", false),
                            PermissionSubItemData("log_stok", "Manajemen Stok", "Catatan perubahan & penyesuaian stok", false),
                            PermissionSubItemData("log_pemeliharaan", "Pemeliharaan", "Catatan jadwal & tindakan pemeliharaan", false),
                            PermissionSubItemData("log_aktivitas", "Aktivitas Sistem", "Log audit aktivitas & pengaksesan sistem", false)
                        )
                    )
                )
            ),
            PermissionGroupData(
                groupTitle = "2. Inventaris Aset & Alat",
                groupSubtitle = "Katalog alat, status kondisi, laporan kerusakan, & pemeliharaan",
                groupIcon = Icons.Default.Build,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "alat",
                        title = "Menu Alat",
                        description = "Katalog inventaris alat, spesifikasi, import/export data",
                        icon = Icons.Default.Build,
                        iconBgColor = Color(0xFFF3E8FF),
                        iconColor = Color(0xFF7C3AED),
                        subItems = listOf(
                            PermissionSubItemData("alat_view", "Katalog Alat", "Melihat katalog & ketersediaan stok alat", false),
                            PermissionSubItemData("alat_detail", "Detail Spesifikasi", "Melihat detail spesifikasi teknis alat", false),
                            PermissionSubItemData("alat_import", "Import Data Excel/CSV", "Mengimpor data alat dari file Excel/CSV", false),
                            PermissionSubItemData("alat_export", "Export Data", "Mengekspor daftar alat ke berkas Excel", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "kondisi_alat",
                        title = "Menu Kondisi Alat",
                        description = "Inspeksi kondisi fisik & status kelayakan alat",
                        icon = Icons.Default.Info,
                        iconBgColor = Color(0xFFFFE4E6),
                        iconColor = Color(0xFFE11D48),
                        subItems = listOf(
                            PermissionSubItemData("kondisi_alat_catat", "Pencatatan Kondisi", "Mencatat hasil pemeriksaan kondisi alat", false),
                            PermissionSubItemData("kondisi_alat_view", "Riwayat Kondisi", "Melihat riwayat status kondisi kelayakan alat", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "alat_rusak",
                        title = "Alat Rusak",
                        description = "Pengaduan & rekap data kerusakan alat",
                        icon = Icons.Default.Warning,
                        iconBgColor = Color(0xFFFFECEF),
                        iconColor = Color(0xFFEF4444),
                        subItems = listOf(
                            PermissionSubItemData("alat_rusak_submit", "Tambah Alat Rusak", "Melaporkan / mencatat kejadian alat rusak", false),
                            PermissionSubItemData("alat_rusak_view", "Riwayat Alat Rusak", "Melihat daftar & riwayat alat yang rusak", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "pemeliharaan",
                        title = "Menu Pemeliharaan",
                        description = "Penjadwalan servis berkala & perawatan aset",
                        icon = Icons.Default.Build,
                        iconBgColor = Color(0xFFEFF6FF),
                        iconColor = Color(0xFF2563EB),
                        subItems = listOf(
                            PermissionSubItemData("pemeliharaan_tambah", "Tambah Pemeliharaan", "Membuat agenda pemeliharaan / perbaikan alat", false),
                            PermissionSubItemData("pemeliharaan_view", "Riwayat Pemeliharaan", "Melihat jadwal & histori perawatan alat", false)
                        )
                    )
                )
            ),
            PermissionGroupData(
                groupTitle = "3. Bahan Habis Pakai (BHP)",
                groupSubtitle = "Katalog bahan, log pemakaian praktikum, & bahan afkir",
                groupIcon = Icons.Default.Science,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "bahan",
                        title = "Menu Bahan",
                        description = "Stok bahan habis pakai praktikum & logistik",
                        icon = Icons.Default.Science,
                        iconBgColor = Color(0xFFE0F2FE),
                        iconColor = Color(0xFF0284C7),
                        subItems = listOf(
                            PermissionSubItemData("bahan_view", "Katalog Bahan", "Melihat stok & daftar bahan habis pakai", false),
                            PermissionSubItemData("bahan_detail", "Detail Spesifikasi", "Melihat rincian lokasi simpan & spesifikasi bahan", false),
                            PermissionSubItemData("bahan_import", "Import Data Excel/CSV", "Mengimpor daftar bahan dari file Excel/CSV", false),
                            PermissionSubItemData("bahan_export", "Export Data", "Mengekspor data stok bahan ke format Excel", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "pemakaian_bahan",
                        title = "Menu Pemakaian Bahan",
                        description = "Pencatatan konsumsi bahan praktikum",
                        icon = Icons.Default.ShoppingCart,
                        iconBgColor = Color(0xFFFCE7F3),
                        iconColor = Color(0xFFDB2777),
                        subItems = listOf(
                            PermissionSubItemData("pemakaian_bahan_form", "Form Pemakaian", "Mengisi form pengambilan/pemakaian bahan", false),
                            PermissionSubItemData("pemakaian_bahan_log", "Riwayat Pemakaian", "Melihat log konsumsi pemakaian bahan praktikum", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "bahan_afkir",
                        title = "Menu Bahan Afkir",
                        description = "Pengelolaan bahan rusak / kadaluwarsa",
                        icon = Icons.Default.DeleteSweep,
                        iconBgColor = Color(0xFFFFEDD5),
                        iconColor = Color(0xFFEA580C),
                        subItems = listOf(
                            PermissionSubItemData("bahan_afkir_submit", "Catat Afkir", "Pencatatan bahan kedaluwarsa / rusak (afkir)", false),
                            PermissionSubItemData("bahan_afkir_view", "Riwayat Afkir", "Melihat riwayat & daftar bahan afkir", false)
                        )
                    )
                )
            ),
            PermissionGroupData(
                groupTitle = "4. Master Data, Stok Opname & Laporan",
                groupSubtitle = "Pengelolaan data induk, audit fisik, & rekapan laporan",
                groupIcon = Icons.Default.Storage,
                items = listOf(
                    PermissionParentItemData(
                        parentKey = "master_data",
                        title = "Master Data",
                        description = "Data induk barang, kategori, ruang, & sumber dana",
                        icon = Icons.Default.Storage,
                        iconBgColor = Color(0xFFD1FAE5),
                        iconColor = Color(0xFF10B981),
                        subItems = listOf(
                            PermissionSubItemData("master_data_view", "Lihat Induk Barang & Lokasi", "Melihat struktur master data barang & daftar ruang", false),
                            PermissionSubItemData("master_data_manage", "Pengelolaan Data & Kategori", "Mengelola kategori, ruang, & parameter sarpras", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "stok_opname",
                        title = "Stok Opname",
                        description = "Audit fisik & penyesuaian ketersediaan stok gudang",
                        icon = Icons.Default.Inventory,
                        iconBgColor = Color(0xFFEFF6FF),
                        iconColor = Color(0xFF3B82F6),
                        subItems = listOf(
                            PermissionSubItemData("stok_opname_audit", "Audit Physical Count Gudang", "Memasukkan data hasil hitung fisik di lapangan", false),
                            PermissionSubItemData("stok_opname_reconcile", "Penyesuaian Fisik Stok", "Menyesuaikan angka stok sistem dengan fisik", false)
                        )
                    ),
                    PermissionParentItemData(
                        parentKey = "laporan",
                        title = "Menu Laporan",
                        description = "Ringkasan & laporan terpadu seluruh modul sarpras",
                        icon = Icons.Default.Assessment,
                        iconBgColor = Color(0xFFECFEFF),
                        iconColor = Color(0xFF06B6D4),
                        subItems = listOf(
                            PermissionSubItemData("laporan_ringkasan", "Ringkasan", "Melihat eksekutif summary & grafik laporan", false),
                            PermissionSubItemData("laporan_alat", "Alat", "Laporan rekapitulasi data alat", false),
                            PermissionSubItemData("laporan_bahan", "Bahan", "Laporan rekapitulasi data bahan habis pakai", false),
                            PermissionSubItemData("laporan_afkir", "Afkir", "Laporan rekapitulasi bahan afkir", false),
                            PermissionSubItemData("laporan_peminjaman", "Peminjaman", "Laporan rekapitulasi peminjaman alat", false),
                            PermissionSubItemData("laporan_pengembalian", "Pengembalian", "Laporan rekapitulasi pengembalian alat", false),
                            PermissionSubItemData("laporan_alat_rusak", "Alat Rusak", "Laporan rekapitulasi kerusakan alat", false),
                            PermissionSubItemData("laporan_pemeliharaan", "Pemeliharaan", "Laporan rekapitulasi pemeliharaan alat", false),
                            PermissionSubItemData("laporan_export_excel", "Export Laporan Excel", "Mengekspor berkas laporan ke format Excel", false),
                            PermissionSubItemData("laporan_print_pdf", "Cetak / PDF Report", "Mencetak berkas laporan ke format PDF", false)
                        )
                    )
                )
            )
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pengaturan Akses",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Dynamic User Level Control (Hierarki Parent-Child)",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_role_management")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("btn_reset_permissions")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Permission",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .shadow(elevation = 3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFEA580C),
                                Color(0xFFD97706)
                            )
                        )
                    )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pastelGradientBackground(isDark = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Info Header Card
                LunarisCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEDD5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Akses Security",
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hierarki Akses Role Siswa",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepPurpleText
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Tombol Switch Induk (Parent) merefleksikan status sub-menu di dalamnya: Aktif Penuh (Hijau), Non-Aktif (Abu-abu), atau Parsial (Kuning/Oranye). Tekan panah untuk mengatur sub-menu.",
                                fontSize = 11.sp,
                                color = CarbonBlackText.copy(alpha = 0.75f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Permission Groups
                permissionGroups.forEach { group ->
                    Text(
                        text = group.groupTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurpleText,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = group.groupSubtitle,
                        fontSize = 11.sp,
                        color = CarbonBlackText.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        group.items.forEach { parentItem ->
                            val isExpanded = expandedParents[parentItem.parentKey] ?: false

                            // Compute child active counts
                            val activeSubCount = parentItem.subItems.count { sub ->
                                studentPermissions[sub.key] ?: sub.defaultVal
                            }
                            val totalSubCount = parentItem.subItems.size

                            // Status Parent Logic: Full, Partial, Off
                            val isFullActive = activeSubCount == totalSubCount
                            val isOff = activeSubCount == 0
                            val isPartial = !isFullActive && !isOff

                            val parentBadgeText = when {
                                isFullActive -> "Aktif Penuh"
                                isPartial -> "Parsial ($activeSubCount/$totalSubCount)"
                                else -> "Non-Aktif"
                            }

                            val parentBadgeBgColor = when {
                                isFullActive -> Color(0xFFDCFCE7)
                                isPartial -> Color(0xFFFEF3C7)
                                else -> Color(0xFFF3F4F6)
                            }

                            val parentBadgeTextColor = when {
                                isFullActive -> Color(0xFF15803D)
                                isPartial -> Color(0xFFB45309)
                                else -> Color(0xFF6B7280)
                            }

                            val parentSwitchChecked = activeSubCount > 0

                            LunarisCard(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPartial) Color(0xFFFCD34D) else Color(0xFFE9D5FF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    // Parent Header Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    expandedParents = expandedParents.toMutableMap().apply {
                                                        put(parentItem.parentKey, !isExpanded)
                                                    }
                                                }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(parentItem.iconBgColor)
                                            ) {
                                                Icon(
                                                    imageVector = parentItem.icon,
                                                    contentDescription = parentItem.title,
                                                    tint = parentItem.iconColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = parentItem.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CarbonBlackText
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(parentBadgeBgColor)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = parentBadgeText,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = parentBadgeTextColor
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = parentItem.description,
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Parent Switch Logic
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(
                                                checked = parentSwitchChecked,
                                                onCheckedChange = { _ ->
                                                    // Toggling Parent:
                                                    // If currently FULL -> turn ALL sub-items OFF
                                                    // If currently OFF or PARTIAL -> turn ALL sub-items ON
                                                    val targetVal = !isFullActive
                                                    val updates = mutableMapOf<String, Boolean>()
                                                    updates[parentItem.parentKey] = targetVal
                                                    parentItem.subItems.forEach { sub ->
                                                        updates[sub.key] = targetVal
                                                    }
                                                    viewModel.updateStudentPermissionsBatch(updates)

                                                    val statusMsg = if (targetVal) "diaktifkan penuh" else "dinonaktifkan"
                                                    Toast.makeText(
                                                        context,
                                                        "Grup '${parentItem.title}' $statusMsg untuk Siswa",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = if (isPartial) Color(0xFFD97706) else Color(0xFF10B981),
                                                    uncheckedThumbColor = Color.White,
                                                    uncheckedTrackColor = Color(0xFFD1D5DB)
                                                ),
                                                modifier = Modifier.testTag("parent_switch_${parentItem.parentKey}")
                                            )

                                            IconButton(
                                                onClick = {
                                                    expandedParents = expandedParents.toMutableMap().apply {
                                                        put(parentItem.parentKey, !isExpanded)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp).testTag("btn_expand_${parentItem.parentKey}")
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Expand Submenu",
                                                    tint = Color.Gray
                                                )
                                            }
                                        }
                                    }

                                    // Expandable Sub-Items List
                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFF8FAFC))
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountTree,
                                                    contentDescription = "Submenu",
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Sub-Menu Fitur Spesifik:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF475569)
                                                )
                                            }

                                            HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFE2E8F0))

                                            parentItem.subItems.forEachIndexed { subIdx, subItem ->
                                                val isSubChecked = studentPermissions[subItem.key] ?: subItem.defaultVal

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = subItem.title,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = CarbonBlackText
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(
                                                                        if (isSubChecked) Color(0xFFDCFCE7) else Color(0xFFF3F4F6)
                                                                    )
                                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (isSubChecked) "Aktif" else "Mati",
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isSubChecked) Color(0xFF15803D) else Color(0xFF6B7280)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = subItem.description,
                                                            fontSize = 10.sp,
                                                            color = Color.Gray,
                                                            lineHeight = 12.sp
                                                        )
                                                    }

                                                    Switch(
                                                        checked = isSubChecked,
                                                        onCheckedChange = { newSubVal ->
                                                            // Update sub item value
                                                            val updates = mutableMapOf<String, Boolean>()
                                                            updates[subItem.key] = newSubVal

                                                            // Recalculate parent state
                                                            val futureActiveCount = parentItem.subItems.count { sub ->
                                                                if (sub.key == subItem.key) newSubVal
                                                                else (studentPermissions[sub.key] ?: sub.defaultVal)
                                                            }
                                                            updates[parentItem.parentKey] = (futureActiveCount > 0)

                                                            viewModel.updateStudentPermissionsBatch(updates)

                                                            val statusTxt = if (newSubVal) "DILANGSUNGKAN" else "DISEMBUNYIKAN"
                                                            Toast.makeText(
                                                                context,
                                                                "Sub-menu '${subItem.title}' $statusTxt",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        },
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = Color.White,
                                                            checkedTrackColor = Color(0xFF10B981),
                                                            uncheckedThumbColor = Color.White,
                                                            uncheckedTrackColor = Color(0xFFCBD5E1)
                                                        ),
                                                        modifier = Modifier.scale(0.85f).testTag("sub_switch_${subItem.key}")
                                                    )
                                                }

                                                if (subIdx < parentItem.subItems.size - 1) {
                                                    HorizontalDivider(
                                                        thickness = 0.5.dp,
                                                        color = Color(0xFFE2E8F0)
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

                // Reset Button
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFEA580C)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEA580C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_reset_defaults_bottom")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reset ke Pengaturan Akses Default",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFEA580C)
                )
            },
            title = {
                Text(
                    text = "Reset Hak Akses Siswa?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Pengaturan hak akses role Siswa dan hierarki sub-menu akan dikembalikan ke konfigurasi standar (Peminjaman, Pengembalian, Daftar Alat, Daftar Bahan, dan Scan QR diizinkan).",
                    fontSize = 13.sp,
                    color = CarbonBlackText.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetStudentPermissionsToDefault()
                        showResetDialog = false
                        Toast.makeText(context, "Hak akses Siswa berhasil di-reset ke default!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                ) {
                    Text("Ya, Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
