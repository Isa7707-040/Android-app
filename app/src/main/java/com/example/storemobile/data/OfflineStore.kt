package com.example.storemobile.data

import android.content.Context
import com.example.storemobile.data.model.PendingOperation
import com.example.storemobile.data.model.SyncSnapshot
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/* ═══════════════════════════════════════════════════════════════════
 *  OFFLINE XOTIRA (JSON fayl)
 *  ───────────────────────────────────────────────────────────────────
 *  Boshliq ilovasining butun offline holati bitta JSON faylda saqlanadi:
 *    • snapshot      — serverdan oxirgi yuklab olingan mijoz/firma/mahsulot
 *    • pending       — hali serverga yuborilmagan amallar navbati (qarz to'lovlari)
 *    • lastSyncIso   — oxirgi muvaffaqiyatli sinxron vaqti
 *
 *  Room ishlatilmadi — bu yengilroq va hech qanday yangi gradle bog'liqlik
 *  talab qilmaydi (Gson allaqachon loyihada bor). Ma'lumot hajmi kichik
 *  (mijozlar/firmalar ro'yxati) bo'lgani uchun bu usul mukammal mos keladi.
 *
 *  Barcha operatsiyalar Mutex bilan himoyalangan — bir vaqtda yozish/o'qish
 *  to'qnashuvi bo'lmaydi.
 * ═══════════════════════════════════════════════════════════════════ */

// Diskka yoziladigan to'liq holat
data class OfflineState(
    val snapshot: SyncSnapshot = SyncSnapshot(),
    val pending: List<PendingOperation> = emptyList(),
    val lastSyncIso: String = ""
)

class OfflineStore(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val mutex = Mutex()

    private val file: File
        get() = File(appContext.filesDir, FILE_NAME)

    /** Butun offline holatni o'qiydi (fayl yo'q/buzuq bo'lsa — bo'sh holat). */
    suspend fun load(): OfflineState = withContext(Dispatchers.IO) {
        mutex.withLock { readUnsafe() }
    }

    /** Serverdan kelgan yangi snapshotni saqlaydi (pending'ga tegmaydi). */
    suspend fun saveSnapshot(snapshot: SyncSnapshot, lastSyncIso: String) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = readUnsafe()
                writeUnsafe(current.copy(snapshot = snapshot, lastSyncIso = lastSyncIso))
            }
        }

    /** Navbatga yangi offline amal qo'shadi. */
    suspend fun addPending(op: PendingOperation) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readUnsafe()
            writeUnsafe(current.copy(pending = current.pending + op))
        }
    }

    /** Berilgan operationId'larni navbatdan o'chiradi (serverga yuborilgach). */
    suspend fun removePending(operationIds: Set<String>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readUnsafe()
            writeUnsafe(current.copy(pending = current.pending.filterNot { it.operationId in operationIds }))
        }
    }

    /** Bitta amalni bekor qiladi (boshliq xato kiritsa, sinxrondan oldin o'chirishi mumkin). */
    suspend fun cancelPending(operationId: String) = removePending(setOf(operationId))

    /** Hamma narsani tozalaydi (chiqishda yoki qayta o'rnatishda). */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try { if (file.exists()) file.delete() } catch (_: Exception) {}
        }
    }

    // ── Ichki (mutex ostida chaqiriladi) ──────────────────────────
    private fun readUnsafe(): OfflineState {
        return try {
            if (!file.exists()) return OfflineState()
            val text = file.readText()
            if (text.isBlank()) OfflineState()
            else gson.fromJson(text, OfflineState::class.java) ?: OfflineState()
        } catch (_: Exception) {
            OfflineState()
        }
    }

    private fun writeUnsafe(state: OfflineState) {
        try {
            file.writeText(gson.toJson(state))
        } catch (_: Exception) {
            // Diskka yozib bo'lmasa — jim o'tkazib yuboramiz (ilova yiqilmasin).
        }
    }

    companion object {
        private const val FILE_NAME = "boss_offline.json"
    }
}