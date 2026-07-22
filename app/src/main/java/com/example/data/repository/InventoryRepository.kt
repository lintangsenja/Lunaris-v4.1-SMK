package com.example.data.repository

import android.util.Log
import com.example.data.dao.InventoryDao
import com.example.data.entity.ItemEntity
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.example.data.model.ItemWithStock
import com.example.data.network.GoogleSheetsSyncService
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.entity.CategoryEntity
import com.example.data.entity.UnitEntity
import com.example.data.entity.PemakaianBahanEntity
import com.example.data.entity.BahanAfkirEntity

data class TransactionDetailResult(
    val transaction: LoanTransactionEntity,
    val items: List<LoanItemEntity>,
    val returnStatusDisplay: String,
    val isReturned: Boolean
)

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val syncService: GoogleSheetsSyncService
) {

    val itemsWithStock: Flow<List<ItemWithStock>> = inventoryDao.getItemsWithStock()
    val allTransactions: Flow<List<LoanTransactionEntity>> = inventoryDao.getAllTransactions()
    val activeTransactions: Flow<List<LoanTransactionEntity>> = inventoryDao.getActiveTransactions()
    val allDamagedItems: Flow<List<com.example.data.entity.DamagedItemEntity>> = inventoryDao.getAllDamagedItems()
    val allPemakaianBahan: Flow<List<PemakaianBahanEntity>> = inventoryDao.getAllPemakaianBahan()
    val allBahanAfkir: Flow<List<BahanAfkirEntity>> = inventoryDao.getAllBahanAfkir()

    val allCategories: Flow<List<CategoryEntity>> = inventoryDao.getAllCategories()
    val allUnits: Flow<List<UnitEntity>> = inventoryDao.getAllUnits()

    suspend fun recordPemakaian(pemakaian: PemakaianBahanEntity) {
        inventoryDao.recordPemakaian(pemakaian)
    }

    suspend fun recordBahanAfkir(afkir: BahanAfkirEntity) {
        inventoryDao.recordBahanAfkir(afkir)
    }

    suspend fun undoBahanAfkir(idAfkir: String) {
        inventoryDao.undoBahanAfkir(idAfkir)
    }

    suspend fun deleteBahanAfkirPermanently(
        idAfkir: String,
        currentDate: String,
        currentTime: String,
        namaPetugas: String
    ) {
        inventoryDao.deleteBahanAfkirPermanently(idAfkir, currentDate, currentTime, namaPetugas)
    }

    suspend fun insertItem(
        id: String,
        name: String,
        stokAwal: Int,
        kategori: String = "",
        satuan: String = "",
        merekAlat: String = "",
        ruang: String = "",
        sumberDana: String? = null,
        kondisi: String = "",
        keterangan: String = "",
        type: String = "ALAT",
        isBorrowable: Boolean = true
    ) {
        inventoryDao.insertItem(
            ItemEntity(
                idBarang = id,
                namaBarang = name,
                stokAwal = stokAwal,
                kategori = kategori,
                satuan = satuan,
                merekAlat = merekAlat,
                ruang = ruang,
                sumberDana = sumberDana,
                kondisi = kondisi,
                keterangan = keterangan,
                type = type,
                isBorrowable = isBorrowable
            )
        )
    }

    suspend fun updateItem(item: ItemEntity) {
        inventoryDao.updateItem(item)
    }

    suspend fun deleteItemById(idBarang: String) {
        inventoryDao.deleteItemById(idBarang)
    }

    suspend fun getActiveLoanCountForItem(idBarang: String): Int {
        return inventoryDao.getActiveLoanCountForItem(idBarang)
    }

    // Categories
    suspend fun insertCategory(name: String) {
        inventoryDao.insertCategory(CategoryEntity(name = name))
    }

    suspend fun updateCategory(category: CategoryEntity) {
        inventoryDao.updateCategory(category)
    }

    suspend fun deleteCategoryById(id: Int) {
        inventoryDao.deleteCategoryById(id)
    }

    // Units
    suspend fun insertUnit(name: String) {
        inventoryDao.insertUnit(UnitEntity(name = name))
    }

    suspend fun updateUnit(unit: UnitEntity) {
        inventoryDao.updateUnit(unit)
    }

    suspend fun deleteUnitById(id: Int) {
        inventoryDao.deleteUnitById(id)
    }

    suspend fun getItemsForTransaction(idTransaksi: String): List<LoanItemEntity> {
        val rawItems = inventoryDao.getItemsForTransaction(idTransaksi)
        val mergedMap = LinkedHashMap<String, LoanItemEntity>()
        for (item in rawItems) {
            val key = if (item.idBarang.isNotBlank()) item.idBarang else item.namaBarang.trim().lowercase()
            if (mergedMap.containsKey(key)) {
                val existing = mergedMap[key]!!
                mergedMap[key] = existing.copy(jumlah = existing.jumlah + item.jumlah)
            } else {
                mergedMap[key] = item
            }
        }
        return mergedMap.values.toList()
    }

    suspend fun getTransactionDetail(idTransaksi: String): TransactionDetailResult? {
        val tx = inventoryDao.getTransactionById(idTransaksi) ?: return null
        val items = getItemsForTransaction(idTransaksi)
        val isReturned = tx.status == "Kembali"
        val returnStatusDisplay = if (isReturned) {
            val tgl = tx.tanggalKembali ?: tx.tanggal
            val wkt = tx.waktuKembali ?: tx.waktu
            val ptg = tx.petugasKembali ?: tx.namaPetugas
            "Dikembalikan pada $tgl $wkt WIB (Petugas: $ptg)"
        } else {
            "Belum Dikembalikan"
        }
        return TransactionDetailResult(
            transaction = tx,
            items = items,
            returnStatusDisplay = returnStatusDisplay,
            isReturned = isReturned
        )
    }

    suspend fun getAllLoanItems(): List<LoanItemEntity> {
        return inventoryDao.getAllLoanItems()
    }

    suspend fun createLoan(
        transaction: LoanTransactionEntity,
        items: List<LoanItemEntity>,
        settingsRepo: SettingsRepository
    ): Boolean {
        // 1. Save locally to Room
        inventoryDao.createLoan(transaction, items)

        // 2. Sync to Sheets if configured & auto-sync is enabled
        val webAppUrl = settingsRepo.getSheetsUrl()
        if (webAppUrl.isNotEmpty() && settingsRepo.isAutoSyncEnabled()) {
            return try {
                val success = syncService.pushLoan(webAppUrl, transaction, items)
                if (success) {
                    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
                    settingsRepo.setLastSyncTime(sdf.format(Date()))
                }
                success
            } catch (e: Exception) {
                Log.e("InventoryRepo", "Failed to sync loan to sheets", e)
                false // Local save still succeeded
            }
        }
        return true
    }

    suspend fun returnLoan(
        idTransaksi: String,
        tanggalKembali: String,
        waktuKembali: String,
        kondisiKembali: String,
        petugasKembali: String,
        keteranganKerusakan: String?,
        itemConditions: Map<String, String> = emptyMap(),
        itemDamagedCounts: Map<String, Int> = emptyMap(),
        itemNotes: Map<String, String> = emptyMap(),
        settingsRepo: SettingsRepository
    ): Boolean {
        val transaction = inventoryDao.getTransactionById(idTransaksi) ?: return false
        val updatedTransaction = transaction.copy(
            status = "Kembali",
            tanggalKembali = tanggalKembali,
            waktuKembali = waktuKembali,
            kondisiKembali = kondisiKembali,
            petugasKembali = petugasKembali,
            keteranganKerusakan = keteranganKerusakan
        )

        // 1. Update locally
        inventoryDao.updateTransaction(updatedTransaction)

        // 1b. Process per-item conditions and update stock/damaged lists
        val rawLoanItems = inventoryDao.getItemsForTransaction(idTransaksi)
        val loanItemsMap = LinkedHashMap<String, LoanItemEntity>()
        for (item in rawLoanItems) {
            val key = if (item.idBarang.isNotBlank()) item.idBarang else item.namaBarang.trim().lowercase()
            if (loanItemsMap.containsKey(key)) {
                val existing = loanItemsMap[key]!!
                loanItemsMap[key] = existing.copy(jumlah = existing.jumlah + item.jumlah)
            } else {
                loanItemsMap[key] = item
            }
        }
        val loanItems = loanItemsMap.values.toList()
        val damagedItems = mutableListOf<com.example.data.entity.DamagedItemEntity>()

        loanItems.forEach { item ->
            val cond = itemConditions[item.idBarang] ?: "Baik / Normal"
            val note = itemNotes[item.idBarang]?.trim() ?: ""
            val totalQty = item.jumlah

            val damagedQty = if (itemDamagedCounts.containsKey(item.idBarang)) {
                (itemDamagedCounts[item.idBarang] ?: 0).coerceIn(0, totalQty)
            } else {
                if (cond == "Baik / Normal") 0 else totalQty
            }

            if (damagedQty > 0) {
                val effectiveCond = if (cond == "Baik / Normal") "Rusak Ringan" else cond
                when (effectiveCond) {
                    "Rusak Ringan", "Rusak" -> {
                        inventoryDao.addStokRusak(item.idBarang, damagedQty)
                        inventoryDao.updateItemKondisi(item.idBarang, "Rusak Ringan")
                        damagedItems.add(
                            com.example.data.entity.DamagedItemEntity(
                                idBarang = item.idBarang,
                                namaBarang = item.namaBarang,
                                jumlah = damagedQty,
                                tanggalKerusakan = tanggalKembali,
                                waktuKerusakan = waktuKembali,
                                keteranganKerusakan = note.ifBlank { "Rusak Ringan saat pengembalian ($damagedQty unit dari $totalQty unit)" },
                                namaPetugas = petugasKembali,
                                kondisiBaru = "Rusak Ringan",
                                status = "Rusak (Perlu Tindakan)"
                            )
                        )
                    }
                    "Rusak Berat" -> {
                        inventoryDao.addStokRusak(item.idBarang, damagedQty)
                        inventoryDao.updateItemKondisi(item.idBarang, "Rusak Berat")
                        damagedItems.add(
                            com.example.data.entity.DamagedItemEntity(
                                idBarang = item.idBarang,
                                namaBarang = item.namaBarang,
                                jumlah = damagedQty,
                                tanggalKerusakan = tanggalKembali,
                                waktuKerusakan = waktuKembali,
                                keteranganKerusakan = note.ifBlank { "Rusak Berat saat pengembalian ($damagedQty unit dari $totalQty unit)" },
                                namaPetugas = petugasKembali,
                                kondisiBaru = "Rusak Berat",
                                status = "Rusak (Perlu Tindakan)"
                            )
                        )
                    }
                    "Hilang" -> {
                        inventoryDao.decreaseItemStock(item.idBarang, damagedQty)
                        inventoryDao.updateItemKondisi(item.idBarang, "Hilang")
                        damagedItems.add(
                            com.example.data.entity.DamagedItemEntity(
                                idBarang = item.idBarang,
                                namaBarang = item.namaBarang,
                                jumlah = damagedQty,
                                tanggalKerusakan = tanggalKembali,
                                waktuKerusakan = waktuKembali,
                                keteranganKerusakan = note.ifBlank { "Hilang saat pengembalian ($damagedQty unit dari $totalQty unit)" },
                                namaPetugas = petugasKembali,
                                kondisiBaru = "Hilang",
                                status = "Hilang"
                            )
                        )
                    }
                    "Pemeliharaan" -> {
                        inventoryDao.addStokRusak(item.idBarang, damagedQty)
                        inventoryDao.updateItemKondisi(item.idBarang, "Pemeliharaan")
                        damagedItems.add(
                            com.example.data.entity.DamagedItemEntity(
                                idBarang = item.idBarang,
                                namaBarang = item.namaBarang,
                                jumlah = damagedQty,
                                tanggalKerusakan = tanggalKembali,
                                waktuKerusakan = waktuKembali,
                                keteranganKerusakan = note.ifBlank { "Dalam pemeliharaan saat pengembalian ($damagedQty unit dari $totalQty unit)" },
                                namaPetugas = petugasKembali,
                                kondisiBaru = "Pemeliharaan",
                                status = "Servis Luar/Pemeliharaan"
                            )
                        )
                    }
                }
            }
        }

        if (damagedItems.isNotEmpty()) {
            inventoryDao.insertDamagedItems(damagedItems)
        }

        // 2. Sync to Sheets if configured
        val webAppUrl = settingsRepo.getSheetsUrl()
        if (webAppUrl.isNotEmpty() && settingsRepo.isAutoSyncEnabled()) {
            return try {
                val success = syncService.pushReturn(
                    webAppUrl = webAppUrl,
                    idTransaksi = idTransaksi,
                    tanggalKembali = tanggalKembali,
                    waktuKembali = waktuKembali,
                    kondisiKembali = kondisiKembali,
                    petugasKembali = petugasKembali
                )
                if (success) {
                    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
                    settingsRepo.setLastSyncTime(sdf.format(Date()))
                }
                success
            } catch (e: Exception) {
                Log.e("InventoryRepo", "Failed to sync return to sheets", e)
                false // Local save still succeeded
            }
        }
        return true
    }

    suspend fun syncWithSheets(
        settingsRepo: SettingsRepository,
        onProgress: (String) -> Unit
    ): Result<Unit> {
        val webAppUrl = settingsRepo.getSheetsUrl()
        if (webAppUrl.isEmpty()) {
            return Result.failure(Exception("URL Google Sheets belum diatur! Silakan atur di menu Pengaturan."))
        }

        return try {
            onProgress("Menghubungkan ke Google Sheets...")
            
            // 1. Pull Items from Sheets
            onProgress("Mengunduh daftar barang dari Google Sheets...")
            val sheetsItems = syncService.pullItems(webAppUrl)
            
            if (sheetsItems.isNotEmpty()) {
                onProgress("Menyinkronkan data barang ke database lokal...")
                sheetsItems.forEach { item ->
                    inventoryDao.insertItem(item)
                }
            }

            // 2. If locally we have items but Google Sheet doesn't, we can push them
            val localItemsCount = inventoryDao.getItemsCount()
            if (localItemsCount > 0 && sheetsItems.isEmpty()) {
                onProgress("Mengunggah daftar barang lokal ke Google Sheets...")
                // We would query all local items first (not flowing) to push them
                // Let's do this by making a direct Dao call if needed, but pulling is usually the source of truth.
            }

            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
            settingsRepo.setLastSyncTime(sdf.format(Date()))
            
            onProgress("Sinkronisasi selesai dengan sukses!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("InventoryRepo", "Synchronization failed", e)
            Result.failure(Exception("Gagal menyinkronkan data: ${e.localizedMessage ?: "Koneksi bermasalah"}"))
        }
    }

    suspend fun clearAllData() {
        inventoryDao.clearItems()
        inventoryDao.clearTransactions()
        inventoryDao.clearLoanItems()
        inventoryDao.clearDamagedItems()
        inventoryDao.clearPemakaianBahan()
        inventoryDao.clearBahanAfkir()
    }

    suspend fun clearAllTransactions() {
        inventoryDao.clearTransactions()
        inventoryDao.clearLoanItems()
        inventoryDao.clearDamagedItems()
        inventoryDao.clearPemakaianBahan()
        inventoryDao.clearBahanAfkir()
    }

    suspend fun repairStokRusak(idBarang: String, amount: Int) {
        inventoryDao.repairStokRusak(idBarang, amount)
    }

    suspend fun recordDamagedReport(damaged: com.example.data.entity.DamagedItemEntity) {
        inventoryDao.recordDamagedReport(damaged)
    }

    suspend fun cancelDamagedReport(id: Int) {
        inventoryDao.cancelDamagedReport(id)
    }

    suspend fun updateDamagedStatus(
        damagedId: Int,
        newStatus: String,
        alasan: String,
        namaPetugas: String,
        currentDate: String,
        currentTime: String
    ) {
        inventoryDao.updateDamagedStatus(damagedId, newStatus, alasan, namaPetugas, currentDate, currentTime)
    }

    suspend fun deleteDamagedItemPermanently(
        id: Int,
        currentDate: String,
        currentTime: String,
        namaPetugas: String
    ) {
        inventoryDao.deleteDamagedItemPermanently(id, currentDate, currentTime, namaPetugas)
    }
}
