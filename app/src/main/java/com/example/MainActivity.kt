package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InputBarangScreen
import com.example.ui.screens.LogTransaksiScreen
import com.example.ui.screens.LaporanScreen
import com.example.ui.screens.PeminjamanScreen
import com.example.ui.screens.PengembalianScreen
import com.example.ui.screens.PengaturanScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.StokOpnameScreen
import com.example.ui.screens.ScanQrScreen
import com.example.ui.screens.UserManagementScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.theme.LunarisTheme
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.screens.LoginScreen

import com.example.ui.screens.MasterDataScreen
import com.example.ui.screens.KondisiAlatScreen
import com.example.ui.screens.AlatScreen
import com.example.ui.screens.BahanScreen
import com.example.ui.screens.PemakaianBahanScreen
import com.example.ui.screens.BahanAfkirScreen
import com.example.ui.screens.AlatRusakScreen
import com.example.ui.screens.PemeliharaanScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RoleManagementScreen

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.pastelGradientBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Developer Branding on first launch
        val settingsRepository = com.example.data.repository.SettingsRepository(this)
        settingsRepository.checkAndInitializeBranding()

        // Seed initial default data (Units, Sumber Dana, Kondisi)
        com.example.data.database.DatabaseInitializer.initialize(this)

        setContent {
            val inventoryViewModel: InventoryViewModel = viewModel()
            val isLoggedIn by inventoryViewModel.isLoggedIn.collectAsState()
            val userRole by inventoryViewModel.userRole.collectAsState()
            val studentPermissions by inventoryViewModel.studentPermissions.collectAsState()
            val themePreference by inventoryViewModel.appTheme.collectAsState()
            val isDrawerOpen by inventoryViewModel.isDrawerOpen.collectAsState()

            val darkTheme = false

            LunarisTheme(darkTheme = false) {
                if (!isLoggedIn) {
                    LoginScreen(
                        viewModel = inventoryViewModel,
                        onLoginSuccess = {}
                    )
                } else {
                    val navController = rememberNavController()

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    val hasMasterDataPerm = studentPermissions["master_data"] == true || studentPermissions["master_data_view"] == true || studentPermissions["master_data_manage"] == true
                    val hasScanQrPerm = studentPermissions["scan_qr"] == true || studentPermissions["generate_qr"] == true || studentPermissions["qr_group"] == true
                    val hasLaporanPerm = studentPermissions["laporan"] == true || studentPermissions["laporan_ringkasan"] == true || studentPermissions["laporan_alat"] == true || studentPermissions["laporan_bahan"] == true || studentPermissions["laporan_afkir"] == true || studentPermissions["laporan_peminjaman"] == true || studentPermissions["laporan_pengembalian"] == true || studentPermissions["laporan_alat_rusak"] == true || studentPermissions["laporan_pemeliharaan"] == true || studentPermissions["laporan_export_excel"] == true || studentPermissions["laporan_print_pdf"] == true

                    val mainDestinations = if (userRole == "admin") {
                        listOf("dashboard", "master_data", "scan_qr", "laporan", "profil")
                    } else {
                        val list = mutableListOf("dashboard")
                        if (hasMasterDataPerm) list.add("master_data")
                        if (hasScanQrPerm) list.add("scan_qr")
                        if (hasLaporanPerm) list.add("laporan")
                        list.add("profil")
                        list
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pastelGradientBackground(isDark = darkTheme)
                    ) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                val showBottomBar = (currentRoute in mainDestinations) && !isDrawerOpen
                                AnimatedVisibility(
                                    visible = showBottomBar,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                ) {
                                    NavigationBar(
                                        containerColor = if (darkTheme) Color(0xFF120E1C) else Color(0xFFF3E8FF),
                                        tonalElevation = 0.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        val tabs = if (userRole == "admin") {
                                            listOf(
                                                Triple("Dashboard", "dashboard", Icons.Default.Home),
                                                Triple("Master Data", "master_data", Icons.Default.Storage),
                                                Triple("Scan QR", "scan_qr", Icons.Default.QrCode),
                                                Triple("Laporan", "laporan", Icons.Default.Assessment),
                                                Triple("Profil", "profil", Icons.Default.Person)
                                            )
                                        } else {
                                            val t = mutableListOf(Triple("Dashboard", "dashboard", Icons.Default.Home))
                                            if (hasMasterDataPerm) {
                                                t.add(Triple("Master Data", "master_data", Icons.Default.Storage))
                                            }
                                            if (hasScanQrPerm) {
                                                t.add(Triple("Scan QR", "scan_qr", Icons.Default.QrCode))
                                            }
                                            if (hasLaporanPerm) {
                                                t.add(Triple("Laporan", "laporan", Icons.Default.Assessment))
                                            }
                                            t.add(Triple("Profil", "profil", Icons.Default.Person))
                                            t
                                        }
                                        tabs.forEach { (label, route, icon) ->
                                            val isSelected = currentRoute == route
                                            val activeColor = if (darkTheme) Color(0xFFD8B4FE) else Color(0xFF5B21B6)
                                            val inactiveColor = if (darkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)

                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = {
                                                    if (currentRoute != route) {
                                                        navController.navigate(route) {
                                                            popUpTo("dashboard") { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = label,
                                                        tint = if (isSelected) activeColor else inactiveColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) activeColor else inactiveColor
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    indicatorColor = if (darkTheme) Color(0x1F2E2445) else Color(0x1F5B21B6)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = "dashboard",
                                modifier = Modifier.padding(
                                    top = innerPadding.calculateTopPadding(),
                                    bottom = 0.dp,
                                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr)
                                )
                            ) {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = inventoryViewModel,
                                onNavigateToMenu = { menuRoute ->
                                    inventoryViewModel.logMenuVisit(menuRoute)
                                    val destination = when (menuRoute) {
                                        "Peminjaman" -> "peminjaman"
                                        "Pengembalian" -> "pengembalian"
                                        "Input Barang" -> "master_data"
                                        "Master Data" -> "master_data"
                                        "Stok Opname" -> "stok_opname"
                                        "Log Transaksi" -> "log_transaksi"
                                        "Laporan" -> "laporan"
                                        "Scan QR" -> "scan_qr"
                                        "Backup & Restore" -> "backup_restore"
                                        "Pengaturan" -> "pengaturan"
                                        "Pengaturan Akses", "Hak Akses", "Role Management" -> "role_management"
                                        "Manajemen Pengguna", "User Management" -> "user_management"
                                        "Kondisi Alat" -> "kondisi_alat"
                                        "Alat" -> "alat"
                                        "Bahan" -> "bahan"
                                        "Pemakaian Bahan" -> "pemakaian_bahan"
                                        "Bahan Afkir" -> "bahan_afkir"
                                        "Alat Rusak" -> "alat_rusak"
                                        "Pemeliharaan" -> "pemeliharaan"
                                        "Profil" -> "profil"
                                        else -> "dashboard"
                                    }
                                    navController.navigate(destination)
                                }
                            )
                        }
                        composable(
                            "peminjaman?scannedId={scannedId}",
                            arguments = listOf(navArgument("scannedId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            })
                        ) { backStackEntry ->
                            val scannedId = backStackEntry.arguments?.getString("scannedId")
                            PeminjamanScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                initialScannedId = scannedId
                            )
                        }
                        composable(
                            "pengembalian?scannedId={scannedId}",
                            arguments = listOf(navArgument("scannedId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            })
                        ) { backStackEntry ->
                            val scannedId = backStackEntry.arguments?.getString("scannedId")
                            PengembalianScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                initialScannedId = scannedId
                            )
                        }
                        composable("scan_qr") {
                            ScanQrScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPeminjaman = { id ->
                                    navController.navigate("peminjaman?scannedId=$id") {
                                        popUpTo("dashboard")
                                    }
                                },
                                onNavigateToPengembalian = { id ->
                                    navController.navigate("pengembalian?scannedId=$id") {
                                        popUpTo("dashboard")
                                    }
                                }
                            )
                        }
                        composable("alat") {
                            AlatScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("bahan") {
                            BahanScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("pemakaian_bahan") {
                            PemakaianBahanScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("bahan_afkir") {
                            BahanAfkirScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("alat_rusak") {
                            AlatRusakScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("pemeliharaan") {
                            PemeliharaanScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("master_data") {
                            MasterDataScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("stok_opname") {
                            StokOpnameScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("log_transaksi") {
                            LogTransaksiScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("laporan") {
                            LaporanScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("pengaturan") {
                            PengaturanScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("backup_restore") {
                            BackupScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("profil") {
                            ProfileScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("kondisi_alat") {
                            KondisiAlatScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("role_management") {
                            RoleManagementScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("user_management") {
                            UserManagementScreen(
                                viewModel = inventoryViewModel,
                                onNavigateBack = { navController.popBackStack() }
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
