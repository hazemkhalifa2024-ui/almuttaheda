package com.example.data.api

import com.example.data.model.Device
import com.example.data.model.InventoryPart
import com.example.data.model.PartMovementLog
import com.example.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    @GET("Users")
    suspend fun loginUser(
        @Query("username") usernameQuery: String,
        @Query("password") passwordQuery: String
    ): Response<List<User>>

    @GET("Users")
    suspend fun getAllUsers(
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

    @GET("InventoryParts")
    suspend fun getInventoryParts(
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
        @Query("order") order: String = "id.desc"
    ): Response<List<PartMovementLog>>

    @POST("PartMovements")
    suspend fun createPartMovement(
        @Body movement: Map<String, @JvmSuppressWildcards Any>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<PartMovementLog>>
}
