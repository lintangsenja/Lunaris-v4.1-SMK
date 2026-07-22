package com.example.data.database

import android.content.Context
import androidx.room.withTransaction
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class BackupManager(private val database: AppDatabase) {

    suspend fun exportDatabase(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dao = database.inventoryDao()
            val root = JSONObject()

            // Metadata
            val metadata = JSONObject().apply {
                put("app_name", "Lunaris")
                put("version", "Lunaris-v3.8")
                put("timestamp", System.currentTimeMillis())
            }
            root.put("metadata", metadata)

            // 1. Items
            val itemsJson = JSONArray()
            dao.getAllItemsList().forEach { item ->
                val obj = JSONObject().apply {
                    put("idBarang", item.idBarang)
                    put("namaBarang", item.namaBarang)
                    put("stokAwal", item.stokAwal)
                    put("kategori", item.kategori)
                    put("satuan", item.satuan)
                    put("stokRusak", item.stokRusak)
                    put("merekAlat", item.merekAlat)
                    put("ruang", item.ruang)
                    put("sumberDana", item.sumberDana ?: JSONObject.NULL)
                    put("kondisi", item.kondisi)
                    put("keterangan", item.keterangan)
                    put("isDemo", item.isDemo)
                    put("type", item.type)
                }
                itemsJson.put(obj)
            }
            root.put("items", itemsJson)

            // 2. Categories
            val categoriesJson = JSONArray()
            dao.getAllCategoriesList().forEach { cat ->
                val obj = JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("isDemo", cat.isDemo)
                }
                categoriesJson.put(obj)
            }
            root.put("categories", categoriesJson)

            // 3. Units
            val unitsJson = JSONArray()
            dao.getAllUnitsList().forEach { u ->
                val obj = JSONObject().apply {
                    put("id", u.id)
                    put("name", u.name)
                    put("isDemo", u.isDemo)
                }
                unitsJson.put(obj)
            }
            root.put("units", unitsJson)

            // 4. Loan Transactions
            val transactionsJson = JSONArray()
            dao.getAllLoanTransactionsList().forEach { tx ->
                val obj = JSONObject().apply {
                    put("idTransaksi", tx.idTransaksi)
                    put("tanggal", tx.tanggal)
                    put("namaPeminjam", tx.namaPeminjam)
                    put("kelas", tx.kelas)
                    put("waktu", tx.waktu)
                    put("kondisi", tx.kondisi)
                    put("namaPetugas", tx.namaPetugas)
                    put("status", tx.status)
                    put("tanggalKembali", tx.tanggalKembali ?: JSONObject.NULL)
                    put("waktuKembali", tx.waktuKembali ?: JSONObject.NULL)
                    put("kondisiKembali", tx.kondisiKembali ?: JSONObject.NULL)
                    put("petugasKembali", tx.petugasKembali ?: JSONObject.NULL)
                    put("keteranganKerusakan", tx.keteranganKerusakan ?: JSONObject.NULL)
                    put("whatsappNumber", tx.whatsappNumber ?: JSONObject.NULL)
                    put("durasiHari", tx.durasiHari)
                    put("isDemo", tx.isDemo)
                }
                transactionsJson.put(obj)
            }
            root.put("loan_transactions", transactionsJson)

            // 5. Loan Items
            val loanItemsJson = JSONArray()
            dao.getAllLoanItemsList().forEach { li ->
                val obj = JSONObject().apply {
                    put("id", li.id)
                    put("idTransaksi", li.idTransaksi)
                    put("idBarang", li.idBarang)
                    put("namaBarang", li.namaBarang)
                    put("jumlah", li.jumlah)
                    put("isDemo", li.isDemo)
                }
                loanItemsJson.put(obj)
            }
            root.put("loan_items", loanItemsJson)

            // 6. Damaged Items
            val damagedJson = JSONArray()
            dao.getAllDamagedItemsList().forEach { di ->
                val obj = JSONObject().apply {
                    put("id", di.id)
                    put("idBarang", di.idBarang)
                    put("namaBarang", di.namaBarang)
                    put("jumlah", di.jumlah)
                    put("tanggalKerusakan", di.tanggalKerusakan)
                    put("waktuKerusakan", di.waktuKerusakan)
                    put("keteranganKerusakan", di.keteranganKerusakan)
                    put("namaPetugas", di.namaPetugas)
                    put("kondisiBaru", di.kondisiBaru)
                    put("status", di.status)
                    put("statusKeterangan", di.statusKeterangan)
                    put("isDemo", di.isDemo)
                }
                damagedJson.put(obj)
            }
            root.put("damaged_items", damagedJson)

            // 7. Pemakaian Bahan
            val pemakaianJson = JSONArray()
            dao.getAllPemakaianBahanList().forEach { pb ->
                val obj = JSONObject().apply {
                    put("idPemakaian", pb.idPemakaian)
                    put("idBarang", pb.idBarang)
                    put("namaBarang", pb.namaBarang)
                    put("jumlahDiambil", pb.jumlahDiambil)
                    put("satuan", pb.satuan)
                    put("namaPeminta", pb.namaPeminta)
                    put("jabatan", pb.jabatan)
                    put("kelas", pb.kelas ?: JSONObject.NULL)
                    put("namaPetugas", pb.namaPetugas)
                    put("tanggalPemakaian", pb.tanggalPemakaian)
                    put("keterangan", pb.keterangan)
                    put("isDemo", pb.isDemo)
                }
                pemakaianJson.put(obj)
            }
            root.put("pemakaian_bahan", pemakaianJson)

            // 8. Bahan Afkir
            val afkirJson = JSONArray()
            dao.getAllBahanAfkirList().forEach { ba ->
                val obj = JSONObject().apply {
                    put("idAfkir", ba.idAfkir)
                    put("idBarang", ba.idBarang)
                    put("namaBarang", ba.namaBarang)
                    put("jumlahAfkir", ba.jumlahAfkir)
                    put("satuan", ba.satuan)
                    put("alasan", ba.alasan)
                    put("tanggalAfkir", ba.tanggalAfkir)
                    put("status", ba.status)
                    put("isDemo", ba.isDemo)
                }
                afkirJson.put(obj)
            }
            root.put("bahan_afkir", afkirJson)

            // 9. Profile
            val profileJson = JSONArray()
            dao.getProfile()?.let { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("namaPetugas", p.namaPetugas)
                    put("nip", p.nip)
                    put("namaInstansi", p.namaInstansi)
                    put("fotoUri", p.fotoUri)
                }
                profileJson.put(obj)
            }
            root.put("profile", profileJson)

            outputStream.use { os ->
                os.write(root.toString(2).toByteArray(StandardCharsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importDatabase(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val content = inputStream.use { stream ->
                stream.readBytes().toString(StandardCharsets.UTF_8)
            }
            val root = JSONObject(content)

            // Validate metadata
            val metadata = root.optJSONObject("metadata")
            if (metadata == null) {
                return@withContext Result.failure(IllegalArgumentException("File backup tidak valid (Metadata hilang)."))
            }

            val appName = metadata.optString("app_name", "")
            val version = metadata.optString("version", "")
            
            if (appName != "Lunaris" || !version.startsWith("Lunaris-v3")) {
                return@withContext Result.failure(IllegalArgumentException("Skema backup tidak kompatibel dengan Lunaris-v3.8."))
            }

            // Extract data
            val itemsJson = root.optJSONArray("items") ?: JSONArray()
            val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
            val unitsJson = root.optJSONArray("units") ?: JSONArray()
            val transactionsJson = root.optJSONArray("loan_transactions") ?: JSONArray()
            val loanItemsJson = root.optJSONArray("loan_items") ?: JSONArray()
            val damagedJson = root.optJSONArray("damaged_items") ?: JSONArray()
            val pemakaianJson = root.optJSONArray("pemakaian_bahan") ?: JSONArray()
            val afkirJson = root.optJSONArray("bahan_afkir") ?: JSONArray()
            val profileJson = root.optJSONArray("profile") ?: JSONArray()

            // Execute atomically in Room Transaction
            database.withTransaction {
                val dao = database.inventoryDao()

                // 1. Clear all old tables
                dao.clearItems()
                dao.clearCategories()
                dao.clearUnits()
                dao.clearTransactions()
                dao.clearLoanItems()
                dao.clearDamagedItems()
                dao.clearPemakaianBahan()
                dao.clearBahanAfkir()
                dao.clearProfile()

                // 2. Insert items
                val items = mutableListOf<ItemEntity>()
                for (i in 0 until itemsJson.length()) {
                    val obj = itemsJson.getJSONObject(i)
                    items.add(ItemEntity(
                        idBarang = obj.getString("idBarang"),
                        namaBarang = obj.getString("namaBarang"),
                        stokAwal = obj.getInt("stokAwal"),
                        kategori = obj.getString("kategori"),
                        satuan = obj.getString("satuan"),
                        stokRusak = obj.optInt("stokRusak", 0),
                        merekAlat = obj.optString("merekAlat", ""),
                        ruang = obj.optString("ruang", ""),
                        sumberDana = if (obj.isNull("sumberDana")) null else obj.optString("sumberDana"),
                        kondisi = obj.optString("kondisi", "Normal"),
                        keterangan = obj.optString("keterangan", ""),
                        isDemo = obj.optBoolean("isDemo", false),
                        type = obj.optString("type", "ALAT")
                    ))
                }
                if (items.isNotEmpty()) dao.insertItems(items)

                // 3. Insert categories
                val categories = mutableListOf<CategoryEntity>()
                for (i in 0 until categoriesJson.length()) {
                    val obj = categoriesJson.getJSONObject(i)
                    categories.add(CategoryEntity(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        isDemo = obj.optBoolean("isDemo", false)
                    ))
                }
                if (categories.isNotEmpty()) dao.insertCategories(categories)

                // 4. Insert units
                val units = mutableListOf<UnitEntity>()
                for (i in 0 until unitsJson.length()) {
                    val obj = unitsJson.getJSONObject(i)
                    units.add(UnitEntity(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        isDemo = obj.optBoolean("isDemo", false)
                    ))
                }
                if (units.isNotEmpty()) dao.insertUnits(units)

                // 5. Insert transactions
                val transactions = mutableListOf<LoanTransactionEntity>()
                for (i in 0 until transactionsJson.length()) {
                    val obj = transactionsJson.getJSONObject(i)
                    transactions.add(LoanTransactionEntity(
                        idTransaksi = obj.getString("idTransaksi"),
                        tanggal = obj.getString("tanggal"),
                        namaPeminjam = obj.getString("namaPeminjam"),
                        kelas = obj.getString("kelas"),
                        waktu = obj.getString("waktu"),
                        kondisi = obj.getString("kondisi"),
                        namaPetugas = obj.getString("namaPetugas"),
                        status = obj.getString("status"),
                        tanggalKembali = if (obj.isNull("tanggalKembali")) null else obj.optString("tanggalKembali"),
                        waktuKembali = if (obj.isNull("waktuKembali")) null else obj.optString("waktuKembali"),
                        kondisiKembali = if (obj.isNull("kondisiKembali")) null else obj.optString("kondisiKembali"),
                        petugasKembali = if (obj.isNull("petugasKembali")) null else obj.optString("petugasKembali"),
                        keteranganKerusakan = if (obj.isNull("keteranganKerusakan")) null else obj.optString("keteranganKerusakan"),
                        whatsappNumber = if (obj.isNull("whatsappNumber")) null else obj.optString("whatsappNumber"),
                        durasiHari = obj.optInt("durasiHari", 1),
                        isDemo = obj.optBoolean("isDemo", false)
                    ))
                }
                if (transactions.isNotEmpty()) dao.insertTransactions(transactions)

                // 6. Insert loan items
                val loanItems = mutableListOf<LoanItemEntity>()
                for (i in 0 until loanItemsJson.length()) {
                    val obj = loanItemsJson.getJSONObject(i)
                    loanItems.add(LoanItemEntity(
                        id = obj.getInt("id"),
                        idTransaksi = obj.getString("idTransaksi"),
                        idBarang = obj.getString("idBarang"),
                        namaBarang = obj.getString("namaBarang"),
                        jumlah = obj.getInt("jumlah"),
                        isDemo = obj.optBoolean("isDemo", false)
                    ))
                }
                if (loanItems.isNotEmpty()) dao.insertLoanItems(loanItems)

                // 7. Insert damaged items
                val damaged = mutableListOf<DamagedItemEntity>()
                for (i in 0 until damagedJson.length()) {
                    val obj = damagedJson.getJSONObject(i)
                    damaged.add(DamagedItemEntity(
                        id = obj.getInt("id"),
                        idBarang = obj.getString("idBarang"),
                        namaBarang = obj.getString("namaBarang"),
                        jumlah = obj.getInt("jumlah"),
                        tanggalKerusakan = obj.getString("tanggalKerusakan"),
                        waktuKerusakan = obj.getString("waktuKerusakan"),
                        keteranganKerusakan = obj.getString("keteranganKerusakan"),
                        namaPetugas = obj.optString("namaPetugas", ""),
                        kondisiBaru = obj.optString("kondisiBaru", ""),
                        status = obj.optString("status", "Rusak (Perlu Tindakan)"),
                        statusKeterangan = obj.optString("statusKeterangan", ""),
                        isDemo = obj.optBoolean("isDemo", false)
                    ))
                }
                if (damaged.isNotEmpty()) dao.insertDamagedItems(damaged)

                // 8. Insert pemakaian bahan
                val pemakaian = mutableListOf<PemakaianBahanEntity>()
                for (i in 0 until pemakaianJson.length()) {
                    val obj = pemakaianJson.getJSONObject(i)
                    pemakaian.add(PemakaianBahanEntity(
                        idPemakaian = obj.getString("idPemakaian"),
                        idBarang = obj.getString("idBarang"),
                        namaBarang = obj.getString("namaBarang"),
                        jumlahDiambil = obj.getInt("jumlahDiambil"),
                        satuan = obj.getString("satuan"),
                        namaPeminta = obj.getString("namaPeminta"),
                        jabatan = obj.getString("jabatan"),
                        kelas = if (obj.isNull("kelas")) null else obj.optString("kelas"),
                        namaPetugas = obj.getString("namaPetugas"),
                        tanggalPemakaian = obj.getString("tanggalPemakaian"),
                        keterangan = obj.getString("keterangan"),
                        isDemo = obj.optBoolean("isDemo", false)
                    ))
                }
                if (pemakaian.isNotEmpty()) dao.insertPemakaianBahanList(pemakaian)

                // 9. Insert bahan afkir
                val afkir = mutableListOf<BahanAfkirEntity>()
                for (i in 0 until afkirJson.length()) {
                    val obj = afkirJson.getJSONObject(i)
                    afkir.add(BahanAfkirEntity(
                        idAfkir = obj.getString("idAfkir"),
                        idBarang = obj.getString("idBarang"),
                        namaBarang = obj.getString("namaBarang"),
                        jumlahAfkir = obj.getInt("jumlahAfkir"),
                        satuan = obj.getString("satuan"),
                        alasan = obj.getString("alasan"),
                        tanggalAfkir = obj.getString("tanggalAfkir"),
                        status = obj.optString("status", "Aktif"),
                        isDemo = obj.optBoolean("isDemo", false)
                    ))
                }
                if (afkir.isNotEmpty()) dao.insertBahanAfkirList(afkir)

                // 10. Insert profile
                if (profileJson.length() > 0) {
                    val obj = profileJson.getJSONObject(0)
                    dao.insertProfile(ProfileEntity(
                        id = obj.getInt("id"),
                        namaPetugas = obj.getString("namaPetugas"),
                        nip = obj.getString("nip"),
                        namaInstansi = obj.getString("namaInstansi"),
                        fotoUri = obj.getString("fotoUri")
                    ))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
