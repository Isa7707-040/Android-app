package com.example.storemobile.data

import com.example.storemobile.data.model.Client
import com.example.storemobile.data.model.DebtPaymentDto
import com.example.storemobile.data.model.LoginRequest
import com.example.storemobile.data.model.LoginResponse
import com.example.storemobile.data.model.NewClientRequest
import com.example.storemobile.data.model.Order
import com.example.storemobile.data.model.OrderRequest
import com.example.storemobile.data.model.Product
import com.example.storemobile.data.model.Seller
import com.example.storemobile.data.model.SupplierPaymentDto
import com.example.storemobile.data.model.SyncPushRequest
import com.example.storemobile.data.model.SyncPushResponse
import com.example.storemobile.data.model.SyncSnapshot
import com.example.storemobile.data.remote.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String) : ApiResult<Nothing>
}

class StoreRepository {

    private val api get() = ApiProvider.api()

    suspend fun getSellers(): ApiResult<List<Seller>> =
        call("Foydalanuvchilar yuklanmadi") { api.getSellers() }

    suspend fun getStaff(): ApiResult<List<Seller>> =
        when (val r = call("Xodimlar yuklanmadi") { api.getStaff() }) {
            is ApiResult.Success -> r
            is ApiResult.Error -> call("Xodimlar yuklanmadi") { api.getSellers() }
        }

    suspend fun login(username: String, password: String): ApiResult<LoginResponse> =
        call("Kirishda xatolik") { api.login(LoginRequest(username, password)) }

    suspend fun getProducts(): ApiResult<List<Product>> =
        call("Mahsulotlar yuklanmadi") { api.getProducts() }

    suspend fun getClients(): ApiResult<List<Client>> =
        call("Mijozlar yuklanmadi") { api.getClients() }

    suspend fun createClient(name: String, phone: String?): ApiResult<Client> =
        call("Mijoz qo'shilmadi") { api.createClient(NewClientRequest(name, phone)) }

    suspend fun createOrder(request: OrderRequest): ApiResult<Order> =
        call("Buyurtma yuborilmadi") { api.createOrder(request) }

    suspend fun getPendingOrders(): ApiResult<List<Order>> =
        call("Buyurtmalar yuklanmadi") { api.getPendingOrders() }

    suspend fun getHistory(): ApiResult<List<Order>> =
        call("Tarix yuklanmadi") { api.getHistory() }

    // ---------- To'lov tarixi ----------

    /** Mijozning barcha qarz to'lovlari tarixini serverdan yuklaydi */
    suspend fun getClientPayments(id: Int): ApiResult<List<DebtPaymentDto>> =
        call("Mijoz to'lovlari yuklanmadi") { api.getClientPayments(id) }

    /** Firmaning barcha to'lovlari tarixini serverdan yuklaydi */
    suspend fun getSupplierPayments(id: Int): ApiResult<List<SupplierPaymentDto>> =
        call("Firma to'lovlari yuklanmadi") { api.getSupplierPayments(id) }

    // ---------- Sinxron ----------

    suspend fun getSnapshot(): ApiResult<SyncSnapshot> =
        call("Ma'lumotlar yuklanmadi") { api.getSnapshot() }

    suspend fun pushSync(request: SyncPushRequest): ApiResult<SyncPushResponse> =
        call("Sinxronlash amalga oshmadi") { api.pushSync(request) }

    private suspend fun <T> call(
        errorPrefix: String,
        block: suspend () -> Response<T>
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) ApiResult.Success(body)
                else ApiResult.Error("$errorPrefix: bo'sh javob")
            } else {
                val detail = response.errorBody()?.string()?.take(180)?.ifBlank { null }
                ApiResult.Error(detail ?: "$errorPrefix (kod ${response.code()})")
            }
        } catch (e: Exception) {
            ApiResult.Error("Server bilan aloqa yo'q. Manzilni tekshiring.")
        }
    }
}
