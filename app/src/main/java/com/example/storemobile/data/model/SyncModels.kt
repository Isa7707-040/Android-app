package com.example.storemobile.data.model

/* ════════════════════════════════════════════════════════════════
 *  SINXRON (offline-first) MODELLARI
 *  ────────────────────────────────────────────────────────────────
 *  Boshliq ko'chada (internetsiz) qarz yig'adi / firmaga to'laydi. Har bir
 *  amal telefonda navbatga (PendingOperation) yoziladi. Do'konga qaytib
 *  "Sinxron" bosilganda bu amallar serverga yuboriladi (SyncPushRequest)
 *  va server eng yangi holatni qaytaradi (SyncSnapshot).
 * ════════════════════════════════════════════════════════════════ */

/* ─────────────────────────  Firma (Supplier)  ───────────────────── */

// Yetkazib beruvchi (firma). debtBalance = bizning shu firmaga qarzimiz.
data class Supplier(
    val id: Int = 0,
    val name: String = "",
    val phone: String? = null,
    val note: String? = null,
    val debtBalance: Double = 0.0
) {
    val hasDebt: Boolean get() = debtBalance > 0.5
}

/* ─────────────────────────  Snapshot (serverdan)  ───────────────── */

// GET /api/Sync/snapshot javobi — barcha mijoz/firma/mahsulot va server vaqti.
data class SyncSnapshot(
    val serverTime: String = "",
    val clients: List<Client> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val products: List<Product> = emptyList()
)

/* ─────────────────────  Offline amal turi  ───────────── */

object SyncOpType {
    const val CLIENT_PAYMENT = "ClientPayment"     // mijoz qarzini to'ladi
    const val SUPPLIER_PAYMENT = "SupplierPayment" // biz firmaga to'ladik
}

/* ─────────────────  Navbatdagi offline amal (telefonda saqlanadi)  ───── */

// Bu telefonning lokal xotirasida (JSON) saqlanadi. entityName/displayName
// faqat ekranda ko'rsatish uchun (serverga yuborilmaydi).
data class PendingOperation(
    val operationId: String = "",      // YAGONA GUID (idempotentlik kaliti)
    val type: String = "",             // SyncOpType.*
    val entityId: Int = 0,             // ClientId yoki SupplierId
    val entityName: String = "",       // ko'rsatish uchun (mijoz/firma nomi)
    val amount: Double = 0.0,
    val note: String? = null,
    val createdAt: String = ""         // ISO-8601 (qurilmada bajarilgan vaqt)
) {
    val isClient: Boolean get() = type == SyncOpType.CLIENT_PAYMENT
    val isSupplier: Boolean get() = type == SyncOpType.SUPPLIER_PAYMENT
}

/* ─────────────────────────  PUSH so'rovi/javobi  ───────────── */

// POST /api/Sync/push tanasi
data class SyncPushRequest(
    val device: String? = null,
    val operations: List<SyncOpRequest> = emptyList()
)

// Serverga yuboriladigan bitta amal (entityName/displayName YO'Q — faqat zarur maydonlar)
// collectorUserId: qarzni KIM yig'di (boshliq/admin user id). Server buni
// DebtPayment.CashierId ga yozadi (Source="Mobil") — shunda admin yig'gan pul
// ham kunlik kassa hisobiga qo'shiladi.
data class SyncOpRequest(
    val operationId: String,
    val type: String,
    val entityId: Int,
    val amount: Double,
    val note: String?,
    val createdAt: String,
    val collectorUserId: Int? = null
)

// POST /api/Sync/push javobi
data class SyncPushResponse(
    val serverTime: String = "",
    val results: List<SyncOpResult> = emptyList(),
    val snapshot: SyncSnapshot = SyncSnapshot()
)

data class SyncOpResult(
    val operationId: String = "",
    val status: String = "",           // applied | duplicate | skipped | error
    val appliedAmount: Double = 0.0,
    val remaining: Double = 0.0,
    val message: String? = null
) {
    val isOk: Boolean get() = status == "applied" || status == "duplicate"
}
