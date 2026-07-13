package com.example.storemobile.ui.boss

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.storemobile.data.ApiResult
import com.example.storemobile.data.OfflineStore
import com.example.storemobile.data.SessionManager
import com.example.storemobile.data.StoreRepository
import com.example.storemobile.data.model.Client
import com.example.storemobile.data.model.DebtPaymentDto
import com.example.storemobile.data.model.PendingOperation
import com.example.storemobile.data.model.Supplier
import com.example.storemobile.data.model.SupplierPaymentDto
import com.example.storemobile.data.model.SyncOpRequest
import com.example.storemobile.data.model.SyncOpType
import com.example.storemobile.data.model.SyncPushRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/* ════════════════════════════════════════════════════════════════
 *  BOSHLIQ VIEW-MODEL (offline-first)
 *  ────────────────────────────────────────────────────────────────
 *  • start()    — telefon xotirasidan oxirgi snapshot + navbatni yuklaydi.
 *  • payClient / paySupplier — internetsiz ishlaydi: amal navbatga yoziladi,
 *    ekrandagi qarz DARHOL kamayadi (lokal hisob).
 *  • syncNow()  — do'konga qaytib: navbatdagi amallarni serverga yuboradi
 *    va eng yangi snapshotni qaytarib oladi (ikki tomon teng bo'ladi).
 *
 *  Ekranda ko'rsatiladigan qarz = snapshot qarzi − navbatdagi to'lovlar.
 *  Shu sabab boshliq internetsiz ham aniq, jonli qoldiqni ko'radi.
 * ════════════════════════════════════════════════════════════════ */

/**
 * Qidiruv uchun matnni normallashtiradi: katta/kichik harf farqi yo'qoladi
 * (Locale.ROOT — til sozlamasiga bog'liq emas), bo'sh joylar va apostrof
 * variantlari (' ʻ ʼ ` ´) bir xil ko'rinishga keltiriladi. Shu sabab
 * "OLIM", "olim", "o'lim" — barchasi mos keladi.
 */
private fun normalizeSearch(s: String): String =
    s.lowercase(Locale.ROOT)
        .replace('\u02bb', '\'')
        .replace('\u02bc', '\'')
        .replace('\u0060', '\'')
        .replace('\u00b4', '\'')
        .trim()

data class BossUiState(
    val loading: Boolean = true,
    val syncing: Boolean = false,

    // Jonli (lokal) qoldiqlar bilan ro'yxatlar
    val clients: List<Client> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),

    // Navbatdagi (hali yuborilmagan) amallar
    val pending: List<PendingOperation> = emptyList(),
    val pendingByClient: Map<Int, Double> = emptyMap(),
    val pendingBySupplier: Map<Int, Double> = emptyMap(),

    val clientSearch: String = "",
    val supplierSearch: String = "",

    val lastSyncIso: String = "",
    val syncSummary: String? = null,
    val toast: String? = null,

    // ─── Task #4: summalarni yashirish (global — bo'limlar o'rtasida saqlanadi) ───
    val hideAmounts: Boolean = false,

    // ─── Qarz tarixi (mijoz yoki firma) ───
    val clientHistory: List<DebtPaymentDto>? = null,
    val supplierHistory: List<SupplierPaymentDto>? = null,
    val historyLoading: Boolean = false,
    val historyError: String? = null,
    val historyName: String = ""
) {
    val pendingCount: Int get() = pending.size
    val hasSnapshot: Boolean get() = clients.isNotEmpty() || suppliers.isNotEmpty() || lastSyncIso.isNotBlank()

    val totalClientDebt: Double get() = clients.sumOf { it.debtBalance }
    val totalSupplierDebt: Double get() = suppliers.sumOf { it.debtBalance }

    // Navbatda yig'ilgan / to'langan summalar (boshliqqa ko'rsatish uchun)
    val pendingClientTotal: Double get() = pending.filter { it.isClient }.sumOf { it.amount }
    val pendingSupplierTotal: Double get() = pending.filter { it.isSupplier }.sumOf { it.amount }

    /** Tarix dialogi ko'rsatilishi kerakmi (mijoz yoki firma)? */
    val showHistory: Boolean get() = historyLoading || clientHistory != null || supplierHistory != null || historyError != null

    val filteredClients: List<Client>
        get() {
            val q = normalizeSearch(clientSearch)
            if (q.isEmpty()) return clients
            return clients.filter {
                normalizeSearch(it.name).contains(q) ||
                        normalizeSearch(it.phone ?: "").contains(q)
            }
        }

    val filteredSuppliers: List<Supplier>
        get() {
            val q = normalizeSearch(supplierSearch)
            if (q.isEmpty()) return suppliers
            return suppliers.filter {
                normalizeSearch(it.name).contains(q) ||
                        normalizeSearch(it.phone ?: "").contains(q)
            }
        }
}

class BossViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = StoreRepository()
    private val store = OfflineStore(app)
    private val session = SessionManager(app)

    private val _ui = MutableStateFlow(BossUiState())
    val ui: StateFlow<BossUiState> = _ui.asStateFlow()

    /** App-wide theme preference ("system" | "light" | "dark"). */
    val themeMode: StateFlow<String> = session.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionManager.THEME_SYSTEM
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch { session.saveThemeMode(mode) }
    }

    // Serverdan kelgan "xom" (asl) qoldiqlar — lokal hisob shulardan ayriladi.
    private var rawClients: List<Client> = emptyList()
    private var rawSuppliers: List<Supplier> = emptyList()
    private var pending: List<PendingOperation> = emptyList()
    private var lastSyncIso: String = ""

    var userName: String = "Boshliq"
    // Qarzni yig'ayotgan boshliq/admin user id (sinxronda serverga yuboriladi).
    private var userId: Int = 0
    private var started = false

    fun start(userName: String) {
        if (started) return
        started = true
        this.userName = userName
        viewModelScope.launch {
            // Sinxronda kerak bo'ladi: qarzni kim yig'gani (admin user id).
            userId = session.savedSession.first()?.userId ?: 0
            val state = store.load()
            rawClients = state.snapshot.clients
            rawSuppliers = state.snapshot.suppliers
            pending = state.pending
            lastSyncIso = state.lastSyncIso
            recompute(loading = false)
        }
    }

    fun setClientSearch(v: String) { _ui.value = _ui.value.copy(clientSearch = v) }
    fun setSupplierSearch(v: String) { _ui.value = _ui.value.copy(supplierSearch = v) }

    // ─── Task #4: Global summalar ko'rinishini almashtirish ───

    /**
     * Barcha bo'limlarda (Qarzdorlar, Firmalar) summalar ko'rinishini almashtiradi.
     * Bu holat ViewModel darajasida saqlanadi, shuning uchun bo'limlar
     * o'rtasida o'tishda qayta ochilmaydi.
     */
    fun toggleHideAmounts() {
        _ui.value = _ui.value.copy(hideAmounts = !_ui.value.hideAmounts)
    }

    // ─── Qarz tarixi (mijoz) ───

    /**
     * Mijozning barcha qarz to'lovlari tarixini serverdan yuklaydi.
     * Natija [BossUiState.clientHistory] ga yoziladi.
     * Internet bo'lmasa [BossUiState.historyError] da xato ko'rsatiladi.
     */
    fun loadClientHistory(client: Client) {
        _ui.value = _ui.value.copy(
            historyLoading = true,
            clientHistory = null,
            supplierHistory = null,
            historyError = null,
            historyName = client.name
        )
        viewModelScope.launch {
            when (val r = repo.getClientPayments(client.id)) {
                is ApiResult.Success -> _ui.value = _ui.value.copy(
                    historyLoading = false,
                    clientHistory = r.data
                )
                is ApiResult.Error -> _ui.value = _ui.value.copy(
                    historyLoading = false,
                    historyError = r.message
                )
            }
        }
    }

    /**
     * Firmaning barcha to'lovlari tarixini serverdan yuklaydi.
     * Natija [BossUiState.supplierHistory] ga yoziladi.
     */
    fun loadSupplierHistory(supplier: Supplier) {
        _ui.value = _ui.value.copy(
            historyLoading = true,
            supplierHistory = null,
            clientHistory = null,
            historyError = null,
            historyName = supplier.name
        )
        viewModelScope.launch {
            when (val r = repo.getSupplierPayments(supplier.id)) {
                is ApiResult.Success -> _ui.value = _ui.value.copy(
                    historyLoading = false,
                    supplierHistory = r.data
                )
                is ApiResult.Error -> _ui.value = _ui.value.copy(
                    historyLoading = false,
                    historyError = r.message
                )
            }
        }
    }

    /** Tarix dialogini yopadi va holatni tozalaydi. */
    fun clearHistory() {
        _ui.value = _ui.value.copy(
            clientHistory = null,
            supplierHistory = null,
            historyLoading = false,
            historyError = null,
            historyName = ""
        )
    }

    /* ───────── Offline to'lovlar (internet shart emas) ───────── */

    fun payClient(client: Client, amount: Double, note: String?, onDone: (Boolean) -> Unit) {
        val amt = Math.round(amount).toDouble()
        if (amt <= 0) { _ui.value = _ui.value.copy(toast = "Summa 0 dan katta bo'lishi kerak"); onDone(false); return }
        val op = PendingOperation(
            operationId = UUID.randomUUID().toString(),
            type = SyncOpType.CLIENT_PAYMENT,
            entityId = client.id,
            entityName = client.name,
            amount = amt,
            note = note?.trim()?.ifBlank { null },
            createdAt = nowIso()
        )
        viewModelScope.launch {
            store.addPending(op)
            pending = pending + op
            recompute()
            _ui.value = _ui.value.copy(toast = "Qabul qilindi: ${client.name} — endi sinxron qiling")
            onDone(true)
        }
    }

    fun paySupplier(supplier: Supplier, amount: Double, note: String?, onDone: (Boolean) -> Unit) {
        val amt = Math.round(amount).toDouble()
        if (amt <= 0) { _ui.value = _ui.value.copy(toast = "Summa 0 dan katta bo'lishi kerak"); onDone(false); return }
        val op = PendingOperation(
            operationId = UUID.randomUUID().toString(),
            type = SyncOpType.SUPPLIER_PAYMENT,
            entityId = supplier.id,
            entityName = supplier.name,
            amount = amt,
            note = note?.trim()?.ifBlank { null },
            createdAt = nowIso()
        )
        viewModelScope.launch {
            store.addPending(op)
            pending = pending + op
            recompute()
            _ui.value = _ui.value.copy(toast = "To'lov qabul qilindi: ${supplier.name}")
            onDone(true)
        }
    }

    /** Navbatdagi bir amalni bekor qilish (sinxrondan oldin). */
    fun cancelPending(op: PendingOperation) {
        viewModelScope.launch {
            store.cancelPending(op.operationId)
            pending = pending.filterNot { it.operationId == op.operationId }
            recompute()
            _ui.value = _ui.value.copy(toast = "Bekor qilindi")
        }
    }

    /* ───────── Sinxron (push + pull) ───────── */

    fun syncNow(onDone: (Boolean) -> Unit = {}) {
        if (_ui.value.syncing) return
        _ui.value = _ui.value.copy(syncing = true, syncSummary = null)

        val sent = pending // joriy navbat (sinxron paytida o'zgarmasligi uchun nusxa)
        val collector = if (userId > 0) userId else null
        val request = SyncPushRequest(
            device = deviceLabel(),
            operations = sent.map {
                SyncOpRequest(
                    operationId = it.operationId,
                    type = it.type,
                    entityId = it.entityId,
                    amount = it.amount,
                    note = it.note,
                    createdAt = it.createdAt,
                    // Qarz yig'gan admin id — server buni kunlik kassaga qo'shadi.
                    collectorUserId = collector
                )
            }
        )

        viewModelScope.launch {
            when (val r = repo.pushSync(request)) {
                is ApiResult.Success -> {
                    val resp = r.data

                    // 1) Eng yangi snapshotni saqlaymiz (do'kondagi savdolar ham aks etadi)
                    rawClients = resp.snapshot.clients
                    rawSuppliers = resp.snapshot.suppliers
                    lastSyncIso = if (resp.serverTime.isNotBlank()) resp.serverTime else nowIso()
                    store.saveSnapshot(resp.snapshot, lastSyncIso)

                    // 2) Serverga yuborilgan va qabul qilingan amallarni navbatdan o'chiramiz
                    val handled = resp.results.map { it.operationId }.toSet()
                    val sentIds = sent.map { it.operationId }.toSet()
                    val toRemove = if (handled.isNotEmpty()) handled else sentIds
                    store.removePending(toRemove)
                    pending = pending.filterNot { it.operationId in toRemove }

                    recompute()

                    // 3) Natija xulosasi
                    val applied = resp.results.count { it.status == "applied" }
                    val dup = resp.results.count { it.status == "duplicate" }
                    val err = resp.results.count { it.status == "error" }
                    val appliedSum = resp.results.filter { it.status == "applied" }.sumOf { it.appliedAmount }
                    val summary = buildString {
                        append("Sinxron tugadi. ")
                        if (sent.isEmpty()) append("Ma'lumotlar yangilandi.")
                        else {
                            append("Qo'llandi: $applied ta")
                            if (appliedSum > 0) append(" (${formatMoney(appliedSum)} so'm)")
                            if (dup > 0) append(", takror: $dup ta")
                            if (err > 0) append(", xato: $err ta")
                        }
                    }
                    _ui.value = _ui.value.copy(syncing = false, syncSummary = summary, toast = summary)
                    onDone(true)
                }
                is ApiResult.Error -> {
                    _ui.value = _ui.value.copy(
                        syncing = false,
                        syncSummary = null,
                        toast = "Sinxron bo'lmadi: ${r.message}. Do'kon WiFi'siga ulanib qayta urinib ko'ring."
                    )
                    onDone(false)
                }
            }
        }
    }

    fun consumeToast() { _ui.value = _ui.value.copy(toast = null) }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            session.clearSession()
            onDone()
        }
    }

    /* ───────── Ichki hisob ───────── */

    // Lokal jonli qoldiqlarni qayta hisoblaydi: snapshot qarzi − navbatdagi to'lovlar.
    private fun recompute(loading: Boolean = false) {
        val byClient = pending.filter { it.isClient }
            .groupBy { it.entityId }
            .mapValues { e -> e.value.sumOf { it.amount } }
        val bySupplier = pending.filter { it.isSupplier }
            .groupBy { it.entityId }
            .mapValues { e -> e.value.sumOf { it.amount } }

        val liveClients = rawClients.map { c ->
            val paid = byClient[c.id] ?: 0.0
            c.copy(debtBalance = (c.debtBalance - paid).coerceAtLeast(0.0))
        }
        val liveSuppliers = rawSuppliers.map { s ->
            val paid = bySupplier[s.id] ?: 0.0
            s.copy(debtBalance = (s.debtBalance - paid).coerceAtLeast(0.0))
        }

        _ui.value = _ui.value.copy(
            loading = loading,
            clients = liveClients,
            suppliers = liveSuppliers,
            pending = pending,
            pendingByClient = byClient,
            pendingBySupplier = bySupplier,
            lastSyncIso = lastSyncIso
        )
    }

    private fun deviceLabel(): String =
        listOfNotNull(userName.ifBlank { null }, Build.MODEL).joinToString(" / ")

    private fun nowIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date()) + "Z"
    }

    private fun formatMoney(value: Double): String {
        val rounded = Math.round(value)
        val sb = StringBuilder(rounded.toString())
        var i = sb.length - 3
        while (i > 0) { sb.insert(i, ' '); i -= 3 }
        return sb.toString()
    }
}
