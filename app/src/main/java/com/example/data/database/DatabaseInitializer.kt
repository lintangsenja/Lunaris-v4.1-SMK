package com.example.data.database

import android.content.Context
import com.example.data.entity.UnitEntity
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DatabaseInitializer {

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Seed Units table if empty
                val db = AppDatabase.getDatabase(context)
                val count = db.inventoryDao().getUnitsCount()
                if (count == 0) {
                    val defaultUnits = listOf(
                        "Pcs", "Unit", "Set", "Box", "Pak", "Dus", "Lembar", "Rim", 
                        "Batang", "Bungkus", "Botol", "Kg", "Meter", "Roll", "Liter"
                    ).sorted()
                    for (unit in defaultUnits) {
                        db.inventoryDao().insertUnit(UnitEntity(name = unit))
                    }
                    android.util.Log.d("DatabaseInitializer", "Seeded units table successfully.")
                }

                // 2. Seed Sumber Dana if empty
                val settingsRepo = SettingsRepository(context)
                val currentSumberDana = settingsRepo.getSumberDana()
                if (currentSumberDana.isEmpty()) {
                    val defaultSumberDana = listOf(
                        "BOS", "BOP", "Bantuan Komite", "Bantuan Pempus", "Bantuan Pemrov", "Hibah"
                    ).sorted()
                    settingsRepo.saveSumberDana(defaultSumberDana)
                    android.util.Log.d("DatabaseInitializer", "Seeded sumber dana successfully.")
                }

                // 3. Seed Kondisi if empty
                val currentKondisi = settingsRepo.getKondisi()
                if (currentKondisi.isEmpty()) {
                    val defaultKondisi = listOf(
                        "Baik", "Rusak", "Pemeliharaan", "Expired"
                    ).sorted()
                    settingsRepo.saveKondisi(defaultKondisi)
                    android.util.Log.d("DatabaseInitializer", "Seeded kondisi successfully.")
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseInitializer", "Error initializing/seeding data", e)
            }
        }
    }
}
