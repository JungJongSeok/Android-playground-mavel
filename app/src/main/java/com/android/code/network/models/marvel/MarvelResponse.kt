package com.android.code.network.models.marvel

import com.google.gson.annotations.SerializedName


data class SampleResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("offset")
    val offset: Int,
    @SerializedName("results")
    val results: List<MarvelResult>?,
    @SerializedName("total")
    val total: Int
)

data class MarvelResult(
    @SerializedName("description")
    val description: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("thumbnail")
    val thumbnail: Thumbnail?,
)

data class Thumbnail(
    @SerializedName("extension")
    val extension: String,
    @SerializedName("path")
    val path: String
)