package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.ItemEntity
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.example.data.model.ItemWithStock
import com.example.data.entity.DamagedItemEntity
import com.example.data.entity.PemakaianBahanEntity
import com.example.data.entity.BahanAfkirEntity
import com.example.data.entity.ProfileEntity
import com.example.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

import com.example.data.entity.CategoryEntity
import com.example.data.entity.UnitEntity

@Dao
interface InventoryDao {

    // Items
    @Query("SELECT * FROM items ORDER BY idBarang DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("""
        SELECT 
          i.idBarang, 
          i.namaBarang, 
          i.stokAwal,
          CASE 
            WHEN (i.stokAwal - COALESCE((
                SELECT SUM(sub.jumlah)
                FROM (
                  SELECT idTransaksi, idBarang, MAX(jumlah) AS jumlah
                  FROM loan_items
                  GROUP BY idTransaksi, idBarang
                ) sub
                INNER JOIN loan_transactions lt ON sub.idTransaksi = lt.idTransaksi
                WHERE sub.idBarang = i.idBarang AND lt.status = 'Dipinjam'
            ), 0) - i.stokRusak) < 0 THEN 0
            ELSE (i.stokAwal - COALESCE((
                SELECT SUM(sub.jumlah)
                FROM (
                  SELECT idTransaksi, idBarang, MAX(jumlah) AS jumlah
                  FROM loan_items
                  GROUP BY idTransaksi, idBarang
                ) sub
                INNER JOIN loan_transactions lt ON sub.idTransaksi = lt.idTransaksi
                WHERE sub.idBarang = i.idBarang AND lt.status = 'Dipinjam'
            ), 0) - i.stokRusak)
          END AS stokTersedia,
          i.kategori,
          i.satuan,
          i.stokRusak,
          i.merekAlat,
          i.ruang,
          i.sumberDana,
          i.kondisi,
          i.keterangan,
          i.isDemo,
          i.type,
          i.isBorrowable
        FROM items i
        ORDER BY i.namaBarang ASC
    """)
    fun getItemsWithStock(): Flow<List<ItemWithStock>>

    @Query("UPDATE items SET stokRusak = stokRusak + :amount WHERE idBarang = :idBarang")
    suspend fun addStokRusak(idBarang: String, amount: Int)

    @Query("UPDATE items SET stokRusak = CASE WHEN stokRusak - :amount < 0 THEN 0 ELSE stokRusak - :amount END WHERE idBarang = :idBarang")
    suspend fun repairStokRusak(idBarang: String, amount: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemsCount(): Int

    @Query("DELETE FROM items WHERE idBarang = :idBarang")
    suspend fun deleteItemById(idBarang: String)

    @Query("""
        SELECT COUNT(*) 
        FROM loan_items li 
        INNER JOIN loan_transactions lt ON li.idTransaksi = lt.idTransaksi
        WHERE li.idBarang = :idBarang AND lt.status = 'Dipinjam'
    """)
    suspend fun getActiveLoanCountForItem(idBarang: String): Int

    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Int)

    // Units
    @Query("SELECT * FROM units ORDER BY name ASC")
    fun getAllUnits(): Flow<List<UnitEntity>>

    @Query("SELECT COUNT(*) FROM units")
    suspend fun getUnitsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity)

    @Update
    suspend fun updateUnit(unit: UnitEntity)

    @Query("DELETE FROM units WHERE id = :id")
    suspend fun deleteUnitById(id: Int)

    // Loans
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LoanTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanItems(items: List<LoanItemEntity>)

    @Query("DELETE FROM loan_items WHERE idTransaksi = :idTransaksi")
    suspend fun deleteLoanItemsForTransaction(idTransaksi: String)

    @Query("DELETE FROM loan_items WHERE rowid NOT IN (SELECT MIN(rowid) FROM loan_items GROUP BY idTransaksi, idBarang)")
    suspend fun cleanupDuplicateLoanItems()

    @Update
    suspend fun updateTransaction(transaction: LoanTransactionEntity)

    @Transaction
    suspend fun createLoan(transaction: LoanTransactionEntity, items: List<LoanItemEntity>) {
        insertTransaction(transaction)
        deleteLoanItemsForTransaction(transaction.idTransaksi)
        insertLoanItems(items)
        cleanupDuplicateLoanItems()
    }

    @Query("SELECT * FROM loan_transactions ORDER BY tanggal DESC, waktu DESC")
    fun getAllTransactions(): Flow<List<LoanTransactionEntity>>

    @Query("SELECT * FROM loan_items WHERE idTransaksi = :idTransaksi")
    suspend fun getItemsForTransaction(idTransaksi: String): List<LoanItemEntity>

    @Query("SELECT * FROM loan_items")
    suspend fun getAllLoanItems(): List<LoanItemEntity>

    // Get active transactions
    @Query("SELECT * FROM loan_transactions WHERE status = 'Dipinjam' ORDER BY tanggal DESC")
    fun getActiveTransactions(): Flow<List<LoanTransactionEntity>>

    // Get specific transaction by id
    @Query("SELECT * FROM loan_transactions WHERE idTransaksi = :idTransaksi LIMIT 1")
    suspend fun getTransactionById(idTransaksi: String): LoanTransactionEntity?

    @Query("DELETE FROM items")
    suspend fun clearItems()

    @Query("DELETE FROM loan_transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM loan_items")
    suspend fun clearLoanItems()

    // Damaged Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDamagedItems(items: List<DamagedItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDamagedItem(item: DamagedItemEntity)

    @Update
    suspend fun updateDamagedItem(item: DamagedItemEntity)

    @Query("SELECT * FROM damaged_items ORDER BY tanggalKerusakan DESC, waktuKerusakan DESC")
    fun getAllDamagedItems(): Flow<List<DamagedItemEntity>>

    @Query("SELECT * FROM damaged_items WHERE id = :id LIMIT 1")
    suspend fun getDamagedItemById(id: Int): DamagedItemEntity?

    @Query("DELETE FROM damaged_items WHERE id = :id")
    suspend fun deleteDamagedItemById(id: Int)

    @Query("UPDATE items SET kondisi = :kondisi WHERE idBarang = :idBarang")
    suspend fun updateItemKondisi(idBarang: String, kondisi: String)

    @Transaction
    suspend fun recordDamagedReport(damaged: DamagedItemEntity) {
        insertDamagedItem(damaged)
        addStokRusak(damaged.idBarang, damaged.jumlah)
        updateItemKondisi(damaged.idBarang, damaged.kondisiBaru)
    }

    @Transaction
    suspend fun cancelDamagedReport(id: Int) {
        val record = getDamagedItemById(id)
        if (record != null) {
            deleteDamagedItemById(id)
            repairStokRusak(record.idBarang, record.jumlah)
            updateItemKondisi(record.idBarang, "Normal")
        }
    }

    @Transaction
    suspend fun updateDamagedStatus(
        damagedId: Int,
        newStatus: String,
        alasan: String,
        namaPetugas: String,
        currentDate: String,
        currentTime: String
    ) {
        val record = getDamagedItemById(damagedId) ?: return
        val oldStatus = record.status

        if (oldStatus == newStatus) {
            val updatedRecord = record.copy(
                statusKeterangan = alasan,
                namaPetugas = namaPetugas
            )
            updateDamagedItem(updatedRecord)
            return
        }

        val isOldReady = oldStatus == "Normal (Tersedia)"
        val isNewReady = newStatus == "Normal (Tersedia)"

        if (isOldReady && !isNewReady) {
            addStokRusak(record.idBarang, record.jumlah)
            updateItemKondisi(record.idBarang, "Rusak")
        } else if (!isOldReady && isNewReady) {
            repairStokRusak(record.idBarang, record.jumlah)
            updateItemKondisi(record.idBarang, "Normal")
        }

        val updatedRecord = record.copy(
            status = newStatus,
            statusKeterangan = alasan,
            namaPetugas = namaPetugas
        )
        updateDamagedItem(updatedRecord)

        val transactionId = "TX-AUD-" + System.currentTimeMillis()
        val auditTx = LoanTransactionEntity(
            idTransaksi = transactionId,
            tanggal = currentDate,
            waktu = currentTime,
            namaPeminjam = "Audit: ${record.namaBarang}",
            kelas = "Ubah Status",
            kondisi = newStatus,
            namaPetugas = namaPetugas,
            status = "Kembali",
            tanggalKembali = currentDate,
            waktuKembali = currentTime,
            kondisiKembali = newStatus,
            petugasKembali = namaPetugas,
            keteranganKerusakan = "Status diubah dari '$oldStatus' ke '$newStatus'. Catatan: $alasan"
        )
        insertTransaction(auditTx)

        val auditItem = LoanItemEntity(
            idTransaksi = transactionId,
            idBarang = record.idBarang,
            namaBarang = record.namaBarang,
            jumlah = record.jumlah
        )
        insertLoanItems(listOf(auditItem))
    }

    @Query("DELETE FROM damaged_items")
    suspend fun clearDamagedItems()

    // Pemakaian Bahan
    @Query("SELECT * FROM pemakaian_bahan ORDER BY tanggalPemakaian DESC, idPemakaian DESC")
    fun getAllPemakaianBahan(): Flow<List<PemakaianBahanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPemakaianBahan(pemakaian: PemakaianBahanEntity)

    @Query("UPDATE items SET stokAwal = CASE WHEN stokAwal - :amount < 0 THEN 0 ELSE stokAwal - :amount END WHERE idBarang = :idBarang")
    suspend fun decreaseItemStock(idBarang: String, amount: Int)

    @Transaction
    suspend fun recordPemakaian(pemakaian: PemakaianBahanEntity) {
        insertPemakaianBahan(pemakaian)
        decreaseItemStock(pemakaian.idBarang, pemakaian.jumlahDiambil)
    }

    @Query("DELETE FROM pemakaian_bahan")
    suspend fun clearPemakaianBahan()

    // Bahan Afkir
    @Query("SELECT * FROM bahan_afkir ORDER BY tanggalAfkir DESC, idAfkir DESC")
    fun getAllBahanAfkir(): Flow<List<BahanAfkirEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBahanAfkir(afkir: BahanAfkirEntity)

    @Query("DELETE FROM bahan_afkir WHERE idAfkir = :idAfkir")
    suspend fun deleteBahanAfkirOnly(idAfkir: String)

    @Query("SELECT * FROM bahan_afkir WHERE idAfkir = :idAfkir LIMIT 1")
    suspend fun getBahanAfkirById(idAfkir: String): BahanAfkirEntity?

    @Query("UPDATE items SET stokAwal = stokAwal + :amount WHERE idBarang = :idBarang")
    suspend fun increaseItemStock(idBarang: String, amount: Int)

    @Transaction
    suspend fun recordBahanAfkir(afkir: BahanAfkirEntity) {
        insertBahanAfkir(afkir)
        decreaseItemStock(afkir.idBarang, afkir.jumlahAfkir)
    }

    @Query("UPDATE bahan_afkir SET status = 'Dibatalkan' WHERE idAfkir = :idAfkir")
    suspend fun updateBahanAfkirStatus(idAfkir: String)

    @Transaction
    suspend fun undoBahanAfkir(idAfkir: String) {
        val record = getBahanAfkirById(idAfkir)
        if (record != null && record.status != "Dibatalkan") {
            updateBahanAfkirStatus(idAfkir)
            increaseItemStock(record.idBarang, record.jumlahAfkir)
        }
    }

    @Transaction
    suspend fun deleteBahanAfkirPermanently(
        idAfkir: String,
        currentDate: String,
        currentTime: String,
        namaPetugas: String
    ) {
        val record = getBahanAfkirById(idAfkir)
        if (record != null) {
            deleteBahanAfkirOnly(idAfkir)
            if (record.status != "Dibatalkan") {
                increaseItemStock(record.idBarang, record.jumlahAfkir)
            }
            
            val transactionId = "TX-AFK-DEL-" + System.currentTimeMillis()
            val auditTx = LoanTransactionEntity(
                idTransaksi = transactionId,
                tanggal = currentDate,
                waktu = currentTime,
                namaPeminjam = "Hapus Permanen: ${record.namaBarang}",
                kelas = "Audit Afkir",
                kondisi = "Permanen",
                namaPetugas = namaPetugas,
                status = "Kembali",
                tanggalKembali = currentDate,
                waktuKembali = currentTime,
                kondisiKembali = "Permanen",
                petugasKembali = namaPetugas,
                keteranganKerusakan = "Pencatatan afkir ${record.idAfkir} untuk ${record.namaBarang} (${record.jumlahAfkir} ${record.satuan}) dihapus secara permanen dari sistem."
            )
            insertTransaction(auditTx)

            val auditItem = LoanItemEntity(
                idTransaksi = transactionId,
                idBarang = record.idBarang,
                namaBarang = record.namaBarang,
                jumlah = record.jumlahAfkir
            )
            insertLoanItems(listOf(auditItem))
        }
    }

    @Transaction
    suspend fun deleteDamagedItemPermanently(
        id: Int,
        currentDate: String,
        currentTime: String,
        namaPetugas: String
    ) {
        val record = getDamagedItemById(id)
        if (record != null) {
            deleteDamagedItemById(id)
            decreaseItemStock(record.idBarang, record.jumlah)
            repairStokRusak(record.idBarang, record.jumlah)
            
            val transactionId = "TX-DMG-DEL-" + System.currentTimeMillis()
            val auditTx = LoanTransactionEntity(
                idTransaksi = transactionId,
                tanggal = currentDate,
                waktu = currentTime,
                namaPeminjam = "Hapus Permanen: ${record.namaBarang}",
                kelas = "Audit Rusak",
                kondisi = "Dihapus",
                namaPetugas = namaPetugas,
                status = "Kembali",
                tanggalKembali = currentDate,
                waktuKembali = currentTime,
                kondisiKembali = "Dihapus",
                petugasKembali = namaPetugas,
                keteranganKerusakan = "Penghapusan fisik aset permanen sebanyak ${record.jumlah} unit."
            )
            insertTransaction(auditTx)

            val auditItem = LoanItemEntity(
                idTransaksi = transactionId,
                idBarang = record.idBarang,
                namaBarang = record.namaBarang,
                jumlah = record.jumlah
            )
            insertLoanItems(listOf(auditItem))
        }
    }

    @Query("DELETE FROM bahan_afkir")
    suspend fun clearBahanAfkir()

    // Profile
    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    // Demo Data Support
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<LoanTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPemakaianBahanList(list: List<PemakaianBahanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBahanAfkirList(list: List<BahanAfkirEntity>)

    @Query("DELETE FROM categories WHERE isDemo = 1")
    suspend fun deleteDemoCategories()

    @Query("DELETE FROM units WHERE isDemo = 1")
    suspend fun deleteDemoUnits()

    @Query("DELETE FROM items WHERE isDemo = 1")
    suspend fun deleteDemoItems()

    @Query("DELETE FROM loan_transactions WHERE isDemo = 1")
    suspend fun deleteDemoTransactions()

    @Query("DELETE FROM loan_items WHERE isDemo = 1")
    suspend fun deleteDemoLoanItems()

    @Query("DELETE FROM damaged_items WHERE isDemo = 1")
    suspend fun deleteDemoDamagedItems()

    @Query("DELETE FROM pemakaian_bahan WHERE isDemo = 1")
    suspend fun deleteDemoPemakaianBahan()

    @Query("DELETE FROM bahan_afkir WHERE isDemo = 1")
    suspend fun deleteDemoBahanAfkir()

    // Backup & Restore Support
    @Query("SELECT * FROM items")
    suspend fun getAllItemsList(): List<ItemEntity>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM units")
    suspend fun getAllUnitsList(): List<UnitEntity>

    @Query("SELECT * FROM loan_transactions")
    suspend fun getAllLoanTransactionsList(): List<LoanTransactionEntity>

    @Query("SELECT * FROM loan_items")
    suspend fun getAllLoanItemsList(): List<LoanItemEntity>

    @Query("SELECT * FROM damaged_items")
    suspend fun getAllDamagedItemsList(): List<DamagedItemEntity>

    @Query("SELECT * FROM pemakaian_bahan")
    suspend fun getAllPemakaianBahanList(): List<PemakaianBahanEntity>

    @Query("SELECT * FROM bahan_afkir")
    suspend fun getAllBahanAfkirList(): List<BahanAfkirEntity>

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM units")
    suspend fun clearUnits()

    @Query("DELETE FROM profile")
    suspend fun clearProfile()

    // Users
    @Query("SELECT * FROM users ORDER BY role ASC, username ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUserByUsername(username: String)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}
