package com.example.data.api

import com.example.data.model.Device
import com.example.data.model.InventoryPart
import com.example.data.model.PartMovementLog
import com.example.data.model.User
import com.example.data.model.ShopConfig
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    @GET("Shops")
    suspend fun getShopConfig(
        @Query("id") idQuery: String
    ): Response<List<ShopConfig>>

    @GET("Shops")
    suspend fun getAllShops(
        @Query("order") order: String = "id.desc"
    ): Response<List<ShopConfig>>

    @POST("Shops")
    suspend fun createShop(
        @Body shop: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<ShopConfig>>

    @PATCH("Shops")
    suspend fun updateShop(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<ShopConfig>>

    @DELETE("Shops")
    suspend fun deleteShop(
        @Query("id") idQuery: String
    ): Response<Unit>

    @GET("Users")
    suspend fun loginUser(
        @Query("username") usernameQuery: String,
        @Query("password") passwordQuery: String
    ): Response<List<User>>

    @GET("Users")
    suspend fun getUserByUsername(
        @Query("username") usernameQuery: String
    ): Response<List<User>>

    @GET("Users")
    suspend fun getAllUsers(
        @Query("shop_id") shopIdQuery: String? = null,
        @Query("order") order: String = "id.desc"
    ): Response<List<User>>

    @PATCH("Users")
    suspend fun updateUserPermissions(
        @Query("id") idQuery: String,
        @Body body: Map<String, String>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<User>>

    @POST("Users")
    suspend fun createUser(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<User>>

    @PATCH("Users")
    suspend fun updateUser(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<User>>

    @retrofit2.http.DELETE("Users")
    suspend fun deleteUser(
        @Query("id") idQuery: String
    ): Response<Unit>

    @GET("Devices")
    suspend fun getDevices(
        @Query("shop_id") shopIdQuery: String? = null,
        @Query("order") order: String = "id.desc"
    ): Response<List<Device>>

    @POST("Devices")
    suspend fun createDevice(
        @Body device: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Device>>

    @PATCH("Devices")
    suspend fun updateDevice(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Device>>

    @DELETE("Devices")
    suspend fun deleteDevice(
        @Query("id") idQuery: String
    ): Response<Unit>

    @GET("InventoryParts")
    suspend fun getInventoryParts(
        @Query("shop_id") shopIdQuery: String? = null,
        @Query("order") order: String = "id.desc"
    ): Response<List<InventoryPart>>

    @POST("InventoryParts")
    suspend fun createInventoryPart(
        @Body part: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<InventoryPart>>

    @PATCH("InventoryParts")
    suspend fun updateInventoryPart(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<InventoryPart>>

    @GET("PartMovements")
    suspend fun getPartMovements(
        @Query("shop_id") shopIdQuery: String? = null,
        @Query("order") order: String = "id.desc"
    ): Response<List<PartMovementLog>>

    @POST("PartMovements")
    suspend fun createPartMovement(
        @Body movement: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<PartMovementLog>>

    // ==========================================
    // Lowercase Table Endpoints (Standard PostgreSQL / PostgREST)
    // ==========================================

    @GET("shops")
    suspend fun getShopConfigLower(
        @Query("id") idQuery: String
    ): Response<List<ShopConfig>>

    @GET("shops")
    suspend fun getAllShopsLower(
        @Query("order") order: String = "id.desc"
    ): Response<List<ShopConfig>>

    @POST("shops")
    suspend fun createShopLower(
        @Body shop: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<ShopConfig>>

    @PATCH("shops")
    suspend fun updateShopLower(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<ShopConfig>>

    @DELETE("shops")
    suspend fun deleteShopLower(
        @Query("id") idQuery: String
    ): Response<Unit>

    @GET("users")
    suspend fun loginUserLower(
        @Query("username") usernameQuery: String,
        @Query("password") passwordQuery: String
    ): Response<List<User>>

    @GET("users")
    suspend fun getUserByUsernameLower(
        @Query("username") usernameQuery: String
    ): Response<List<User>>

    @GET("users")
    suspend fun getAllUsersLower(
        @Query("shop_id") shopIdQuery: String? = null,
        @Query("order") order: String = "id.desc"
    ): Response<List<User>>

    @PATCH("users")
    suspend fun updateUserPermissionsLower(
        @Query("id") idQuery: String,
        @Body body: Map<String, String>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<User>>

    @POST("users")
    suspend fun createUserLower(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<User>>

    @PATCH("users")
    suspend fun updateUserLower(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<User>>

    @DELETE("users")
    suspend fun deleteUserLower(
        @Query("id") idQuery: String
    ): Response<Unit>

    @GET("devices")
    suspend fun getDevicesLower(
        @Query("shop_id") shopIdQuery: String? = null,
        @Query("order") order: String = "id.desc"
    ): Response<List<Device>>

    @POST("devices")
    suspend fun createDeviceLower(
        @Body device: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Device>>

    @PATCH("devices")
    suspend fun updateDeviceLower(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Device>>

    @DELETE("devices")
    suspend fun deleteDeviceLower(
        @Query("id") idQuery: String
    ): Response<Unit>

    @GET("inventory_parts")
    suspend fun getInventoryPartsLower(
        @Query("shop_id") shopIdQuery: String? = null,
        @Query("order") order: String = "id.desc"
    ): Response<List<InventoryPart>>

    @POST("inventory_parts")
    suspend fun createInventoryPartLower(
        @Body part: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<InventoryPart>>

    @PATCH("inventory_parts")
    suspend fun updateInventoryPartLower(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<InventoryPart>>

    @GET("part_movements")
    suspend fun getPartMovementsLower(
        @Query("shop_id") shopIdQuery: String? = null,
        @Query("order") order: String = "id.desc"
    ): Response<List<PartMovementLog>>

    @POST("part_movements")
    suspend fun createPartMovementLower(
        @Body movement: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<PartMovementLog>>
}
