package com.android.code.data.repository

import com.android.code.BuildConfig
import com.android.code.network.MarvelService
import com.android.code.network.models.BaseResponse
import com.android.code.network.models.marvel.SampleResponse
import com.android.code.ui.search.SearchType
import com.android.code.util.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject


interface MarvelRepository {
    suspend fun characters(
        nameStartsWith: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): BaseResponse<SampleResponse>

    suspend fun setRecentList(recentList: List<String>)

    suspend fun getRecentList(): List<String>
}

class MarvelRepositoryImpl @Inject constructor(
    private val marvelService: MarvelService,
    private val sharedPreferencesManager: SharedPreferencesManager,
    private val type: SearchType
) : MarvelRepository {

    override suspend fun characters(
        nameStartsWith: String?,
        offset: Int,
        limit: Int,
    ): BaseResponse<SampleResponse> {
        val timestamp = System.currentTimeMillis()
        val hash = marvelHash(timestamp)
        val nameStarts = if (nameStartsWith.isNullOrBlank()) {
            null
        } else {
            nameStartsWith
        }
        return marvelService.characters(nameStarts,
            offset,
            limit,
            BuildConfig.MARVEL_PUBLIC_KEY,
            timestamp,
            hash)
    }

    private suspend fun marvelHash(timestamp: Long): String {
        return withContext(Dispatchers.IO) {
            val digest = MessageDigest.getInstance("MD5")
            val hashString =
                timestamp.toString() + BuildConfig.MARVEL_PRIVATE_KEY + BuildConfig.MARVEL_PUBLIC_KEY
            digest.update(hashString.encodeToByteArray())
            digest.digest().fold("") { acc, byte -> acc + String.format("%02x", byte) }
        }
    }

    override suspend fun setRecentList(recentList: List<String>) {
        withContext(Dispatchers.IO) {
            when (type) {
                SearchType.GRID -> sharedPreferencesManager.recentGridSearchList = recentList
                SearchType.STAGGERED -> sharedPreferencesManager.recentStaggeredSearchList = recentList
            }
        }
    }

    override suspend fun getRecentList(): List<String> {
        return withContext(Dispatchers.IO) {
            when (type) {
                SearchType.GRID -> sharedPreferencesManager.recentGridSearchList
                SearchType.STAGGERED -> sharedPreferencesManager.recentStaggeredSearchList
            } ?: emptyList()
        }
    }
}