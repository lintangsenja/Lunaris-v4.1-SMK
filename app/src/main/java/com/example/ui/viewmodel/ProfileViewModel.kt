package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.ProfileEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).inventoryDao()
    private val settingsRepository = com.example.data.repository.SettingsRepository(application)

    // Reactively stream the profile data from database
    val profile: StateFlow<ProfileEntity?> = dao.getProfileFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Pre-populate database Profile table with settings from SharedPreferences if empty
        viewModelScope.launch {
            val existing = dao.getProfile()
            if (existing == null) {
                val defaultName = settingsRepository.getDefaultOfficer().ifBlank { "Administrator" }
                val defaultNip = settingsRepository.getOfficerNip().ifBlank { "-" }
                val defaultInstansi = settingsRepository.getInstansiName().ifBlank { "Gudang Utama Lunaris" }
                val defaultLogo = settingsRepository.getInstansiLogoPath()

                val initialProfile = ProfileEntity(
                    id = 1,
                    namaPetugas = defaultName,
                    nip = defaultNip,
                    namaInstansi = defaultInstansi,
                    fotoUri = defaultLogo
                )
                dao.insertProfile(initialProfile)
            }
        }
    }

    // Save profile data
    fun saveProfile(namaPetugas: String, nip: String, namaInstansi: String, fotoUri: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val updated = ProfileEntity(
                id = 1,
                namaPetugas = namaPetugas,
                nip = nip,
                namaInstansi = namaInstansi,
                fotoUri = fotoUri
            )
            dao.insertProfile(updated)
            
            // Sync with existing SharedPreferences so other parts of the app also get updated immediately
            settingsRepository.setDefaultOfficer(namaPetugas)
            settingsRepository.setOfficerNip(nip)
            settingsRepository.setInstansiName(namaInstansi)
            if (fotoUri.isNotEmpty()) {
                settingsRepository.setUserProfilePhoto(fotoUri)
            }
            
            onComplete()
        }
    }
}
