package com.example.data.network

import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.ItemEntity
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseService(private val db: AppDatabase) {

    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var itemsListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null
    private var loanItemsListener: ListenerRegistration? = null

    fun startRealtimeSync() {
        Log.d("FirebaseService", "Starting real-time Firestore sync...")

        // 1. Sync Items
        itemsListener = firestore.collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseService", "Error listening to items", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        try {
                            for (doc in snapshot.documents) {
                                val idBarang = doc.id
                                val namaBarang = doc.getString("namaBarang") ?: continue
                                val stokAwal = doc.getLong("stokAwal")?.toInt() ?: 0
                                val kategori = doc.getString("kategori") ?: ""
                                val satuan = doc.getString("satuan") ?: ""
                                val stokRusak = doc.getLong("stokRusak")?.toInt() ?: 0
                                val merekAlat = doc.getString("merekAlat") ?: ""
                                val ruang = doc.getString("ruang") ?: ""
                                val sumberDana = doc.getString("sumberDana")
                                val kondisi = doc.getString("kondisi") ?: ""
                                val keterangan = doc.getString("keterangan") ?: ""
                                val isDemo = doc.getBoolean("isDemo") ?: false
                                val type = doc.getString("type") ?: "ALAT"
                                val isBorrowable = doc.getBoolean("isBorrowable") ?: true

                                val item = ItemEntity(
                                    idBarang = idBarang,
                                    namaBarang = namaBarang,
                                    stokAwal = stokAwal,
                                    kategori = kategori,
                                    satuan = satuan,
                                    stokRusak = stokRusak,
                                    merekAlat = merekAlat,
                                    ruang = ruang,
                                    sumberDana = sumberDana,
                                    kondisi = kondisi,
                                    keterangan = keterangan,
                                    isDemo = isDemo,
                                    type = type,
                                    isBorrowable = isBorrowable
                                )
                                db.inventoryDao().insertItem(item)
                            }
                            Log.d("FirebaseService", "Synced ${snapshot.size()} items from Firestore to Room.")
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "Failed to sync items to Room", e)
                        }
                    }
                }
            }

        // 2. Sync Loan Transactions
        transactionsListener = firestore.collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseService", "Error listening to transactions", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        try {
                            for (doc in snapshot.documents) {
                                val idTransaksi = doc.id
                                val tanggal = doc.getString("tanggal") ?: continue
                                val namaPeminjam = doc.getString("namaPeminjam") ?: continue
                                val kelas = doc.getString("kelas") ?: ""
                                val waktu = doc.getString("waktu") ?: ""
                                val kondisi = doc.getString("kondisi") ?: ""
                                val namaPetugas = doc.getString("namaPetugas") ?: ""
                                val status = doc.getString("status") ?: "Dipinjam"
                                val tanggalKembali = doc.getString("tanggalKembali")
                                val waktuKembali = doc.getString("waktuKembali")
                                val kondisiKembali = doc.getString("kondisiKembali")
                                val petugasKembali = doc.getString("petugasKembali")
                                val keteranganKerusakan = doc.getString("keteranganKerusakan")
                                val whatsappNumber = doc.getString("whatsappNumber")
                                val durasiHari = doc.getLong("durasiHari")?.toInt() ?: 1
                                val isDemo = doc.getBoolean("isDemo") ?: false
                                val tujuanPeminjaman = doc.getString("tujuanPeminjaman")
                                val detailTujuan = doc.getString("detailTujuan")

                                val transaction = LoanTransactionEntity(
                                    idTransaksi = idTransaksi,
                                    tanggal = tanggal,
                                    namaPeminjam = namaPeminjam,
                                    kelas = kelas,
                                    waktu = waktu,
                                    kondisi = kondisi,
                                    namaPetugas = namaPetugas,
                                    status = status,
                                    tanggalKembali = tanggalKembali,
                                    waktuKembali = waktuKembali,
                                    kondisiKembali = kondisiKembali,
                                    petugasKembali = petugasKembali,
                                    keteranganKerusakan = keteranganKerusakan,
                                    whatsappNumber = whatsappNumber,
                                    durasiHari = durasiHari,
                                    isDemo = isDemo,
                                    tujuanPeminjaman = tujuanPeminjaman,
                                    detailTujuan = detailTujuan
                                )
                                db.inventoryDao().insertTransaction(transaction)
                            }
                            Log.d("FirebaseService", "Synced ${snapshot.size()} transactions from Firestore to Room.")
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "Failed to sync transactions to Room", e)
                        }
                    }
                }
            }

        // 3. Sync Loan Items
        loanItemsListener = firestore.collection("loan_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseService", "Error listening to loan_items", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch {
                        try {
                            val itemsList = mutableListOf<LoanItemEntity>()
                            for (doc in snapshot.documents) {
                                val id = doc.getLong("id")?.toInt() ?: continue
                                val idTransaksi = doc.getString("idTransaksi") ?: continue
                                val idBarang = doc.getString("idBarang") ?: continue
                                val namaBarang = doc.getString("namaBarang") ?: continue
                                val jumlah = doc.getLong("jumlah")?.toInt() ?: 1
                                val isDemo = doc.getBoolean("isDemo") ?: false

                                val loanItem = LoanItemEntity(
                                    id = id,
                                    idTransaksi = idTransaksi,
                                    idBarang = idBarang,
                                    namaBarang = namaBarang,
                                    jumlah = jumlah,
                                    isDemo = isDemo
                                )
                                itemsList.add(loanItem)
                            }
                            if (itemsList.isNotEmpty()) {
                                val groupedByTx = itemsList.groupBy { it.idTransaksi }
                                for ((txId, txItems) in groupedByTx) {
                                    db.inventoryDao().deleteLoanItemsForTransaction(txId)
                                    db.inventoryDao().insertLoanItems(txItems)
                                }
                                db.inventoryDao().cleanupDuplicateLoanItems()
                            }
                            Log.d("FirebaseService", "Synced ${snapshot.size()} loan items from Firestore to Room.")
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "Failed to sync loan items to Room", e)
                        }
                    }
                }
            }
    }

    fun stopRealtimeSync() {
        itemsListener?.remove()
        transactionsListener?.remove()
        loanItemsListener?.remove()
        Log.d("FirebaseService", "Stopped real-time Firestore sync.")
    }

    // Helper functions to write back to Firestore on any user action
    fun saveItemToFirestore(item: ItemEntity) {
        scope.launch {
            try {
                firestore.collection("items").document(item.idBarang).set(item)
                    .addOnSuccessListener {
                        Log.d("FirebaseService", "Successfully wrote item ${item.idBarang} to Firestore.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseService", "Failed to write item ${item.idBarang} to Firestore", e)
                    }
            } catch (e: Exception) {
                Log.e("FirebaseService", "Exception writing item to Firestore", e)
            }
        }
    }

    fun deleteItemFromFirestore(idBarang: String) {
        scope.launch {
            try {
                firestore.collection("items").document(idBarang).delete()
                    .addOnSuccessListener {
                        Log.d("FirebaseService", "Successfully deleted item $idBarang from Firestore.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseService", "Failed to delete item $idBarang from Firestore", e)
                    }
            } catch (e: Exception) {
                Log.e("FirebaseService", "Exception deleting item from Firestore", e)
            }
        }
    }

    fun saveTransactionToFirestore(transaction: LoanTransactionEntity) {
        scope.launch {
            try {
                firestore.collection("transactions").document(transaction.idTransaksi).set(transaction)
                    .addOnSuccessListener {
                        Log.d("FirebaseService", "Successfully wrote transaction ${transaction.idTransaksi} to Firestore.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseService", "Failed to write transaction ${transaction.idTransaksi} to Firestore", e)
                    }
            } catch (e: Exception) {
                Log.e("FirebaseService", "Exception writing transaction to Firestore", e)
            }
        }
    }

    fun saveLoanItemsToFirestore(items: List<LoanItemEntity>) {
        scope.launch {
            try {
                for (item in items) {
                    val docId = "${item.idTransaksi}_${item.idBarang}"
                    val data = mapOf(
                        "id" to item.id,
                        "idTransaksi" to item.idTransaksi,
                        "idBarang" to item.idBarang,
                        "namaBarang" to item.namaBarang,
                        "jumlah" to item.jumlah,
                        "isDemo" to item.isDemo
                    )
                    firestore.collection("loan_items").document(docId).set(data)
                        .addOnSuccessListener {
                            Log.d("FirebaseService", "Successfully wrote loan item $docId to Firestore.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseService", "Failed to write loan item $docId to Firestore", e)
                        }
                }
            } catch (e: Exception) {
                Log.e("FirebaseService", "Exception writing loan items to Firestore", e)
            }
        }
    }

    fun clearAllTransactionsFromFirestore(onComplete: (() -> Unit)? = null) {
        scope.launch {
            try {
                val collections = listOf("transactions", "loan_items", "pemakaian_bahan", "bahan_afkir", "damaged_items")
                for (coll in collections) {
                    firestore.collection(coll).get()
                        .addOnSuccessListener { snapshot ->
                            if (snapshot != null && !snapshot.isEmpty) {
                                val batch = firestore.batch()
                                for (doc in snapshot.documents) {
                                    batch.delete(doc.reference)
                                }
                                batch.commit().addOnCompleteListener {
                                    Log.d("FirebaseService", "Cleared collection $coll from Firestore.")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseService", "Error querying collection $coll for deletion", e)
                        }
                }
                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e("FirebaseService", "Exception clearing Firestore transaction collections", e)
                onComplete?.invoke()
            }
        }
    }
}
