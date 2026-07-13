package com.example.storemobile.data.model

data class Product(
    val id: Int = 0,
    val name: String = "",
    val unit: String = "dona",
    val quantityInBlock: Int = 1,
    val buyPriceBlock: Double = 0.0,
    val sellPriceBlock: Double = 0.0,
    val sellPricePiece: Double = 0.0,
    val totalPieces: Int = 0
) {
    val inStock: Boolean get() = totalPieces > 0
    val blocks: Int get() = if (quantityInBlock > 0) totalPieces / quantityInBlock else 0
    val loosePieces: Int get() = if (quantityInBlock > 0) totalPieces % quantityInBlock else totalPieces
    val isKg: Boolean get() = unit.equals("kg", ignoreCase = true)
    val isBlockType: Boolean get() = quantityInBlock > 1 && !isKg
    val sellableByBlock: Boolean get() = isBlockType
    val effectiveSellPricePiece: Double
        get() = when {
            sellPricePiece > 0.0 -> sellPricePiece
            sellPriceBlock > 0.0 && quantityInBlock > 0 -> sellPriceBlock / quantityInBlock
            else -> 0.0
        }
    val effectiveSellPriceBlock: Double
        get() = when {
            sellPriceBlock > 0.0 -> sellPriceBlock
            sellPricePiece > 0.0 -> sellPricePiece * quantityInBlock
            else -> 0.0
        }
    val hasShortage: Boolean get() = totalPieces < 0
    val stockShort: String
        get() = when {
            isKg -> "$totalPieces kg"
            !isBlockType -> "$totalPieces dona"
            totalPieces < 0 -> "$totalPieces dona"
            else -> {
                val b = totalPieces / quantityInBlock
                val p = totalPieces % quantityInBlock
                when {
                    b > 0 && p > 0 -> "$b blok $p dona"
                    b > 0 -> "$b blok"
                    else -> "$p dona"
                }
            }
        }
    val stockBadge: String
        get() = when {
            totalPieces == 0 -> "Tugagan"
            hasShortage -> "Kamomad: $stockShort"
            else -> stockShort
        }
    val stockLong: String
        get() = when {
            isKg -> "Omborda: $totalPieces kg"
            !isBlockType -> "Omborda: $totalPieces dona"
            hasShortage -> "Omborda: $totalPieces dona (kamomad)"
            else -> "Omborda: $stockShort  (jami $totalPieces dona)"
        }
}

data class Client(
    val id: Int = 0,
    val name: String = "",
    val phone: String? = null,
    val debtBalance: Double = 0.0
) {
    val hasDebt: Boolean get() = debtBalance > 0.5
}

data class NewClientRequest(
    val name: String,
    val phone: String?
)

data class Seller(
    val id: Int = 0,
    val fullName: String = "",
    val username: String = ""
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val id: Int = 0,
    val fullName: String = "",
    val username: String = "",
    val role: String = ""
)

data class OrderRequest(
    val clientId: Int?,
    val userId: Int?,
    val totalSum: Double,
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    val productId: Int,
    val quantity: Int,
    val price: Double
)

data class Order(
    val id: Int = 0,
    val client: Client? = null,
    val user: Seller? = null,
    val totalSum: Double = 0.0,
    val paidSum: Double = 0.0,
    val status: String = "",
    val paymentType: String = "",
    val createdAt: String = "",
    val items: List<OrderItem> = emptyList()
) {
    val clientName: String get() = client?.name ?: "Naqd xaridor"
    val sellerName: String get() = user?.fullName ?: "—"
    val itemCount: Int get() = items.sumOf { it.quantity }
}

data class OrderItem(
    val id: Int = 0,
    val productId: Int = 0,
    val product: Product? = null,
    val quantity: Int = 0,
    val price: Double = 0.0
) {
    val total: Double get() = quantity * price
    val displayName: String get() = product?.name ?: "Mahsulot #$productId"
}

enum class SaleMode { BLOCK, PIECE }

data class CartLine(
    val product: Product,
    val mode: SaleMode,
    val count: Int
) {
    val key: String get() = "${product.id}-${mode.name}"
    val pieces: Int get() = if (mode == SaleMode.BLOCK) count * product.quantityInBlock else count
    val pricePerPiece: Double
        get() = if (mode == SaleMode.BLOCK) {
            if (product.quantityInBlock > 0) product.effectiveSellPriceBlock / product.quantityInBlock
            else product.effectiveSellPriceBlock
        } else product.effectiveSellPricePiece
    val lineTotal: Double
        get() = if (mode == SaleMode.BLOCK) count * product.effectiveSellPriceBlock
        else count * product.effectiveSellPricePiece
    val unitLabel: String get() = if (mode == SaleMode.BLOCK) "blok" else "dona"
}

// ─────────────────── Qarz to'lov tarixi ───────────────────

/** Mijoz qarz to'lovi tarixi (GET api/Clients/{id}/payments) */
data class DebtPaymentDto(
    val id: Int = 0,
    val clientId: Int = 0,
    val amount: Double = 0.0,
    val remainingAfter: Double = 0.0,
    val paidAt: String = "",
    val note: String? = null
)

/** Firma to'lov tarixi (GET api/Suppliers/{id}/payments) */
data class SupplierPaymentDto(
    val id: Int = 0,
    val supplierId: Int = 0,
    val amount: Double = 0.0,
    val remainingAfter: Double = 0.0,
    val paidAt: String = "",
    val note: String? = null
)
