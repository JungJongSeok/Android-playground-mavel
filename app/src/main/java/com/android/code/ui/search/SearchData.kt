package com.android.code.ui.search

import com.android.code.network.models.marvel.MarvelResult

sealed class SearchData

data class SearchBaseData(
    val result: MarvelResult
) : SearchData()

data class SearchRecentData(
    val recentList: List<String>
) : SearchData()