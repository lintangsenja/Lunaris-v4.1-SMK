package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.DynamicLogo
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LunarisCard
import com.example.ui.theme.pastelGradientBackground
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.ProfileViewModel
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = false

    val userRole by viewModel.userRole.collectAsState()
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val instansiLogoPath by viewModel.instansiLogoPath.collectAsState()
    val userProfilePhoto by viewModel.userProfilePhoto.collectAsState()

    var selectedLogoUriForCrop by remember { mutableStateOf<Uri?>(null) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // fallback
            }
            selectedLogoUriForCrop = uri
        }
    }

    if (selectedLogoUriForCrop != null) {
        ImageCropDialog(
            imageUri = selectedLogoUriForCrop!!,
            title = "Sesuaikan Logo Instansi",
            onDismiss = { selectedLogoUriForCrop = null },
            onCropSuccess = { croppedUri ->
                val logoPath = croppedUri.toString()
                viewModel.updateInstansiLogoPath(logoPath)
                selectedLogoUriForCrop = null
                Toast.makeText(context, "Logo instansi berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // User entity state from InventoryViewModel
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUserEntity = remember(allUsers, loggedInUser) {
        allUsers.find { it.username.equals(loggedInUser, ignoreCase = true) }
    }

    // Password change states
    var currentPassInput by remember { mutableStateOf("") }
    var newPassInput by remember { mutableStateOf("") }
    var confirmPassInput by remember { mutableStateOf("") }
    var isCurrentPassVisible by remember { mutableStateOf(false) }
    var isNewPassVisible by remember { mutableStateOf(false) }
    var isConfirmPassVisible by remember { mutableStateOf(false) }

    // State from ProfileViewModel
    val profileData by profileViewModel.profile.collectAsState()

    // Form states
    var namaPetugasInput by remember { mutableStateOf("") }
    var nipInput by remember { mutableStateOf("") }
    var namaInstansiInput by remember { mutableStateOf("") }
    var fotoUriState by remember { mutableStateOf("") }

    // When currentUserEntity or profileData loads, initialize inputs
    LaunchedEffect(currentUserEntity, profileData, userProfilePhoto) {
        if (currentUserEntity != null && currentUserEntity.fullName.isNotBlank()) {
            namaPetugasInput = currentUserEntity.fullName
        } else {
            profileData?.let {
                if (namaPetugasInput.isBlank()) namaPetugasInput = it.namaPetugas
            }
        }
        profileData?.let {
            if (nipInput.isBlank()) nipInput = it.nip
            if (namaInstansiInput.isBlank()) namaInstansiInput = it.namaInstansi
        }
        if (fotoUriState.isBlank()) {
            fotoUriState = if (userProfilePhoto.isNotBlank()) userProfilePhoto else (profileData?.fotoUri ?: "")
        }
    }

    var selectedImageUriForCrop by remember { mutableStateOf<Uri?>(null) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // fallback if taking permission fails on some URIs
            }
            selectedImageUriForCrop = uri
        }
    }

    if (selectedImageUriForCrop != null) {
        ImageCropDialog(
            imageUri = selectedImageUriForCrop!!,
            onDismiss = { selectedImageUriForCrop = null },
            onCropSuccess = { croppedUri ->
                fotoUriState = croppedUri.toString()
                selectedImageUriForCrop = null
            }
        )
    }

    BackHandler {
        onNavigateBack()
    }

    // Decode Bitmap from photoUriState
    val avatarBitmap = remember(fotoUriState) {
        if (fotoUriState.isNotEmpty()) {
            try {
                val uri = Uri.parse(fotoUriState)
                val inputStream = context.contentResolver.openInputStream(uri)
                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            } catch (e: Exception) {
                // If it's a file path instead of a content URI
                try {
                    val file = java.io.File(fotoUriState)
                    if (file.exists()) {
                        android.graphics.BitmapFactory.decodeFile(fotoUriState)?.asImageBitmap()
                    } else {
                        null
                    }
                } catch (ex: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.9f),
                                    Color(0xFF3B82F6).copy(alpha = 0.9f)
                                )
                            )
                        )
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(48.dp)
                                    .testTag("back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Profil Petugas & Instansi",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .pastelGradientBackground(isDark = isDark)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Avatar Section
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .then(
                        if (userRole == "admin") {
                            Modifier.clickable { imagePickerLauncher.launch("image/*") }
                        } else {
                            Modifier
                        }
                    )
                    .testTag("pick_avatar_button"),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .background(Color.White)
                    ) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = "Foto Profil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .background(Color(0xFFEEF2F6))
                    ) {
                        Text(
                            text = if (namaPetugasInput.isNotEmpty()) namaPetugasInput.take(1).uppercase() else "P",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8B5CF6)
                        )
                    }
                }

                if (userRole == "admin") {
                    // Small edit badge/camera icon overlay
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.BottomEnd)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6))
                            .border(1.5.dp, Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Ubah Foto",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (userRole == "admin") {
                Text(
                    text = "Ketuk foto di atas untuk mengubah foto profil",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (userRole == "siswa") {
                LunarisCard(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Informasi Profil Siswa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5B21B6)
                        )

                        OutlinedTextField(
                            value = loggedInUser,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Username") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Username Icon",
                                    tint = Color(0xFF8B5CF6)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = namaPetugasInput,
                            onValueChange = { namaPetugasInput = it },
                            label = { Text("Nama Lengkap Siswa") },
                            placeholder = { Text("Masukkan nama lengkap Anda") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Nama Icon",
                                    tint = Color(0xFF8B5CF6)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF8B5CF6),
                                focusedLabelColor = Color(0xFF8B5CF6)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_nama_siswa")
                        )

                        Button(
                            onClick = {
                                if (namaPetugasInput.isBlank()) {
                                    Toast.makeText(context, "Nama lengkap tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.updateUserProfileData(loggedInUser, namaPetugasInput) { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_save_siswa_profile")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan Nama Lengkap", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            if (userRole == "admin") {
                // Form inputs Card
                LunarisCard(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Informasi Profil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B21B6)
                    )

                    // Nama Petugas
                    OutlinedTextField(
                        value = namaPetugasInput,
                        onValueChange = { namaPetugasInput = it },
                        label = { Text("Nama Lengkap Petugas") },
                        placeholder = { Text("Nama Petugas") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Nama Petugas Icon",
                                tint = Color(0xFF8B5CF6)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            focusedLabelColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_nama_petugas")
                    )

                    // NIP
                    OutlinedTextField(
                        value = nipInput,
                        onValueChange = { nipInput = it },
                        label = { Text("NIP Petugas") },
                        placeholder = { Text("NIP Petugas") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CardMembership,
                                contentDescription = "NIP Icon",
                                tint = Color(0xFF8B5CF6)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            focusedLabelColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_nip_petugas")
                    )

                    // Nama Instansi
                    OutlinedTextField(
                        value = namaInstansiInput,
                        onValueChange = { namaInstansiInput = it },
                        label = { Text("Nama Instansi") },
                        placeholder = { Text("Nama Instansi") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Instansi Icon",
                                tint = Color(0xFF8B5CF6)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            focusedLabelColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_nama_instansi")
                    )
                }
            }
            }

            if (userRole == "admin") {
                Spacer(modifier = Modifier.height(8.dp))
                LunarisCard(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Logo Dinamis Instansi (Khusus Admin)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5B21B6),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(110.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                                .background(Color(0xFFEEF2F6))
                        ) {
                            DynamicLogo(
                                logoPath = instansiLogoPath,
                                modifier = Modifier.fillMaxSize(),
                                defaultIconTint = Color(0xFF8B5CF6),
                                contentDescription = "Logo Instansi"
                            )
                        }

                        Text(
                            text = "Logo ini akan ditampilkan secara real-time di halaman Login dan Dashboard.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                logoPickerLauncher.launch("image/*")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_upload_logo")
                        ) {
                            Text(
                                text = "Unggah Logo Baru",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (userRole == "admin") {
                Spacer(modifier = Modifier.height(8.dp))

                // Save changes button
                Button(
                    onClick = {
                        if (namaPetugasInput.isBlank()) {
                            Toast.makeText(context, "Nama Petugas tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (fotoUriState.isNotBlank()) {
                            viewModel.updateUserProfilePhoto(fotoUriState)
                        }
                        viewModel.updateUserProfileData(loggedInUser, namaPetugasInput) { _, _ -> }
                        profileViewModel.saveProfile(
                            namaPetugas = namaPetugasInput,
                            nip = nipInput,
                            namaInstansi = namaInstansiInput,
                            fotoUri = fotoUriState
                        ) {
                            Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_profile")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Simpan",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simpan Perubahan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Change Password Card for ALL users
            LunarisCard(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE9D5FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Ganti Password",
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ganti Kata Sandi (Change Password)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5B21B6)
                        )
                    }

                    Text(
                        text = "Perbarui kata sandi Anda secara berkala untuk menjaga keamanan akun.",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )

                    // Current Password
                    OutlinedTextField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it },
                        label = { Text("Password Saat Ini *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B5CF6))
                        },
                        trailingIcon = {
                            IconButton(onClick = { isCurrentPassVisible = !isCurrentPassVisible }) {
                                Icon(
                                    imageVector = if (isCurrentPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Current Password"
                                )
                            }
                        },
                        visualTransformation = if (isCurrentPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            focusedLabelColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_current_password")
                    )

                    // New Password
                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        label = { Text("Password Baru (min. 4 karakter) *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B5CF6))
                        },
                        trailingIcon = {
                            IconButton(onClick = { isNewPassVisible = !isNewPassVisible }) {
                                Icon(
                                    imageVector = if (isNewPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle New Password"
                                )
                            }
                        },
                        visualTransformation = if (isNewPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            focusedLabelColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_password_change")
                    )

                    // Confirm New Password
                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it },
                        label = { Text("Konfirmasi Password Baru *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B5CF6))
                        },
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPassVisible = !isConfirmPassVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Confirm Password"
                                )
                            }
                        },
                        visualTransformation = if (isConfirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6),
                            focusedLabelColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_password_change")
                    )

                    Button(
                        onClick = {
                            viewModel.changeUserPassword(
                                usernameInput = loggedInUser,
                                currentPassInput = currentPassInput,
                                newPassInput = newPassInput,
                                confirmPassInput = confirmPassInput
                            ) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    currentPassInput = ""
                                    newPassInput = ""
                                    confirmPassInput = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_change_password")
                    ) {
                        Text(
                            text = "Perbarui Password",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout Button
            Button(
                onClick = {
                    viewModel.logout()
                    Toast.makeText(context, "Berhasil keluar dari akun!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_logout")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Keluar",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Keluar / Logout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ImageCropDialog(
    imageUri: Uri,
    title: String = "Sesuaikan Foto Profil",
    onDismiss: () -> Unit,
    onCropSuccess: (Uri) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val viewportDp = 300.dp
    val cropDp = 220.dp
    val viewportPx = with(density) { viewportDp.toPx() }
    val cropPx = with(density) { cropDp.toPx() }
    val cropLeft = (viewportPx - cropPx) / 2f
    val cropTop = (viewportPx - cropPx) / 2f

    val originalBitmap = remember(imageUri) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            android.graphics.BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    val baseScale = remember(originalBitmap) {
        if (originalBitmap != null) {
            val scaleX = viewportPx / originalBitmap.width.toFloat()
            val scaleY = viewportPx / originalBitmap.height.toFloat()
            // Fit-to-View: use minOf for Fit/Inside, and apply 0.85x factor to provide comfortable padding
            minOf(scaleX, scaleY) * 0.85f
        } else {
            1f
        }
    }

    var userScale by remember { mutableStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Gunakan dua jari untuk memperbesar, dan geser untuk menyesuaikan area di dalam lingkaran.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp, start = 12.dp, end = 12.dp)
                )

                if (originalBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(viewportDp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    userScale = (userScale * zoom).coerceIn(0.5f, 5.0f)
                                    userOffset = userOffset + pan
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = originalBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = userScale,
                                    scaleY = userScale,
                                    translationX = userOffset.x,
                                    translationY = userOffset.y
                                ),
                            contentScale = ContentScale.Fit
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val circleRadius = cropPx / 2f

                            val path = Path().apply {
                                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                                addOval(
                                    Rect(
                                        canvasWidth / 2f - circleRadius,
                                        canvasHeight / 2f - circleRadius,
                                        canvasWidth / 2f + circleRadius,
                                        canvasHeight / 2f + circleRadius
                                    )
                                )
                                fillType = PathFillType.EvenOdd
                            }
                            drawPath(path = path, color = Color.Black.copy(alpha = 0.65f))

                            drawCircle(
                                color = Color.White,
                                radius = circleRadius,
                                center = Offset(canvasWidth / 2f, canvasHeight / 2f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(viewportDp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8B5CF6))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (originalBitmap != null) {
                                try {
                                    val croppedBitmap = Bitmap.createBitmap(
                                        cropPx.toInt(),
                                        cropPx.toInt(),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = android.graphics.Canvas(croppedBitmap)
                                    val matrix = android.graphics.Matrix()

                                    // Center the bitmap inside the viewport coordinates
                                    matrix.postTranslate(-originalBitmap.width / 2f, -originalBitmap.height / 2f)

                                    // Apply scale and user-defined offset
                                    val totalScale = baseScale * userScale
                                    matrix.postScale(totalScale, totalScale)
                                    matrix.postTranslate(viewportPx / 2f + userOffset.x, viewportPx / 2f + userOffset.y)

                                    // Translate to crop origin coordinates
                                    matrix.postTranslate(-cropLeft, -cropTop)

                                    val paint = Paint().apply {
                                        isAntiAlias = true
                                        isFilterBitmap = true
                                    }
                                    canvas.drawBitmap(originalBitmap, matrix, paint)

                                    val file = java.io.File(
                                        context.cacheDir,
                                        "cropped_profile_${System.currentTimeMillis()}.jpg"
                                    )
                                    val out = java.io.FileOutputStream(file)
                                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                    out.flush()
                                    out.close()

                                    onCropSuccess(Uri.fromFile(file))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Gagal memotong gambar!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Text("Potong & Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
