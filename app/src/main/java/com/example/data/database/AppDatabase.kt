package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.InventoryDao
import com.example.data.entity.ItemEntity
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.example.data.entity.DamagedItemEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.UnitEntity
import com.example.data.entity.PemakaianBahanEntity
import com.example.data.entity.BahanAfkirEntity
import com.example.data.entity.ProfileEntity
import com.example.data.entity.UserEntity

@Database(
    entities = [
        ItemEntity::class, 
        LoanTransactionEntity::class, 
        LoanItemEntity::class, 
        DamagedItemEntity::class,
        CategoryEntity::class,
        UnitEntity::class,
        PemakaianBahanEntity::class,
        BahanAfkirEntity::class,
        ProfileEntity::class,
        UserEntity::class
    ],
    version = 18,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun resetDatabaseInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }

        private fun migrateDatabaseToLatest(database: SupportSQLiteDatabase) {
            val tableCreateQueries = mapOf(
                "units" to "CREATE TABLE IF NOT EXISTS `units` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "categories" to "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "items" to "CREATE TABLE IF NOT EXISTS `items` (`idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `stokAwal` INTEGER NOT NULL, `kategori` TEXT NOT NULL, `satuan` TEXT NOT NULL, `stokRusak` INTEGER NOT NULL, `merekAlat` TEXT NOT NULL, `ruang` TEXT NOT NULL, `sumberDana` TEXT, `kondisi` TEXT NOT NULL, `keterangan` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0, `type` TEXT NOT NULL DEFAULT 'ALAT', `isBorrowable` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`idBarang`))",
                "bahan_afkir" to "CREATE TABLE IF NOT EXISTS `bahan_afkir` (`idAfkir` TEXT NOT NULL, `idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `jumlahAfkir` INTEGER NOT NULL, `satuan` TEXT NOT NULL, `alasan` TEXT NOT NULL, `tanggalAfkir` TEXT NOT NULL, `status` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`idAfkir`))",
                "loan_items" to "CREATE TABLE IF NOT EXISTS `loan_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idTransaksi` TEXT NOT NULL, `idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `jumlah` INTEGER NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "damaged_items" to "CREATE TABLE IF NOT EXISTS `damaged_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `jumlah` INTEGER NOT NULL, `tanggalKerusakan` TEXT NOT NULL, `waktuKerusakan` TEXT NOT NULL, `keteranganKerusakan` TEXT NOT NULL, `namaPetugas` TEXT NOT NULL, `kondisiBaru` TEXT NOT NULL, `status` TEXT NOT NULL, `statusKeterangan` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0)",
                "pemakaian_bahan" to "CREATE TABLE IF NOT EXISTS `pemakaian_bahan` (`idPemakaian` TEXT NOT NULL, `idBarang` TEXT NOT NULL, `namaBarang` TEXT NOT NULL, `jumlahDiambil` INTEGER NOT NULL, `satuan` TEXT NOT NULL, `namaPeminta` TEXT NOT NULL, `jabatan` TEXT NOT NULL, `kelas` TEXT, `namaPetugas` TEXT NOT NULL, `tanggalPemakaian` TEXT NOT NULL, `keterangan` TEXT NOT NULL, `isDemo` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`idPemakaian`))",
                "loan_transactions" to "CREATE TABLE IF NOT EXISTS `loan_transactions` (`idTransaksi` TEXT NOT NULL, `tanggal` TEXT NOT NULL, `namaPeminjam` TEXT NOT NULL, `kelas` TEXT NOT NULL, `waktu` TEXT NOT NULL, `kondisi` TEXT NOT NULL, `namaPetugas` TEXT NOT NULL, `status` TEXT NOT NULL, `tanggalKembali` TEXT, `waktuKembali` TEXT, `kondisiKembali` TEXT, `petugasKembali` TEXT, `keteranganKerusakan` TEXT, `whatsappNumber` TEXT, `durasiHari` INTEGER NOT NULL DEFAULT 1, `isDemo` INTEGER NOT NULL DEFAULT 0, `tujuanPeminjaman` TEXT, `detailTujuan` TEXT, PRIMARY KEY(`idTransaksi`))",
                "profile" to "CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER NOT NULL, `namaPetugas` TEXT NOT NULL, `nip` TEXT NOT NULL, `namaInstansi` TEXT NOT NULL, `fotoUri` TEXT NOT NULL, PRIMARY KEY(`id`))",
                "users" to "CREATE TABLE IF NOT EXISTS `users` (`username` TEXT NOT NULL, `password` TEXT NOT NULL, `role` TEXT NOT NULL, `fullName` TEXT NOT NULL DEFAULT '', `createdAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`username`))"
            )

            val tablesWithColumns = mapOf(
                "units" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "name" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "categories" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "name" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "items" to listOf(
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "stokAwal" to "INTEGER NOT NULL DEFAULT 0",
                    "kategori" to "TEXT NOT NULL DEFAULT ''",
                    "satuan" to "TEXT NOT NULL DEFAULT ''",
                    "stokRusak" to "INTEGER NOT NULL DEFAULT 0",
                    "merekAlat" to "TEXT NOT NULL DEFAULT ''",
                    "ruang" to "TEXT NOT NULL DEFAULT ''",
                    "sumberDana" to "TEXT",
                    "kondisi" to "TEXT NOT NULL DEFAULT ''",
                    "keterangan" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0",
                    "type" to "TEXT NOT NULL DEFAULT 'ALAT'",
                    "isBorrowable" to "INTEGER NOT NULL DEFAULT 1"
                ),
                "bahan_afkir" to listOf(
                    "idAfkir" to "TEXT NOT NULL DEFAULT ''",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "jumlahAfkir" to "INTEGER NOT NULL DEFAULT 0",
                    "satuan" to "TEXT NOT NULL DEFAULT ''",
                    "alasan" to "TEXT NOT NULL DEFAULT ''",
                    "tanggalAfkir" to "TEXT NOT NULL DEFAULT ''",
                    "status" to "TEXT NOT NULL DEFAULT 'Aktif'",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "loan_items" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "idTransaksi" to "TEXT NOT NULL DEFAULT ''",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "jumlah" to "INTEGER NOT NULL DEFAULT 0",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "damaged_items" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 0",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "jumlah" to "INTEGER NOT NULL DEFAULT 0",
                    "tanggalKerusakan" to "TEXT NOT NULL DEFAULT ''",
                    "waktuKerusakan" to "TEXT NOT NULL DEFAULT ''",
                    "keteranganKerusakan" to "TEXT NOT NULL DEFAULT ''",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "kondisiBaru" to "TEXT NOT NULL DEFAULT ''",
                    "status" to "TEXT NOT NULL DEFAULT 'Rusak (Perlu Tindakan)'",
                    "statusKeterangan" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "pemakaian_bahan" to listOf(
                    "idPemakaian" to "TEXT NOT NULL DEFAULT ''",
                    "idBarang" to "TEXT NOT NULL DEFAULT ''",
                    "namaBarang" to "TEXT NOT NULL DEFAULT ''",
                    "jumlahDiambil" to "INTEGER NOT NULL DEFAULT 0",
                    "satuan" to "TEXT NOT NULL DEFAULT ''",
                    "namaPeminta" to "TEXT NOT NULL DEFAULT ''",
                    "jabatan" to "TEXT NOT NULL DEFAULT ''",
                    "kelas" to "TEXT",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "tanggalPemakaian" to "TEXT NOT NULL DEFAULT ''",
                    "keterangan" to "TEXT NOT NULL DEFAULT ''",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0"
                ),
                "loan_transactions" to listOf(
                    "idTransaksi" to "TEXT NOT NULL DEFAULT ''",
                    "tanggal" to "TEXT NOT NULL DEFAULT ''",
                    "namaPeminjam" to "TEXT NOT NULL DEFAULT ''",
                    "kelas" to "TEXT NOT NULL DEFAULT ''",
                    "waktu" to "TEXT NOT NULL DEFAULT ''",
                    "kondisi" to "TEXT NOT NULL DEFAULT ''",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "status" to "TEXT NOT NULL DEFAULT ''",
                    "tanggalKembali" to "TEXT",
                    "waktuKembali" to "TEXT",
                    "kondisiKembali" to "TEXT",
                    "petugasKembali" to "TEXT",
                    "keteranganKerusakan" to "TEXT",
                    "whatsappNumber" to "TEXT",
                    "durasiHari" to "INTEGER NOT NULL DEFAULT 1",
                    "isDemo" to "INTEGER NOT NULL DEFAULT 0",
                    "tujuanPeminjaman" to "TEXT",
                    "detailTujuan" to "TEXT"
                ),
                "profile" to listOf(
                    "id" to "INTEGER NOT NULL DEFAULT 1",
                    "namaPetugas" to "TEXT NOT NULL DEFAULT ''",
                    "nip" to "TEXT NOT NULL DEFAULT ''",
                    "namaInstansi" to "TEXT NOT NULL DEFAULT ''",
                    "fotoUri" to "TEXT NOT NULL DEFAULT ''"
                ),
                "users" to listOf(
                    "username" to "TEXT NOT NULL DEFAULT ''",
                    "password" to "TEXT NOT NULL DEFAULT ''",
                    "role" to "TEXT NOT NULL DEFAULT 'siswa'",
                    "fullName" to "TEXT NOT NULL DEFAULT ''",
                    "createdAt" to "INTEGER NOT NULL DEFAULT 0"
                )
            )

            // 1. Create tables if they do not exist
            for ((_, query) in tableCreateQueries) {
                database.execSQL(query)
            }

            // 2. Add any missing columns to existing tables
            for ((tableName, expectedColumns) in tablesWithColumns) {
                val existingColumns = mutableSetOf<String>()
                try {
                    database.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                        val nameIndex = cursor.getColumnIndex("name")
                        if (nameIndex != -1) {
                            while (cursor.moveToNext()) {
                                existingColumns.add(cursor.getString(nameIndex).lowercase())
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error reading table_info for $tableName", e)
                }

                for ((colName, colDef) in expectedColumns) {
                    if (!existingColumns.contains(colName.lowercase())) {
                        try {
                            database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$colName` $colDef")
                            android.util.Log.d("AppDatabase", "Successfully added column $colName to table $tableName")
                        } catch (e: Exception) {
                            android.util.Log.e("AppDatabase", "Error adding column $colName to table $tableName: ${e.message}")
                        }
                    }
                }
            }

            // 3. Seed/ensure default Super Admin user (Lintang Senja)
            seedInitialUsers(database)
        }

        private fun seedInitialUsers(database: SupportSQLiteDatabase) {
            try {
                database.execSQL(
                    "INSERT OR REPLACE INTO `users` (`username`, `password`, `role`, `fullName`, `createdAt`) " +
                    "VALUES ('lintang', 'lintanglunaris', 'super_admin', 'Lintang Senja', ${System.currentTimeMillis()})"
                )
                database.execSQL(
                    "INSERT OR IGNORE INTO `users` (`username`, `password`, `role`, `fullName`, `createdAt`) " +
                    "VALUES ('admin', 'admin123', 'super_admin', 'Super Admin', ${System.currentTimeMillis()})"
                )
                database.execSQL(
                    "INSERT OR IGNORE INTO `users` (`username`, `password`, `role`, `fullName`, `createdAt`) " +
                    "VALUES ('siswa', 'siswa19', 'siswa', 'Siswa Lunaris', ${System.currentTimeMillis()})"
                )
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error seeding initial default users in AppDatabase", e)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val migrations = (1..17).map { start ->
                    object : Migration(start, 18) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            migrateDatabaseToLatest(database)
                        }
                    }
                }.toTypedArray()

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gudang_sman_database"
                )
                    .addMigrations(*migrations)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedInitialUsers(db)
                        }
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            seedInitialUsers(db)
                        }
                    })
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
