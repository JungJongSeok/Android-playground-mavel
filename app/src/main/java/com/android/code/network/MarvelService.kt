package com.android.code.network

import com.android.code.network.models.BaseResponse
import com.android.code.network.models.marvel.SampleResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface MarvelService {

    @GET("characters")
    suspend fun characters(
        @Query("nameStartsWith") nameStartsWith: String?,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("apikey") apikey: String,
        @Query("ts") timestamp: Long,
        @Query("hash") hash: String
    ): BaseResponse<SampleResponse>


    @GET("characters")
    fun charactersRx(
        @Query("nameStartsWith") nameStartsWith: String?,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("apikey") apikey: String,
        @Query("ts") timestamp: Long,
        @Query("hash") hash: String
    ): Single<BaseResponse<SampleResponse>>
}