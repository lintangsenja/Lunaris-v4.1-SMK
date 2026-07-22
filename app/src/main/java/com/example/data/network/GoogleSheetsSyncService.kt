package com.example.data.network

import android.util.Log
import com.example.data.entity.ItemEntity
import com.example.data.entity.LoanItemEntity
import com.example.data.entity.LoanTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleSheetsSyncService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun pullItems(webAppUrl: String): List<ItemEntity> = withContext(Dispatchers.IO) {
        val url = "$webAppUrl?action=getItems"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP Error: ${response.code}")
            val bodyString = response.body?.string() ?: throw Exception("Empty response body")
            
            // Apps Script web apps might redirect, OkHttp handles redirects but we handle JSON extraction
            val jsonArray = JSONArray(bodyString)
            val items = mutableListOf<ItemEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(
                    ItemEntity(
                        idBarang = obj.getString("idBarang"),
                        namaBarang = obj.getString("namaBarang"),
                        stokAwal = obj.optInt("stokAwal", 0)
                    )
                )
            }
            items
        }
    }

    suspend fun pushItems(webAppUrl: String, items: List<ItemEntity>): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("action", "syncItems")
            val itemsArray = JSONArray()
            items.forEach { item ->
                itemsArray.put(JSONObject().apply {
                    put("idBarang", item.idBarang)
                    put("namaBarang", item.namaBarang)
                    put("stokAwal", item.stokAwal)
                })
            }
            put("items", itemsArray)
        }

        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(webAppUrl)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext false
            val bodyString = response.body?.string() ?: ""
            val json = JSONObject(bodyString)
            json.optBoolean("success", false)
        }
    }

    suspend fun pushLoan(
        webAppUrl: String,
        transaction: LoanTransactionEntity,
        items: List<LoanItemEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("action", "addLoan")
            put("loan", JSONObject().apply {
                put("idTransaksi", transaction.idTransaksi)
                put("tanggal", transaction.tanggal)
                put("namaPeminjam", transaction.namaPeminjam)
                put("kelas", transaction.kelas)
                put("waktu", transaction.waktu)
                put("kondisi", transaction.kondisi)
                put("namaPetugas", transaction.namaPetugas)
                put("status", transaction.status)
            })
            val itemsArray = JSONArray()
            items.forEach { item ->
                itemsArray.put(JSONObject().apply {
                    put("idBarang", item.idBarang)
                    put("namaBarang", item.namaBarang)
                    put("jumlah", item.jumlah)
                })
            }
            put("items", itemsArray)
        }

        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(webAppUrl)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext false
            val bodyString = response.body?.string() ?: ""
            val json = JSONObject(bodyString)
            json.optBoolean("success", false)
        }
    }

    suspend fun pushReturn(
        webAppUrl: String,
        idTransaksi: String,
        tanggalKembali: String,
        waktuKembali: String,
        kondisiKembali: String,
        petugasKembali: String
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("action", "returnLoan")
            put("idTransaksi", idTransaksi)
            put("tanggalKembali", tanggalKembali)
            put("waktuKembali", waktuKembali)
            put("kondisiKembali", kondisiKembali)
            put("petugasKembali", petugasKembali)
        }

        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(webAppUrl)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext false
            val bodyString = response.body?.string() ?: ""
            val json = JSONObject(bodyString)
            json.optBoolean("success", false)
        }
    }
}
