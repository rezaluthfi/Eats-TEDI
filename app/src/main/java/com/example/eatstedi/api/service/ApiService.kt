package com.example.eatstedi.api.service

import com.example.eatstedi.model.Employee
import com.example.eatstedi.model.MenuItem
import com.example.eatstedi.model.Schedule
import com.example.eatstedi.model.Supplier
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("login")
    fun login(@Body loginRequest: LoginRequest): Call<LoginResponse>

    // Register new cashier/employee
    @Multipart
    @POST("register")
    fun registerEmployee(
        @Part("name") name: RequestBody,
        @Part("username") username: RequestBody,
        @Part("no_telp") noTelp: RequestBody,
        @Part("email") email: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("password") password: RequestBody,
        @Part("status") status: RequestBody,
        @Part profilePicture: MultipartBody.Part?
    ): Call<GenericResponse>

    @Multipart
    @POST("create-menu")
    fun createMenu(
        @Part("name") name: RequestBody,
        @Part("price") price: RequestBody,
        @Part("id_supplier") idSupplier: RequestBody,
        @Part("supplier_name") supplierName: RequestBody?,
        @Part("food_type") foodType: RequestBody,
        @Part menuPicture: MultipartBody.Part?
    ): Call<SingleMenuResponse>

    @GET("get-menu")
    fun getMenu(): Call<MenuResponse>

    @GET("get-menu-photo/{id}")
    fun getMenuPhoto(@Path("id") id: Int): Call<ResponseBody>

    @POST("search-menu")
    fun searchMenu(@Body searchRequest: SearchRequest): Call<MenuResponse>

    @Multipart
    @POST("update-menu/{id}")
    fun updateMenu(
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("price") price: RequestBody,
        @Part("food_type") foodType: RequestBody,
        @Part menuPicture: MultipartBody.Part?
    ): Call<SingleMenuResponse>

    @FormUrlEncoded
    @POST("update-menu-stock/{id}")
    fun updateMenuStock(
        @Path("id") id: Int,
        @FieldMap stock: Map<String, String>
    ): Call<UpdateStockResponse>

    @GET("get-stock-log/{id}")
    fun getStockLog(@Path("id") id: Int): Call<StockLogResponse>

    @DELETE("delete-menu/{id}")
    fun deleteMenu(@Path("id") id: Int): Call<GenericResponse>

    @GET("filter-by-supplier/{id}")
    fun filterBySupplier(@Path("id") id: Int): Call<MenuResponse>

    @GET("filter-by-food-type/{type}")
    fun filterByFoodType(@Path("type") type: String): Call<MenuResponse>

    @POST("create-receipts")
    fun createReceipt(@Body request: CreateReceiptRequest): Call<CreateReceiptResponse>

    @POST("logout")
    fun logout(@Header("Authorization") token: String): Call<LogoutResponse>

    @GET("get-receipts")
    fun getReceipts(): Call<ReceiptResponse>

    @GET("get-specific-receipt/{id}")
    fun getSpecificReceipt(@Path("id") id: Int): Call<SpecificReceiptResponse>


    @HTTP(method = "DELETE", path = "delete-specific-orders", hasBody = true)
    fun deleteSpecificOrders(@Body request: DeleteSpecificOrdersRequest): Call<GenericResponse>

    @POST("search-receipts")
    fun searchReceipts(@Body request: SearchReceiptRequest): Call<ReceiptResponse>

    @POST("search-receipts-by-date")
    fun searchReceiptsByDate(@Body request: SearchReceiptByDateRequest): Call<ReceiptResponse>

    @HTTP(method = "DELETE", path = "delete-receipts", hasBody = true)
    fun deleteReceipts(@Body request: DeleteReceiptRequest): Call<GenericResponse>

    @GET("export-receipts")
    fun exportReceipts(): Call<ResponseBody>

    @GET("get-admin-profile")
    fun getAdminProfile(): Call<AdminProfileResponse>

    @Multipart
    @POST("update-admin-profile")
    fun updateAdminProfile(
        @Part("name") name: RequestBody,
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("no_telp") noTelp: RequestBody,
        @Part("password") password: RequestBody,
        @Part("new_password") newPassword: RequestBody?
    ): Call<GenericResponse>

    @GET("get-cashier-profile")
    fun getCashierProfile(): Call<CashierProfileResponse>

    @GET("get-supplier-photo-profile/{supplier_id}")
    fun getSupplierPhotoProfile(@Path("supplier_id") supplierId: Int): Call<ResponseBody>

    @Multipart
    @POST("update-cashier-profile")
    fun updateCashierProfile(
        @Part("id_cashier") idCashier: RequestBody,
        @Part("name") name: RequestBody,
        @Part("username") username: RequestBody,
        @Part("no_telp") noTelp: RequestBody,
        @Part("email") email: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("status") status: RequestBody,
        @Part("password") passwordConfirmation: RequestBody,  // Password saat ini untuk validasi
        @Part("new_password") newPassword: RequestBody?,      // Password baru (bisa null/kosong)
        @Part profilePicture: MultipartBody.Part?
    ): Call<GenericResponse>

    @Multipart
    @POST("update-cashier/{id}")
    fun updateCashierByAdmin(
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("username") username: RequestBody,
        @Part("no_telp") noTelp: RequestBody,
        @Part("email") email: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("status") status: RequestBody,
        @Part profilePicture: MultipartBody.Part?
    ): Call<GenericResponse>

    @GET("get-cashier-photo-profile/{cashier_id}")
    fun getCashierPhotoProfile(@Path("cashier_id") cashierId: Int): Call<ResponseBody>

    @DELETE("delete-cashier/{id}")
    fun deleteCashier(@Path("id") id: Int, @Header("Authorization") token: String): Call<GenericResponse>

    @GET("get-schedules")
    fun getSchedules(): Call<ScheduleResponse>

    @POST("create-schedule/{id_cashier}")
    fun createSchedule(
        @Path("id_cashier") idCashier: Int,
        @Body request: CreateScheduleRequest
    ): Call<GenericResponse>

    @GET("get-schedule-by-id-cashier/{id_cashier}")
    fun getScheduleByIdCashier(@Path("id_cashier") idCashier: Int): Call<ScheduleResponse>

    @DELETE("delete-schedule/{id}")
    fun deleteSchedule(@Path("id") id: Int): Call<GenericResponse>

    @GET("get-cashier")
    fun getCashiers(): Call<CashierResponse>

    @POST("search-cashier") // Asumsi nama endpointnya ini
    fun searchCashiers(@Body request: SearchCashierRequest): Call<CashierResponse>

    @GET("get-supplier")
    fun getSuppliers(): Call<SupplierResponse>

    @Multipart
    @POST("create-supplier")
    fun createSupplier(
        @Part("name") name: RequestBody,
        @Part("username") username: RequestBody,
        @Part("no_telp") noTelp: RequestBody,
        @Part("email") email: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("status") status: RequestBody,
        @Part profilePicture: MultipartBody.Part?
    ): Call<CreateSupplierResponse>

    @Multipart
    @POST("update-supplier/{id}")
    fun updateSupplier(
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("username") username: RequestBody,
        @Part("no_telp") noTelp: RequestBody,
        @Part("email") email: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part("status") status: RequestBody,
        @Part profilePicture: MultipartBody.Part?
    ): Call<UpdateSupplierResponse>

    @POST("search-supplier")
    fun searchSupplier(@Body request: SearchSupplierRequest): Call<SupplierResponse>

    @DELETE("delete-supplier/{id}")
    fun deleteSupplier(@Path("id") id: Int): Call<GenericResponse>

    @GET("get-daily-statistics")
    fun getDailyStatistics(): Call<DailyStatisticsResponse>

    @GET("get-weekly-statistics")
    fun getWeeklyStatistics(): Call<WeeklyStatisticsResponse>

    @GET("get-monthly-statistics")
    fun getMonthlyStatistics(): Call<MonthlyStatisticsResponse>

    @GET("recap-pembayaran-cashier/{id}")
    fun getCashierPaymentRecap(@Path("id") cashierId: Int): Call<CashierPaymentRecapResponse>

    @GET("recap-pembayaran-weekly")
    fun getWeeklyPaymentRecap(): Call<CashierPaymentRecapResponse>

    @GET("export-daily-statistics")
    fun exportDailyStatistics(): Call<ResponseBody>

    @GET("export-weekly-statistics")
    fun exportWeeklyStatistics(): Call<ResponseBody>

    @GET("export-monthly-statistics")
    fun exportMonthlyStatistics(): Call<ResponseBody>

    @GET("get-all-attendance")
    fun getAllAttendance(): Call<AttendanceApiResponse>

    @POST("filter-by-date-attendance")
    fun filterByDateAttendance(@Body request: DateFilterRequest): Call<AttendanceApiResponse>

    @GET("get-attendance-by-absent/{status}")
    fun getAttendanceByAbsent(@Path("status") status: Int): Call<AttendanceApiResponse>

    @GET("export-attendance")
    fun exportAttendance(): Call<ResponseBody>

    @GET("get-attendance-by-cashier/{id}")
    fun getAttendanceByCashier(@Path("id") cashierId: Int): Call<AttendanceApiResponse>

    @POST("filter-attendance-by-date-cashier/{id}")
    fun filterAttendanceByDateCashier(
        @Path("id") cashierId: Int,
        @Body request: DateFilterRequest
    ): Call<AttendanceApiResponse>

    @GET("get-attendance-by-absent-cashier/{id}/{status}")
    fun getAttendanceByAbsentCashier(
        @Path("id") cashierId: Int,
        @Path("status") status: Boolean
    ): Call<AttendanceApiResponse>

    @GET("export-attendance-cashier/{id}")
    fun exportAttendanceCashier(@Path("id") cashierId: Int): Call<ResponseBody>

    @GET("get-log")
    fun getAllLogs(): Call<LogResponse>

    @POST("get-log-by-name")
    fun getLogByName(@Body request: LogByNameRequest): Call<LogResponse>

    @POST("filter-log-by-date")
    fun filterLogByDate(@Body request: LogByDateRequest): Call<LogResponse>
}

// Model yang sudah ada (tidak diubah)
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val activity: String?,
    val data: User? = null,
    val token: String? = null,
    val message: String? = null
)

data class User(
    val id: Int,
    val name: String,
    val role: String,
    val profile_picture: String?,
    val created_at: String?,
    val updated_at: String?
)

data class LogoutResponse(
    val success: Boolean,
    val message: String?
)

data class MenuResponse(
    val success: Boolean,
    val activity: String?,
    val data: List<MenuItem>,
    val message: String? = null
)

data class SingleMenuResponse(
    val success: Boolean,
    val activity: String?,
    val data: MenuItem,
    val message: String? = null
)

data class UpdateStockResponse(
    val success: Boolean,
    val activity: String,
    val data: MenuItem,
    val message: String? = null
)

data class StockLogResponse(
    val success: Boolean,
    val activity: String,
    val data: List<StockLog>,
    val message: String? = null
)

data class StockLog(
    val id: Int,
    val name: String,
    @SerializedName("created_at") val createdAt: String,
    val activity: String,
    val quantity: Int
)

data class GenericResponse(
    val success: Boolean,
    val activity: String? = null,
    val message: Any? = null,
    val data: Any? = null
)

data class SearchRequest(
    val name: String
)

data class CreateReceiptRequest(
    val id_menu: Map<String, Int>,
    val payment_type: String,
    val payment: Int
)

data class CreateReceiptResponse(
    val success: Boolean,
    val activity: String? = null,
    val message: String? = null,
    val data: ReceiptData? = null
)

data class ReceiptData(
    val cashier_id: Int,
    val payment_type: String,
    val payment: Int,
    val total: Int,
    val returns: Int,
    val updated_at: String,
    val created_at: String,
    val id: Int
)

data class ReceiptResponse(
    val success: Boolean,
    val activity: String,
    val data: List<Receipt>,
    val message: String? = null
)

data class SpecificReceiptResponse(
    val success: Boolean,
    val activity: String,
    val data: List<TransactionDetail>,
    val message: String? = null
)

data class TransactionDetail(
    val order_id: Int,
    val menu_name: String,
    val supplier_name: String,
    val amount: Int,
    val price: Int,
    val created_at: String
)

data class DeleteSpecificOrdersRequest(
    val id_orders: List<Int>
)

data class Receipt(
    val id: Int,
    val payment_type: String,
    val cashier_id: Int,
    val total: Int,
    val payment: Int,
    val returns: Int,
    val created_at: String,
    val updated_at: String
)

data class DeleteReceiptRequest(
    val id_receipts: List<Int>
)

data class SearchReceiptRequest(
    val cashier_name: String
)

data class SearchReceiptByDateRequest(
    val start_date: String,
    val end_date: String
)

data class AdminProfileResponse(
    val success: Boolean,
    val activity: String,
    val data: AdminProfileData
)

data class AdminProfileData(
    val id: Int,
    val name: String,
    val username: String,
    val role: String,
    val profile_picture: String?,
    val email: String?,
    val no_telp: String?,
    val created_at: String,
    val updated_at: String
)

data class CashierProfileResponse(
    val success: Boolean,
    val activity: String,
    val data: CashierProfileData
)

data class CashierProfileData(
    val id: Int,
    val name: String,
    val username: String,
    val no_telp: String,
    val email: String,
    val alamat: String,
    val status: String,
    val role: String,
    val profile_picture: String,
    val created_at: String,
    val updated_at: String
)

data class UpdatedCashierData(
    val id: Int,
    val name: String,
    val username: String,
    val no_telp: String,
    val status: String,
    val role: String,
    val profile_picture: String?,
    val email: String,
    val alamat: String
)

data class DeleteCashierRequest(
    val id: Int
)

data class CreateScheduleRequest(
    val id_shifts: Int,
    val day: String
)

data class ScheduleResponse(
    val success: Boolean,
    val activity: String,
    val data: List<Schedule>
)

data class CashierResponse(
    val success: Boolean,
    val activity: String,
    val data: List<Employee>,
    val message: String?
)

data class SearchCashierRequest(
    @SerializedName("name")
    val query: String
)

data class SupplierResponse(
    val success: Boolean,
    val activity: String,
    val data: List<Supplier>,
    val message: String?
)

data class CreateSupplierRequest(
    val name: String,
    val username: String,
    val no_telp: String,
    val status: String
)

data class CreateSupplierResponse(
    val success: Boolean,
    val activity: String,
    val data: Supplier,
    val message: String? = null
)

data class UpdateSupplierRequest(
    val name: String,
    val username: String,
    val no_telp: String,
    val status: String
)

data class UpdateSupplierResponse(
    val success: Boolean,
    val activity: String,
    val data: Supplier,
    val message: String? = null
)

data class SearchSupplierRequest(
    @SerializedName("name")
    val query: String
)

data class CashierPaymentRecapResponse(
    val success: Boolean,
    val activity: String,
    val data: List<CashierPaymentData> // Ubah dari object menjadi List
)

data class CashierPaymentData(
    val id: Int,
    val payment_type: String,
    val cashier_id: Int,
    val total: Int,
    val payment: Int,
    val returns: Int,
    val created_at: String,
    val updated_at: String
)

data class ProcessedCashierRecap(
    val tunai: Int,
    val qris: Int
)

data class CashierPaymentRecapData(
    val tunai: Int,
    val qris: Int
)

data class DailyStatistics(
    val id: Int,
    val payment_type: String,
    val cashier_id: Int,
    val total: Int,
    val payment: Int,
    val returns: Int,
    val created_at: String,
    val updated_at: String
)

data class DailyStatisticsResponse(
    val success: Boolean,
    val activity: String,
    val data: List<DailyStatistics>
)

data class WeeklyStatisticsResponse(
    val success: Boolean,
    val activity: String,
    val data: Map<String, List<DailyStatistics>>
)

data class MonthlyStatisticsResponse(
    val success: Boolean,
    val activity: String,
    val data: Map<String, List<DailyStatistics>>
)

data class AttendanceResponse(
    val success: Boolean,
    val activity: String,
    val message: List<Attendance>
)

data class Attendance(
    val employeeName: String,
    val date: String,
    val shift: String,
    val time: String,
    val attendance: String
)

data class AttendanceRecord(
    val id: Int,
    val attendance: Int,
    val id_schedules: Int,
    val id_cashiers: Int,
    val date: String,
    val created_at: String,
    val updated_at: String,
    val day: String,
    val name: String,
    val id_shift: Int
)

data class AttendanceApiResponse(
    val success: Boolean,
    val activity: String,
    @SerializedName("message") val data: List<AttendanceRecord>
)

data class DateFilterRequest(
    @SerializedName("date_awal") val date_awal: String,
    @SerializedName("date_akhir") val date_akhir: String
)

data class LogResponse(
    val success: Boolean,
    val activity: String? = null,
    val data: List<LogActivity>? = null,
    val message: String? = null
)

data class LogActivity(
    val id: Int? = null,
    @SerializedName("id_cashier") val idCashier: Int? = null,
    @SerializedName("id_admin") val idAdmin: Int? = null,
    val action: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    var user: String? = null,
    var date: String? = null,
    var time: String? = null,
    var activity: String? = null
)

data class LogByNameRequest(
    val name: String
)

data class LogByDateRequest(
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String
)