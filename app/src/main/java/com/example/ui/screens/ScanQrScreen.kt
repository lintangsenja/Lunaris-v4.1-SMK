@file:kotlin.OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class
)

package com.example.ui.screens
import com.example.ui.components.LunarisCard
import com.example.ui.components.LunarisTextField

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.DeepPurpleText
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ItemWithStock
import com.example.ui.viewmodel.InventoryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPeminjaman: (String) -> Unit,
    onNavigateToPengembalian: (String) -> Unit,
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
    val appBarContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else DeepPurpleText
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    val userRole by viewModel.userRole.collectAsState()
    val studentPermissions by viewModel.studentPermissions.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pindai QR", "Buat QR")

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
                                text = "Administrasi QR Code",
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = {
                                    if (index == 1 && userRole == "siswa" && studentPermissions["generate_qr"] == false) {
                                        Toast.makeText(context, "Akses 'Buat/Generate QR' dibatasi oleh Admin untuk Siswa", Toast.LENGTH_SHORT).show()
                                    } else if (index == 0 && userRole == "siswa" && studentPermissions["scan_qr"] == false) {
                                        Toast.makeText(context, "Akses 'Pindai/Scan QR' dibatasi oleh Admin untuk Siswa", Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedTab = index
                                    }
                                },
                                text = { Text(title, fontWeight = FontWeight.Bold) }
                            )
                        }
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
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> ScannerTab(viewModel, onNavigateToPeminjaman, onNavigateToPengembalian)
                1 -> GeneratorTab(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerTab(
    viewModel: InventoryViewModel,
    onNavigateToPeminjaman: (String) -> Unit,
    onNavigateToPengembalian: (String) -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.itemsWithStock.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var scannedItem by remember { mutableStateOf<ItemWithStock?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControlState by remember { mutableStateOf<CameraControl?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Callback when QR code is scanned
    val handleQrScanned = { qrText: String ->
        val matchedItem = items.find { it.idBarang.equals(qrText.trim(), ignoreCase = true) }
        if (matchedItem != null) {
            if (userRole != "admin" && !matchedItem.isBorrowable) {
                Toast.makeText(context, "Barang '${matchedItem.namaBarang}' tidak diperbolehkan untuk dipinjam oleh Siswa!", Toast.LENGTH_LONG).show()
            } else {
                scannedItem = matchedItem
                showResultDialog = true
            }
        } else {
            // Check if it matches format to prevent showing toast repeatedly
            Toast.makeText(context, "Kode QR tidak dikenali: $qrText", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreviewView(
                onQrScanned = { handleQrScanned(it) },
                isFlashOn = isFlashOn,
                onCameraControlReady = { cameraControlState = it }
            )

            // Scanning Laser Line Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Centered Scanner and Info Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp), // offset upwards slightly to clear space for the bottom emulator debug helper
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Centered Scanner Box
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(3.dp, Color(0xFF4F46E5), RoundedCornerShape(24.dp))
                    ) {
                        // Scanning laser line animation
                        val infiniteTransition = rememberInfiniteTransition(label = "laser")
                        val laserY by infiniteTransition.animateFloat(
                            initialValue = 10f,
                            targetValue = 250f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "laser_y"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = laserY.dp)
                                .background(Color.Red)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Flashlight and Info Column
                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            cameraControlState?.enableTorch(isFlashOn)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .testTag("flashlight_toggle")
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Senter",
                            tint = if (isFlashOn) Color(0xFFD69E2E) else Color.DarkGray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Arahkan kamera ke Kode QR Barang",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // No Permission Placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Kamera",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Izin Kamera Dibutuhkan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Fitur pemindaian QR membutuhkan akses kamera bawaan perangkat Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Aktifkan Kamera")
                    }
                }
            }
        }

        // Emulator Simulation Fallback block (Always visible for easy testing in Browser)
        LunarisCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🛠️ Emulator Debug Helper",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4F46E5)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (items.isEmpty()) {
                    Text("Belum ada barang terdaftar di sistem.", fontSize = 10.sp, color = Color.Gray)
                } else {
                    var selectedItemForSim by remember { mutableStateOf(items.first().idBarang) }
                    var expandedSimDropdown by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedSimDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val textLabel = items.find { it.idBarang == selectedItemForSim }?.let { "${it.idBarang} - ${it.namaBarang}" } ?: "Pilih Barang"
                                Text(textLabel, fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = expandedSimDropdown,
                                onDismissRequest = { expandedSimDropdown = false }
                            ) {
                                items.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text("[${item.idBarang}] ${item.namaBarang}", fontSize = 11.sp) },
                                        onClick = {
                                            selectedItemForSim = item.idBarang
                                            expandedSimDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { handleQrScanned(selectedItemForSim) },
                            modifier = Modifier.testTag("simulate_scan_button")
                        ) {
                            Text("Simulasi Scan", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Pop-up dialog on successful scan
        if (showResultDialog && scannedItem != null) {
            AlertDialog(
                onDismissRequest = { showResultDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = "QR", tint = Color(0xFF4F46E5))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Barang Terdeteksi", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = scannedItem!!.namaBarang,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Divider(color = Color(0xFFF1F5F9))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ID Barang:", fontSize = 12.sp, color = Color.Gray)
                            Text(scannedItem!!.idBarang, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Stok Fisik:", fontSize = 12.sp, color = Color.Gray)
                            Text("${scannedItem!!.stokTersedia} / ${scannedItem!!.stokAwal}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Prominent "Stok Tersedia: X unit"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (scannedItem!!.stokTersedia > 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (scannedItem!!.stokTersedia > 0) "Stok Tersedia: ${scannedItem!!.stokTersedia} unit" else "Stok Tersedia: 0 unit (Habis)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (scannedItem!!.stokTersedia > 0) Color(0xFF047857) else Color(0xFFB91C1C)
                            )
                        }
                    }
                },
                confirmButton = {
                    val isScannedOutOfStock = scannedItem!!.stokTersedia == 0
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showResultDialog = false
                                onNavigateToPeminjaman(scannedItem!!.idBarang)
                            },
                            enabled = !isScannedOutOfStock,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScannedOutOfStock) Color.Gray else Color(0xFF4F46E5),
                                disabledContainerColor = Color.LightGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Keluar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isScannedOutOfStock) "Stok Habis" else "Proses Peminjaman (Barang Keluar)")
                        }

                        OutlinedButton(
                            onClick = {
                                showResultDialog = false
                                onNavigateToPengembalian(scannedItem!!.idBarang)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Masuk")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Proses Pengembalian (Barang Masuk)")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResultDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Batal", textAlign = TextAlign.Center)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewView(
    onQrScanned: (String) -> Unit,
    isFlashOn: Boolean,
    onCameraControlReady: (CameraControl) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraExecutor.shutdown()
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Preview UseCase
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                // ImageAnalysis UseCase for ML Kit scanning
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScanning.getClient()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    if (rawValue != null) {
                                        onQrScanned(rawValue)
                                        break
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    onCameraControlReady(camera.cameraControl)
                    camera.cameraControl.enableTorch(isFlashOn)
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }

            }, androidx.core.content.ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun GeneratorTab(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val items by viewModel.itemsWithStock.collectAsState()

    var isManualRegistrationMode by remember { mutableStateOf(false) }

    // Dropdown and search states
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<ItemWithStock?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Generated QR Output State
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedItemName by remember { mutableStateOf("") }
    var generatedItemId by remember { mutableStateOf("") }

    // Settings for persistent QR marker
    val settingsRepo = remember { viewModel.settingsRepository }
    var generatedSet by remember { mutableStateOf(settingsRepo.getGeneratedQrCodes()) }

    // Dialog warnings
    var showRegenerateWarning by remember { mutableStateOf(false) }

    val categoriesEntities by viewModel.allCategories.collectAsState()
    val unitsEntities by viewModel.allUnits.collectAsState()

    val categories = remember(categoriesEntities) {
        categoriesEntities.map { it.name }.ifEmpty {
            listOf("Elektronik", "Alat Tulis", "Sarana Prasarana", "Olahraga", "Logistik")
        }
    }
    val units = remember(unitsEntities) {
        unitsEntities.map { it.name }.ifEmpty {
            listOf("Pcs", "Unit", "Set", "Pack", "Buku")
        }
    }

    // Manual registration form states
    var manualNama by remember { mutableStateOf("") }
    var manualKategori by remember(categories) { mutableStateOf(categories.firstOrNull() ?: "Elektronik") }
    var manualSatuan by remember(units) { mutableStateOf(units.firstOrNull() ?: "Pcs") }
    var manualStok by remember { mutableStateOf("1") }

    var expandedKategoriDropdown by remember { mutableStateOf(false) }
    var expandedSatuanDropdown by remember { mutableStateOf(false) }

    // Helper to generate a QR
    val triggerQrCodeGeneration = { content: String, name: String ->
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            generatedBitmap = bmp
            generatedItemName = name
            generatedItemId = content
            // Persist that QR has been generated
            settingsRepo.markQrCodeGenerated(content)
            generatedSet = settingsRepo.getGeneratedQrCodes()
            Toast.makeText(context, "Kode QR untuk '$name' berhasil dibuat!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat kode QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to save image with custom white area and item name text label
    val saveQrToGallery = { bitmap: Bitmap, fileNameId: String, itemName: String ->
        try {
            // Card dimensions: 640x840 for premium look and spacing
            val cardWidth = 640
            val cardHeight = 840
            
            // Clean up any category brackets prefix like [Elektronik] to get pure item name
            val cleanName = if (itemName.startsWith("[") && itemName.contains("]")) {
                itemName.substringAfter("]").trim()
            } else {
                itemName
            }.trim()

            val combinedBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(combinedBitmap)
            
            // 1. Fill background with premium white
            val bgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), bgPaint)
            
            // 2. Draw modern Gradient Border (#3B82F6 to #2DD4BF)
            val gradient = android.graphics.LinearGradient(
                0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(),
                android.graphics.Color.parseColor("#3B82F6"),
                android.graphics.Color.parseColor("#2DD4BF"),
                android.graphics.Shader.TileMode.CLAMP
            )
            val borderPaint = android.graphics.Paint().apply {
                shader = gradient
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 14f
                isAntiAlias = true
            }
            val margin = 7f
            canvas.drawRoundRect(
                margin, margin, cardWidth.toFloat() - margin, cardHeight.toFloat() - margin,
                24f, 24f,
                borderPaint
            )

            // 3. Draw Four-Corner "Sudut Siku" Ornaments
            val cornerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#3B82F6")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 8f
                isAntiAlias = true
                strokeCap = android.graphics.Paint.Cap.ROUND
            }
            // Top-left
            canvas.drawLine(28f, 28f, 68f, 28f, cornerPaint)
            canvas.drawLine(28f, 28f, 28f, 68f, cornerPaint)
            // Top-right
            canvas.drawLine(cardWidth - 28f, 28f, cardWidth - 68f, 28f, cornerPaint)
            canvas.drawLine(cardWidth - 28f, 28f, cardWidth - 28f, 68f, cornerPaint)
            // Bottom-left
            canvas.drawLine(28f, cardHeight - 28f, 68f, cardHeight - 28f, cornerPaint)
            canvas.drawLine(28f, cardHeight - 28f, 28f, cardHeight - 68f, cornerPaint)
            // Bottom-right
            canvas.drawLine(cardWidth - 28f, cardHeight - 28f, cardWidth - 68f, cardHeight - 28f, cornerPaint)
            canvas.drawLine(cardWidth - 28f, cardHeight - 28f, cardWidth - 28f, cardHeight - 68f, cornerPaint)

            // 4. Draw Double Arrow ">>" and "<<" Ornaments on Left and Right Sides
            val arrowPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#2DD4BF")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            // Left margin double arrow pointing inwards (rightward)
            canvas.drawLine(20f, 320f, 32f, 336f, arrowPaint)
            canvas.drawLine(32f, 336f, 20f, 352f, arrowPaint)
            canvas.drawLine(32f, 320f, 44f, 336f, arrowPaint)
            canvas.drawLine(44f, 336f, 32f, 352f, arrowPaint)

            // Right margin double arrow pointing inwards (leftward)
            canvas.drawLine(620f, 320f, 608f, 336f, arrowPaint)
            canvas.drawLine(608f, 336f, 620f, 352f, arrowPaint)
            canvas.drawLine(608f, 320f, 596f, 336f, arrowPaint)
            canvas.drawLine(596f, 336f, 608f, 352f, arrowPaint)

            // 5. Draw QR code inner card container with soft grey border
            val qrLeft = (cardWidth - 512) / 2f
            val qrTop = 80f
            val qrCardBgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            val qrCardBorderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F1F5F9")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            val qrCardRect = android.graphics.RectF(qrLeft - 12f, qrTop - 12f, qrLeft + 512f + 12f, qrTop + 512f + 12f)
            canvas.drawRoundRect(qrCardRect, 16f, 16f, qrCardBgPaint)
            canvas.drawRoundRect(qrCardRect, 16f, 16f, qrCardBorderPaint)

            // 6. Draw original QR code at the calculated coordinates (centered horizontally, top-aligned)
            canvas.drawBitmap(bitmap, qrLeft, qrTop, null)
            
            // 7. Draw premium White Gading Ivory Container for Labels
            val containerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FCFAF7") // White Gading Ivory
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            val containerBorderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0") // Slate-200 border
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            val containerRect = android.graphics.RectF(64f, 630f, cardWidth - 64f, cardHeight - 80f)
            canvas.drawRoundRect(containerRect, 16f, 16f, containerPaint)
            canvas.drawRoundRect(containerRect, 16f, 16f, containerBorderPaint)

            // 8. Draw Item Name and ID text using elegant typography
            val namePaint = android.text.TextPaint().apply {
                color = android.graphics.Color.parseColor("#0F172A") // Slate-900
                textSize = 28f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            }
            val codePaint = android.text.TextPaint().apply {
                color = android.graphics.Color.parseColor("#64748B") // Slate-500
                textSize = 20f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
            }

            // Limit text width to fit inside the rounded container (with nice margins)
            val maxContainerTextWidth = cardWidth - 160 // 80px margin left & right
            
            val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.text.StaticLayout.Builder.obtain(cleanName, 0, cleanName.length, namePaint, maxContainerTextWidth)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.1f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.text.StaticLayout(
                    cleanName,
                    namePaint,
                    maxContainerTextWidth,
                    android.text.Layout.Alignment.ALIGN_CENTER,
                    1.1f,
                    0f,
                    false
                )
            }
            
            // Calculate dynamic vertical centering inside container
            val totalContentHeight = staticLayout.height + 36f
            val textStartY = 630f + ((130f - totalContentHeight) / 2f)
            
            canvas.save()
            canvas.translate(80f, textStartY)
            staticLayout.draw(canvas)
            canvas.restore()

            // Draw ID code centered below wrapped name
            val codeText = "Kode ID: $fileNameId"
            val codeWidth = codePaint.measureText(codeText)
            val codeX = (cardWidth - codeWidth) / 2f
            val codeY = textStartY + staticLayout.height + 28f
            canvas.drawText(codeText, codeX, codeY, codePaint)

            // Save the resulting high-res combined image
            val filename = "QR_${fileNameId.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            val fos: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GudangSMANSA")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val image = java.io.File(imagesDir, filename)
                fos = java.io.FileOutputStream(image)
            }

            if (fos != null) {
                combinedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                Toast.makeText(context, "Gambar QR Code berhasil disimpan ke Galeri HP!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Gagal membuka jalur penyimpanan", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal menyimpan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Option Selector Mode: Database Select vs Input Manual QR
        LunarisCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { isManualRegistrationMode = false; generatedBitmap = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isManualRegistrationMode) Color(0xFF4F46E5) else Color.Transparent,
                        contentColor = if (!isManualRegistrationMode) Color.White else Color.DarkGray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pilih Barang", fontSize = 12.sp)
                }

                Button(
                    onClick = { isManualRegistrationMode = true; generatedBitmap = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isManualRegistrationMode) Color(0xFF4F46E5) else Color.Transparent,
                        contentColor = if (isManualRegistrationMode) Color.White else Color.DarkGray
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Input QR Manual", fontSize = 12.sp)
                }
            }
        }

        if (!isManualRegistrationMode) {
            // Mode Select from Registered DB
            LunarisCard(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pilih Barang dari Sistem", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                        // Selection dropdown wrapper
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LunarisTextField(
                                value = selectedItem?.let { "[${it.idBarang}] ${it.namaBarang}" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Klik untuk memilih barang...") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (expandedDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih",
                                        modifier = Modifier.clickable { expandedDropdown = !expandedDropdown }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedDropdown = !expandedDropdown }
                                    .testTag("dropdown_barang")
                            )

                            // Actual dropdown menu with search bar
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(300.dp)
                            ) {
                                LunarisTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Cari Nama Barang") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Cari") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )

                                val filteredItems = items.filter {
                                    it.namaBarang.contains(searchQuery, ignoreCase = true) ||
                                            it.idBarang.contains(searchQuery, ignoreCase = true)
                                }

                                if (filteredItems.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Tidak ada barang cocok") },
                                        onClick = {}
                                    )
                                } else {
                                    filteredItems.forEach { item ->
                                        val isGenerated = generatedSet.contains(item.idBarang)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("[${item.idBarang}] ${item.namaBarang}", fontSize = 13.sp)
                                                    if (isGenerated) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Dibuat",
                                                            tint = Color(0xFF15803D),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedItem = item
                                                expandedDropdown = false
                                                generatedBitmap = null
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Checkmark status indicator under dropdown
                        if (selectedItem != null) {
                            val alreadyHasQr = generatedSet.contains(selectedItem!!.idBarang)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (alreadyHasQr) Color(0xFFDCFCE7) else Color(0xFFEFF6FF),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (alreadyHasQr) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = "Status",
                                    tint = if (alreadyHasQr) Color(0xFF15803D) else Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (alreadyHasQr) "Kode QR sudah pernah dibuat" else "Kode QR belum pernah dibuat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alreadyHasQr) Color(0xFF15803D) else Color(0xFF2563EB)
                                )
                            }
                        }

                        // Generate Button
                        Button(
                            onClick = {
                                if (selectedItem == null) {
                                    Toast.makeText(context, "Silakan pilih barang terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val alreadyGenerated = generatedSet.contains(selectedItem!!.idBarang)
                                if (alreadyGenerated) {
                                    showRegenerateWarning = true
                                } else {
                                    triggerQrCodeGeneration(selectedItem!!.idBarang, selectedItem!!.namaBarang)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_generate_qr")
                        ) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = "QR")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate QR Code", fontWeight = FontWeight.Bold)
                        }
                    }
                }
        } else {
            // Mode C: Input QR Manual (Form input barang baru)
            LunarisCard(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pendaftaran Barang Baru via QR", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                        // Input Nama
                        LunarisTextField(
                            value = manualNama,
                            onValueChange = { manualNama = it },
                            label = { Text("Nama Barang") },
                            placeholder = { Text("Contoh: Laptop Asus Vivo") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_nama_barang")
                        )

                        // Input Kategori Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LunarisTextField(
                                value = manualKategori,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kategori") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (expandedKategoriDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih",
                                        modifier = Modifier.clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedKategoriDropdown = !expandedKategoriDropdown }
                            )
                            DropdownMenu(
                                expanded = expandedKategoriDropdown,
                                onDismissRequest = { expandedKategoriDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            manualKategori = cat
                                            expandedKategoriDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Input Satuan Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LunarisTextField(
                                value = manualSatuan,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Satuan") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (expandedSatuanDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih",
                                        modifier = Modifier.clickable { expandedSatuanDropdown = !expandedSatuanDropdown }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedSatuanDropdown = !expandedSatuanDropdown }
                            )
                            DropdownMenu(
                                expanded = expandedSatuanDropdown,
                                onDismissRequest = { expandedSatuanDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                units.forEach { sat ->
                                    DropdownMenuItem(
                                        text = { Text(sat) },
                                        onClick = {
                                            manualSatuan = sat
                                            expandedSatuanDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Input Stok Awal
                        LunarisTextField(
                            value = manualStok,
                            onValueChange = { manualStok = it },
                            label = { Text("Stok Awal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_stok_awal")
                        )

                        // Submit Button which registers on DB + generates QR
                        Button(
                            onClick = {
                                val stockInt = manualStok.toIntOrNull()
                                if (manualNama.isBlank()) {
                                    Toast.makeText(context, "Nama barang tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (stockInt == null || stockInt < 0) {
                                    Toast.makeText(context, "Stok awal harus angka positif!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.registerNewItem(
                                    name = manualNama.trim(),
                                    stokAwal = stockInt,
                                    kategori = manualKategori,
                                    satuan = manualSatuan,
                                    onSuccess = {
                                        // Find the item with that exact name to get the newly generated ID
                                        val newItem = viewModel.itemsWithStock.value.find { 
                                            it.namaBarang == manualNama.trim() && it.kategori == manualKategori && it.satuan == manualSatuan
                                        }
                                        val newId = newItem?.idBarang ?: "BRG-NEW"
                                        triggerQrCodeGeneration(newId, manualNama.trim())

                                        // Clear form inputs
                                        manualNama = ""
                                        manualStok = "1"
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, "Pendaftaran Gagal: $error", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)), // Green-700
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_save_and_generate_qr")
                        ) {
                            Icon(imageVector = Icons.Default.AppRegistration, contentDescription = "Daftar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daftar Baru & Buat QR", fontWeight = FontWeight.Bold)
                        }
                    }
                }
        }

        // Output Display Box of generated QR Code
        if (generatedBitmap != null) {
            LunarisCard(
                shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "KODE QR DI-GENERATE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4F46E5),
                            letterSpacing = 1.sp
                        )

                        // QR Image
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Image(
                                bitmap = generatedBitmap!!.asImageBitmap(),
                                contentDescription = "Hasil QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Labels showing code
                        val nameToShow = if (isManualRegistrationMode) {
                            generatedItemName.ifBlank { "Barang Baru" }
                        } else {
                            selectedItem?.namaBarang ?: ""
                        }
                        val idToShow = if (isManualRegistrationMode) {
                            generatedItemId.ifBlank { "Aset Baru" }
                        } else {
                            selectedItem?.idBarang ?: ""
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = nameToShow,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kode ID: $idToShow",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Save Button
                        Button(
                            onClick = { saveQrToGallery(generatedBitmap!!, idToShow, nameToShow) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_simpan_qr_galeri")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan ke Galeri", fontWeight = FontWeight.Bold)
                        }
                    }
                }
        }
    }

    // Dialog Warnings on Regenerating QR
    if (showRegenerateWarning && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showRegenerateWarning = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Peringatan", tint = Color(0xFFD69E2E))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Peringatan Regenerasi", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Kode QR untuk barang ini sudah ada. Apakah Anda yakin ingin membuat ulang?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateWarning = false
                        triggerQrCodeGeneration(selectedItem!!.idBarang, selectedItem!!.namaBarang)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD69E2E))
                ) {
                    Text("Ya, Buat Ulang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateWarning = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// SharedPreferences extensions inside settings to help tracking
fun com.example.data.repository.SettingsRepository.getGeneratedQrCodes(): Set<String> {
    val prefs = getPrivatePreferences()
    return prefs.getStringSet("generated_qr_codes", emptySet()) ?: emptySet()
}

fun com.example.data.repository.SettingsRepository.markQrCodeGenerated(idBarang: String) {
    val prefs = getPrivatePreferences()
    val current = getGeneratedQrCodes().toMutableSet()
    current.add(idBarang)
    prefs.edit().putStringSet("generated_qr_codes", current).apply()
}

// Reflection helper to access private prefs without modifying SettingsRepository signature
private fun com.example.data.repository.SettingsRepository.getPrivatePreferences(): android.content.SharedPreferences {
    val field = this::class.java.getDeclaredField("prefs")
    field.isAccessible = true
    return field.get(this) as android.content.SharedPreferences
}
