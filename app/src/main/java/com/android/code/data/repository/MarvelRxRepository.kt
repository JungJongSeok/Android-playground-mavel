package com.android.code.data.repository

import com.android.code.BuildConfig
import com.android.code.network.MarvelService
import com.android.code.network.models.BaseResponse
import com.android.code.network.models.marvel.SampleResponse
import com.android.code.ui.search.SearchType
import com.android.code.util.SharedPreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.security.MessageDigest
import javax.inject.Inject


interface MarvelRxRepository {
    fun charactersRx(
        nameStartsWith: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Single<BaseResponse<SampleResponse>>

    fun setRecentList(recentList: List<String>): Single<Unit>

    fun getRecentList(): Single<List<String>>
}

class MarvelRxRepositoryImpl @Inject constructor(
    private val marvelService: MarvelService,
    private val sharedPreferencesManager: SharedPreferencesManager,
    private val type: SearchType,
) : MarvelRxRepository {
    override fun charactersRx(
        nameStartsWith: String?,
        offset: Int,
        limit: Int,
    ): Single<BaseResponse<SampleResponse>> {
        return Single.fromCallable {
            val timestamp = System.currentTimeMillis()
            val hash = marvelHash(timestamp).blockingGet()
            val nameStarts = if (nameStartsWith.isNullOrBlank()) {
                null
            } else {
                nameStartsWith
            }
            Triple(timestamp, hash, nameStarts)
        }
            .subscribeOn(Schedulers.io())
            .flatMap { (timestamp, hash, nameStarts) ->
                marvelService.charactersRx(
                    nameStarts,
                    offset,
                    limit,
                    BuildConfig.MARVEL_PUBLIC_KEY,
                    timestamp,
                    hash
                )
            }
    }

    private fun marvelHash(timestamp: Long): Single<String> {
        return Single.fromCallable {
            val digest = MessageDigest.getInstance("MD5")
            val hashString =
                timestamp.toString() + BuildConfig.MARVEL_PRIVATE_KEY + BuildConfig.MARVEL_PUBLIC_KEY
            digest.update(hashString.encodeToByteArray())
            digest.digest().fold("") { acc, byte -> acc + String.format("%02x", byte) }
        }.subscribeOn(Schedulers.io())
    }

    override fun setRecentList(recentList: List<String>): Single<Unit> {
        return Single.fromCallable {
            when (type) {
                SearchType.GRID -> sharedPreferencesManager.recentGridSearchList = recentList
                SearchType.STAGGERED -> sharedPreferencesManager.recentStaggeredSearchList =
                    recentList
            }
        }.subscribeOn(Schedulers.computation())
    }

    override fun getRecentList(): Single<List<String>> {
        return Single.fromCallable {
            when (type) {
                SearchType.GRID -> sharedPreferencesManager.recentGridSearchList
                SearchType.STAGGERED -> sharedPreferencesManager.recentStaggeredSearchList
            } ?: emptyList()
        }.subscribeOn(Schedulers.computation())
    }
}