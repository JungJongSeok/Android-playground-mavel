package com.android.code.network.models

import com.google.gson.annotations.SerializedName

data class BaseRequest<out T>(
    @SerializedName("data")
    val data: T
)
